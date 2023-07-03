package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import com.bethibande.web.types.FieldListener
import com.bethibande.web.types.HasState
import com.bethibande.web.types.ValueQueue
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelProgressivePromise
import io.netty.handler.codec.Headers
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function

abstract class HttpContextBase(
    protected open val connection: HttpConnection,
    protected open val channel: Channel,
    private val readOnly: Boolean = false,
): HasState() {

    companion object {
        const val STATE_HEADER_RECEIVED = 0x02
        const val STATE_HEADER_SENT = 0x04
        const val STATE_CLOSE_SET = 0x08
        const val STATE_READ_ONLY = 0x10
    }

    init {
        if (this.readOnly) set(STATE_READ_ONLY)
    }

    private val headerListener = FieldListener<AbstractHttpHeader>()
    protected var header: AbstractHttpHeader by headerListener

    protected val dataQueue = ValueQueue<ByteBuf>()

    @Volatile
    protected var lastWrite: ChannelFuture? = null

    protected var variables: Map<String, String> = mapOf()

    fun connection(): HttpConnection = this.connection
    fun closeFuture(): ChannelFuture = this.channel.closeFuture()

    internal fun headerCallback(headers: Headers<*, *, *>) {
        if(has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Already received a header")

        set(STATE_HEADER_RECEIVED)
        this.header = this.convertNettyHeaders(headers)
    }

    internal fun dataCallback(data: ByteBuf) {
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Received data before receiving a header")

        this.dataQueue.offer(data)
    }

    protected abstract fun convertNettyHeaders(headers: Headers<*, *, *>): AbstractHttpHeader

    internal fun onHeader(consumer: Consumer<AbstractHttpHeader>) {
        this.headerListener.addListener(consumer)
    }

    fun onData(consumer: Consumer<ByteBuf>) {
        if(this.dataQueue.hasConsumer()) throw IllegalStateException("A data callback has already been set")
        this.dataQueue.consume(consumer)
    }

    /**
     * Use inside of [onHeader] consumer, will throw an exception if called before header is received.
     * Note, this will set a data consumer using [onData], it is not possible to set a second data consumer.
     */
    fun readAllBytes(consumer: Consumer<ByteBuf>): ChannelProgressivePromise {
        if(this.header.getContentLength() > Int.MAX_VALUE) throw IllegalStateException("The content-length must no exceed ${Int.MAX_VALUE}")

        val length = this.header.getContentLength().toInt()
        var read = 0

        if(length <= 0) throw IllegalStateException("The content-length must be greater than 0")
        val buffer = Unpooled.buffer(length, length)
        val promise = this.channel.newProgressivePromise()

        onData { data ->
            if(data.writerIndex() == 0) { // empty buffer
                return@onData
            }

            read += data.writerIndex()
            buffer.writeBytes(data)
            data.release()

            promise.setProgress(read.toLong(), length.toLong())
            if(read == length) {
                promise.setSuccess()
                consumer.accept(buffer)
            }
        }

        return promise
    }

    fun readAllString(consumer: Consumer<String>, charset: Charset = StandardCharsets.UTF_8): ChannelProgressivePromise {
        return readAllBytes { bytes ->
            if(bytes.isDirect) {
                val copy = ByteArray(bytes.writerIndex())
                bytes.readBytes(copy)

                consumer.accept(String(copy, charset))
                return@readAllBytes
            }

            consumer.accept(String(bytes.array(), charset))
        }
    }

    fun writeHeader(header: AbstractHttpHeader): ChannelFuture {
        if(has(STATE_HEADER_SENT)) throw IllegalStateException("A header has already been sent")

        set(STATE_HEADER_SENT)
        return this.writeAndFlush(header.toFrame())
    }

    protected fun <R> access(fn: Function<Channel, R>): R {
        if(has(STATE_CLOSED)) throw IllegalStateException("The context has already been closed")
        return fn.apply(this.channel)
    }

    protected fun writeAndFlush(obj: Any): ChannelFuture = this.access { channel ->
        if(this.has(STATE_READ_ONLY)) throw IllegalStateException("The context is read-only.")

        val future = channel.writeAndFlush(obj)
        this.lastWrite = future

        return@access future
    }

    protected fun write(obj: Any): ChannelFuture = this.access { channel ->
        if(this.has(STATE_READ_ONLY)) throw IllegalStateException("The context is read-only.")

        val future = channel.write(obj)
        this.lastWrite = future

        return@access future
    }

    protected abstract fun frameData(buf: ByteBuf): Any

    fun write(buf: ByteBuf): ChannelFuture = this.write(this.frameData(buf))

    fun write(bytes: ByteArray): ChannelFuture {
        val buf = Unpooled.wrappedBuffer(bytes)
        buf.writerIndex(bytes.size)
        return this.write(buf)
    }

    fun write(buffer: ByteBuffer): ChannelFuture = this.write(Unpooled.wrappedBuffer(buffer))

    fun write(str: String, charset: Charset = StandardCharsets.UTF_8): ChannelFuture {
        return this.write(str.toByteArray(charset))
    }

    /**
     * @param totalBytes the total amount of bytes that can be read from the stream, may **not** be <= 0
     */
    fun write(stream: InputStream, totalBytes: Long, bufferSize: Int = 8192): ChannelProgressivePromise {
        val promise = this.channel.newProgressivePromise()

        try {
            var progress = 0L
            var read: Int
            do {
                val buffer = Unpooled.buffer(bufferSize)
                read = buffer.writeBytes(stream, bufferSize)
                progress += read

                this.write(buffer).addListener {
                    promise.setProgress(progress, totalBytes)
                    if(progress == totalBytes) promise.setSuccess()
                }
            } while(read > 0)
        } catch (th: Throwable) {
            promise.setFailure(th)
        }

        return promise
    }

    private fun writeResponse(status: HttpResponseStatus, data: Any?): ChannelFuture {
        val header = this.newHeader()
        header.setStatus(status)
        if(data is ByteArray) header.setContentLength(data.size.toLong())
        if(data is ByteBuffer) header.setContentLength(data.capacity().toLong())
        if(data is ByteBuf) header.setContentLength(data.capacity().toLong())

        this.writeHeader(header)
        data?.let {
            if(it is ByteArray) this.write(it)
            if(it is ByteBuffer) this.write(it)
            if(it is ByteBuf) this.write(it)
        }

        this.flush()
        this.close()
        return this.lastWrite!!
    }

    fun response(buf: ByteBuf): ChannelFuture = this.writeResponse(HttpResponseStatus.OK, buf)

    fun response(buffer: ByteBuffer): ChannelFuture = this.writeResponse(HttpResponseStatus.OK, buffer)

    fun response(string: String): ChannelFuture {
        val bytes = string.toByteArray()
        return this.writeResponse(HttpResponseStatus.OK, bytes)
    }

    fun response(status: HttpResponseStatus): ChannelFuture = this.writeResponse(status, null)

    fun flush() = this.access { channel ->
        channel.flush()
    }

    fun variables(variables: Map<String, String>) {
        this.variables = variables
    }

    fun variables(): Map<String, String> = this.variables

    protected abstract fun closeContext()

    fun close() {
        if(has(STATE_CLOSE_SET)) throw IllegalStateException("A close operation is already in progress")
        if(has(STATE_CLOSED)) throw IllegalStateException("The context has already been closed")
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot close context before sending a header")
        set(STATE_CLOSE_SET)

        this.lastWrite!!.addListener {
            this.closeContext()
            set(STATE_CLOSED)
        }
    }

    protected abstract fun newHeaderInstance(): AbstractHttpHeader

    fun newHeader(): AbstractHttpHeader {
        val header = newHeaderInstance()
        // TODO: apply default headers
        return header
    }

    fun isOpen() = !has(STATE_CLOSED) && !has(STATE_CLOSE_SET)
    fun isClosed() = has(STATE_CLOSED)
    fun isClosing() = has(STATE_CLOSE_SET)

}