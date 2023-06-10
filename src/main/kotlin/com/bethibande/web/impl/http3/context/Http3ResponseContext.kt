package com.bethibande.web.impl.http3.context

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.Http3Header
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.types.FieldListener
import com.bethibande.web.types.ValueQueue
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicStreamChannel
import java.util.function.Consumer
import java.util.function.Function

class Http3ResponseContext(
    override val connection: Http3Connection,
    override val channel: QuicStreamChannel,
): HttpResponseContext(connection, channel) {

    companion object {

        const val STATE_HEADER_RECEIVED = 0x02
        const val STATE_HEADER_SENT = 0x04

    }

    private val headerListener = FieldListener<AbstractHttpHeader>()
    private var headers: AbstractHttpHeader by headerListener

    private val dataQueue = ValueQueue<ByteBuf>()

    @Volatile
    private var lastWrite: ChannelFuture? = null

    internal fun headerCallback(headersFrame: Http3HeadersFrame) {
        if(has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Stream already received a header")
        set(STATE_HEADER_RECEIVED)

        this.headers = Http3Header(headersFrame.headers())
    }

    internal fun dataCallback(dataFrame: Http3DataFrame) {
        val buffer = dataFrame.content()

        dataQueue.offer(buffer)
    }

    fun onHeaders(consumer: Consumer<AbstractHttpHeader>) {
        headerListener.addListener(consumer)
    }

    fun onData(consumer: Consumer<ByteBuf>) {
        this.dataQueue.consume(consumer)
    }

    private fun <R> access(fn: Function<QuicStreamChannel, R>): R {
        if(has(STATE_CLOSED)) throw IllegalStateException("Cannot access stream, the stream has already been closed")
        return fn.apply(this.channel)
    }

    fun newHeader(status: HttpResponseStatus, contentLength: Long): Http3Header {
        val header = Http3Header(DefaultHttp3Headers())
        header.setStatus(status)
        header.setContentLength(contentLength)

        return header
    }

    fun writeHeader(header: Http3Header): ChannelFuture {
        if(has(STATE_HEADER_SENT)) throw IllegalStateException("Already sent a header")
        set(STATE_HEADER_SENT)

        val future = access { stream ->
            stream.writeAndFlush(DefaultHttp3HeadersFrame(header.unwrap()))
        }

        this.lastWrite = future

        return future
    }

    fun finish() {
        if(has(STATE_CLOSED)) throw IllegalStateException("Cannot finish request, the request is already closed")
        if(!has(STATE_HEADER_SENT)) throw IllegalStateException("Cannot finish request before writing a header")
        set(STATE_CLOSED)

        this.lastWrite?.addListener {
            this.channel.shutdownInput().addListener { this.channel.close() }
        }
    }

}