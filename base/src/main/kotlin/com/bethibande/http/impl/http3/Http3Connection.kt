package com.bethibande.http.impl.http3

import com.bethibande.http.HttpConnection
import com.bethibande.http.impl.http3.context.Http3RequestContext
import com.bethibande.http.impl.http3.context.Http3ResponseContext
import com.bethibande.http.impl.http3.handler.ClientDataHandler
import com.bethibande.http.request.HttpContextBase
import com.bethibande.http.request.RequestHook
import com.bethibande.http.types.CanRequest
import io.netty.channel.ChannelFuture
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3ServerPushStreamManager
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.incubator.codec.quic.QuicStreamType
import io.netty.util.concurrent.Promise
import java.net.SocketAddress

class Http3Connection(
    private val requestStreamType: QuicStreamType,
    override val channel: QuicChannel,
    private val pushStreamManager: Http3ServerPushStreamManager? = null
): HttpConnection(channel), CanRequest {

    private var address: SocketAddress? = null

    private val streams = mutableListOf<HttpContextBase>()

    override fun channel(): QuicChannel = this.channel

    internal fun updateAddress(address: SocketAddress) {
        this.address = address
    }

    internal fun addStream(context: Http3ResponseContext) {
        this.streams.add(context)
        context.closeFuture().addListener { this.streams.remove(context) }
    }

    override fun getRemoteAddress(): SocketAddress {
        return this.address ?: throw IllegalStateException("Address not yet set")
    }

    override fun canRequest(): Boolean {
        return this.channel.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL) > 0
    }

    override fun request(handler: RequestHook, promise: Promise<Any>) {
        val streamHandler = ClientDataHandler()

        Http3.newRequestStream(
            this.channel,
            streamHandler
        ).addListener { future ->
            val context = Http3RequestContext(
                this,
                future.get() as QuicStreamChannel,
                promise
            )

            streamHandler.setContext(context)
            handler.handle(context)
        }
    }

    override fun close(): ChannelFuture {
        set(STATE_CLOSED)
        return this.channel.close()
    }
}