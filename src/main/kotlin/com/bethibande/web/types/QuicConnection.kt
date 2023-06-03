package com.bethibande.web.types

import com.bethibande.web.Http3Server
import com.bethibande.web.context.HttpRequestContext
import com.bethibande.web.handler.RequestChannelHandler
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.incubator.codec.quic.QuicStreamType
import io.netty.util.concurrent.Future
import java.net.InetSocketAddress
import java.util.function.BiConsumer
import java.util.function.Consumer

class QuicConnection(
    private val channel: QuicChannel,
    private val owner: Http3Server
): IRequestInitiator {

    companion object {
        const val STATE_INITIAL = 0x00
        const val STATE_CLOSED = 0x01
    }

    init {
        channel.closeFuture().addListener { this.disconnect() }
    }

    private val streams = mutableListOf<QuicStreamChannel>()

    @Volatile
    private var state = STATE_INITIAL

    private fun has(state: Int): Boolean = this.state and state == state
    private fun set(state: Int) {
        this.state = this.state or state
    }

    private fun disconnect() {
        if(has(STATE_CLOSED)) throw IllegalStateException("Connection already closed")
        set(STATE_CLOSED)
        this.owner.disconnect(this)
    }

    fun isClosed() = has(STATE_CLOSED)
    fun isOpen() = !has(STATE_CLOSED)

    override fun address(): InetSocketAddress = channel.localAddress() as InetSocketAddress // TODO: fix, throws exception if called

    override fun newStream(
        headerCallback: BiConsumer<Http3HeadersFrame, Boolean>,
        dataCallback: BiConsumer<Http3DataFrame, Boolean>
    ): Future<QuicStreamChannel> {
        val allowed = this.channel.peerAllowedStreams(QuicStreamType.UNIDIRECTIONAL)
        if(allowed == 0L) throw IllegalStateException("Cannot create any more streams, stream limit reached")

        val future = this.channel.newStreamBootstrap()
            .type(QuicStreamType.UNIDIRECTIONAL)
            .handler(RequestChannelHandler(headerCallback, dataCallback))
            .create()

        future.addListener {
            val stream = future.get()
            this.streams.add(stream)

            stream.closeFuture().addListener {
                streams.remove(stream)
            }
        }

        return future
    }

    fun stream(handler: Consumer<HttpRequestContext>) {
        val ctx = HttpRequestContext(this)
        ctx.connect().addListener { handler.accept(ctx) }
    }

}