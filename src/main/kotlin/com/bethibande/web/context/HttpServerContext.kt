package com.bethibande.web.context

import com.bethibande.web.Http3Server
import com.bethibande.web.types.FieldListener
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3Headers
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicStreamChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class HttpServerContext(
    private val server: Http3Server,
    private val stream: QuicStreamChannel
) {

    companion object {

        const val STATE_INITIAL = 0x00
        const val STATE_HEADER_RECEIVED = 0x01
        const val STATE_HEADER_SENT = 0x02
        const val STATE_DATA_RECEIVED = 0x04
        const val STATE_DATA_SENT = 0x08
        const val STATE_RECEIVED_ALL = 0x10
        const val STATE_CLOSED = 0x20

    }

    @Volatile
    private var state = STATE_INITIAL

    @Volatile
    private var contentLength: Long = -2
    @Volatile
    private var dataReceived: Long = 0
    private val bufferedData = ConcurrentLinkedQueue<ByteBuf>()
    @Volatile
    private var dataConsumer: Consumer<ByteBuf>? = null

    private val headerListener = FieldListener<Http3Headers>()
    private var header: Http3Headers by headerListener

    @Volatile
    private var lastWrite: ChannelFuture? = null

    private val readAllListeners = mutableListOf<Runnable>()

    internal fun has(state: Int): Boolean = this.state and state == state
    internal fun set(state: Int) {
        this.state = this.state or state
    }

    private fun receivedAll() {
        this.set(STATE_RECEIVED_ALL)
        this.readAllListeners.forEach(Runnable::run)
    }

    internal fun headerCallback(headers: Http3HeadersFrame, isLast: Boolean) {
        if(has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Already received a header")
        this.set(STATE_HEADER_RECEIVED)

        val header = headers.headers()

        if(header.contains("content-length") && header.getLong("content-length") != 0L) {
            this.contentLength = header.getLong("content-length")
        } else {
            this.receivedAll()
        }

        this.header = header
    }

    internal fun dataCallback(data: Http3DataFrame, isLast: Boolean) {
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Received data before receiving a request header")

        this.set(STATE_DATA_RECEIVED)

        val buf = data.content()
        this.dataReceived += buf.writerIndex()

        if(this.dataConsumer == null || this.bufferedData.isNotEmpty()) {
            this.bufferedData.offer(buf)
        } else {
            this.dataConsumer!!.accept(buf)
            buf.release()
        }

        if(this.dataReceived == this.contentLength) {
            this.receivedAll()
        }
    }

    fun onRequest(consumer: Consumer<Http3Headers>) {
        this.headerListener.addListener(consumer)
    }

    fun onData(consumer: Consumer<ByteBuf>) {
        this.dataConsumer = consumer

        while(this.bufferedData.isNotEmpty()) {
            val buf = this.bufferedData.poll()
            consumer.accept(buf)
            buf.release()
        }
    }

    fun onReadComplete(runnable: Runnable) {
        this.readAllListeners.add(runnable)
        if(this.dataReceived == this.contentLength) runnable.run()
    }

    fun readAll(consumer: Consumer<ByteArray>) {
        if(has(STATE_CLOSED)) throw IllegalStateException("The context is already closed")
        if(this.contentLength <= 0) throw IllegalStateException("Content-length must be greater than 0")
        if(this.contentLength > Int.MAX_VALUE) throw IllegalStateException("Content-length must be smaller than ${Int.MAX_VALUE}")

        val buf = ByteArray(this.contentLength.toInt())
        val index = AtomicInteger(0)
        onData {
            it.readBytes(buf, index.get(), it.writerIndex())
            index.addAndGet(it.writerIndex())
        }

        onReadComplete {
            consumer.accept(buf)
        }
    }

    fun readAllAsString(charset: Charset = StandardCharsets.UTF_8, consumer: Consumer<String>) {
        this.readAll {
            consumer.accept(String(it, charset))
        }
    }

    private fun <R> accessStream(consumer: (QuicStreamChannel) -> R): R {
        if(has(STATE_CLOSED)) throw IllegalStateException("The context is already closed")
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Cannot access stream before receiving request header")
        return consumer.invoke(this.stream)
    }

    fun sendHeader(header: Http3Headers): ChannelFuture {
        if(has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot send multiple headers")
        this.set(STATE_HEADER_SENT)

        return this.accessStream {
            val future = it.writeAndFlush(DefaultHttp3HeadersFrame(header))
            this.lastWrite = future
            return@accessStream future
        }
    }

    fun write(buf: ByteBuf): ChannelFuture {
        this.set(STATE_DATA_SENT)

        return this.accessStream {
            val future = it.write(DefaultHttp3DataFrame(buf))
            this.lastWrite = future
            return@accessStream future
        }
    }

    fun write(str: String, charset: Charset = StandardCharsets.UTF_8): ChannelFuture {
        return this.write(Unpooled.wrappedBuffer(str.toByteArray(charset)))
    }

    fun write(buf: ByteBuffer): ChannelFuture = this.write(Unpooled.wrappedBuffer(buf))

    fun write(arr: ByteArray): ChannelFuture = this.write(Unpooled.wrappedBuffer(arr))

    fun flush() {
        this.lastWrite?.addListener {
            this.stream.flush()
        }
    }

    fun finish() {
        if(!has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Cannot close context before receiving a header")
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot close context before sending a header")
        if(has(STATE_CLOSED)) throw IllegalStateException("The context is already closed")

        this.lastWrite?.addListener {
            this.stream.let { stream ->
                this.set(STATE_CLOSED)

                stream.flush()
                stream.shutdownInput().addListener {
                    stream.close()
                }
            }
        }
    }

    fun newResponseHeader(status: HttpResponseStatus, contentLength: Long) = DefaultHttp3Headers()
        .status(status.code().toString())
        .addLong("content-length", contentLength)

    fun state() = this.state
}