package com.bethibande.web.context

import com.bethibande.web.Http3Client
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3Headers
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.concurrent.Future
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

class HttpClientContext(
    private val client: Http3Client
) {

    companion object {

        const val STATE_INITIAL = 0x00
        const val STATE_STREAM = 0x01
        const val STATE_HEADER_SENT = 0x02
        const val STATE_DATA_SENT = 0x04
        const val STATE_HEADER_RECEIVED = 0x08
        const val STATE_DATA_RECEIVED = 0x10
        const val STATE_RECEIVED_ALL = 0x20
        const val STATE_CLOSED = 0x40

    }

    @Volatile
    private var state = STATE_INITIAL

    @Volatile
    private var response: Http3Headers? = null
    private val responseListeners = mutableListOf<Runnable>()
    @Volatile
    private var contentLength: Long = 0
    @Volatile
    private var dataReceived: Long = 0
    private val bufferedData = ConcurrentLinkedQueue<ByteBuf>()
    private val receivedAllListeners = mutableListOf<Runnable>()

    @Volatile
    private var dataConsumer: Consumer<ByteBuf>? = null
    @Volatile
    private var stream: Future<QuicStreamChannel>? = null
    @Volatile
    private var lastWrite: ChannelFuture? = null

    internal fun connect(): Future<QuicStreamChannel> {
        val stream = this.client.newStream(this::headerCallback, this::dataCallback)
        stream.addListener { this.set(STATE_STREAM) }

        this.stream = stream

        return stream
    }

    private fun receivedAll() {
        this.set(STATE_RECEIVED_ALL)
        this.receivedAllListeners.forEach(Runnable::run)
    }

    private fun headerCallback(frame: Http3HeadersFrame, isLast: Boolean) {
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Received a response header before sending a request")
        if(has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Already received a response header")

        this.set(STATE_HEADER_RECEIVED)

        val headers = frame.headers()
        if(!headers.contains("content-length") || headers.getLong("content-length") == 0L) {
            this.receivedAll()
        } else {
            this.contentLength = headers.getLong("content-length")
        }

        this.response = headers
        this.responseListeners.forEach(Runnable::run)
    }

    private fun dataCallback(data: Http3DataFrame, isLast: Boolean) {
        if(!this.has(STATE_HEADER_RECEIVED))
        this.set(STATE_DATA_RECEIVED)

        val buf = data.content()
        this.dataReceived += buf.writerIndex()

        if(this.dataConsumer == null || this.bufferedData.isNotEmpty()) {
            this.bufferedData.offer(buf)
        } else {
            this.dataConsumer?.accept(buf)
            buf.release()
        }

        if(this.dataReceived == this.contentLength) {
            this.receivedAll()
        }
    }

    private fun has(state: Int): Boolean = this.state and state == state
    private fun set(state: Int) {
        this.state = this.state or state
    }

    private fun <R> accessStream(consumer: (QuicStreamChannel) -> R): R {
        if(!has(STATE_STREAM)) throw IllegalStateException("Cannot send request, the stream is not yet initialized")
        val stream = this.stream!!.get()

        return consumer.invoke(stream)
    }

    fun sendHeader(headers: Http3Headers): ChannelFuture {
        if(has(STATE_HEADER_SENT)) throw IllegalStateException("A request header has already been sent")
        this.set(STATE_HEADER_SENT)

        val future = accessStream { it.writeAndFlush(DefaultHttp3HeadersFrame(headers)) }
        this.lastWrite = future

        return future
    }

    fun write(buf: ByteBuf): ChannelFuture {
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot write data before sending a request header")
        this.set(STATE_DATA_SENT)

        val future = accessStream { it.write(DefaultHttp3DataFrame(buf)) }
        this.lastWrite = future

        return future
    }

    fun write(arr: ByteArray): ChannelFuture = this.write(Unpooled.wrappedBuffer(arr).writerIndex(arr.size))
    fun write(buf: ByteBuffer): ChannelFuture = this.write(Unpooled.wrappedBuffer(buf))
    fun write(str: String, charset: Charset = StandardCharsets.UTF_8): ChannelFuture {
        val arr = str.toByteArray(charset)
        return this.write(Unpooled.wrappedBuffer(arr).writerIndex(arr.size))
    }

    fun flush() {
        this.lastWrite?.addListener {
            accessStream { it.flush() }
        }
    }

    fun onResponse(consumer: Consumer<Http3Headers>) {
        this.responseListeners.add { consumer.accept(this.response!!) }
    }

    fun onStatus(status: Short, consumer: Consumer<Http3Headers>) {
        this.onResponse {
            if(it.status().toString() != status.toString()) return@onResponse

            consumer.accept(it)
        }
    }

    fun onData(consumer: Consumer<ByteBuf>) {
        this.dataConsumer = consumer

        while(this.bufferedData.isNotEmpty()) {
            val buf = this.bufferedData.poll()
            consumer.accept(buf)
            buf.release()
        }
    }

    fun onReadFinished(runnable: Runnable) {
        this.receivedAllListeners.add(runnable)
    }

    fun readAll(consumer: Consumer<Array<ByteBuf>>) {
        this.receivedAllListeners.add {
            consumer.accept(this.bufferedData.toTypedArray())

            this.bufferedData.forEach(ByteBuf::release)
        }
    }

    fun readAllAsArray(consumer: Consumer<ByteArray>) {
        readAll { buffers ->
            val length = getHeader().getInt("content-length")
            val buffer = ByteArray(length)
            var index = 0

            for(buf in buffers) {
                buf.readBytes(buffer, 0, buf.writerIndex())
                index += buf.writerIndex()
            }

            consumer.accept(buffer)
        }
    }

    fun readAsString(charset: Charset = StandardCharsets.UTF_8, consumer: Consumer<String>) {
        readAllAsArray {
            consumer.accept(String(it, charset))
        }
    }

    fun finish() {
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot finish request before sending a header")
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Cannot finish request before receiving a header")

        this.lastWrite!!.addListener {
            accessStream { stream ->
                this.set(STATE_CLOSED)
                stream.shutdownInput().addListener { stream.close() }
            }
        }
    }

    fun getHeader(): Http3Headers {
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("a response header is not yet available")
        return this.response!!
    }

    fun getContentLength(): Long {
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Cannot retrieve content-length before a response header has been received")
        return this.contentLength
    }

    fun newRequestHeader(path: String, method: HttpMethod, contentLength: Long = 0): Http3Headers = DefaultHttp3Headers()
        .scheme("https")
        .authority("${client.address.hostString}:${client.address.port}")
        .path(path)
        .method(method.toString())
        .setLong("content-length", contentLength)

    fun state() = this.state

}