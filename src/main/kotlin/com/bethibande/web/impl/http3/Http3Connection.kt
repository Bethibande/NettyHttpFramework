package com.bethibande.web.impl.http3

import com.bethibande.web.HttpConnection
import com.bethibande.web.impl.http3.context.Http3RequestContext
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import com.bethibande.web.request.HttpContextBase
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.request.RequestHook
import com.bethibande.web.types.CanRequest
import com.bethibande.web.types.HasState
import io.netty.channel.ChannelFuture
import io.netty.incubator.codec.http3.Http3ServerPushStreamManager
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.incubator.codec.quic.QuicStreamType
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.AttributeMap
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.Promise
import java.net.InetSocketAddress
import java.util.function.Consumer

class Http3Connection(
    private val requestStreamType: QuicStreamType,
    private val channel: QuicChannel,
    private val pushStreamManager: Http3ServerPushStreamManager? = null
): HttpConnection, AttributeMap, HasState(), CanRequest {

    private var address: InetSocketAddress? = null

    private val streams = mutableListOf<HttpContextBase>()

    internal fun channel(): QuicChannel = this.channel

    internal fun updateAddress(address: InetSocketAddress) {
        this.address = address
    }

    internal fun addStream(context: Http3ResponseContext) {
        this.streams.add(context)
        context.closeFuture().addListener { this.streams.remove(context) }
    }

    override fun getAddress(): InetSocketAddress {
        return this.address ?: throw IllegalStateException("Address not yet set")
    }

    override fun canRequest(): Boolean = this.channel.peerAllowedStreams(this.requestStreamType) > 0

    override fun request(handler: RequestHook): Promise<Any> {
        if(pushStreamManager != null) {
            val pushId = pushStreamManager.reserveNextPushId()
            val futureStream: Future<QuicStreamChannel> = pushStreamManager.newPushStream(pushId, null) // TODO: channel handler to handle cancel frame from client

            val promise = DefaultPromise<Any>(this.channel.eventLoop())

            futureStream.addListener {
                val stream = futureStream.get()
                val context = Http3RequestContext(this, stream, promise)

                this.streams.add(context)
                context.closeFuture().addListener { this.streams.remove(context) }

                handler.handle(context)
            }

            return promise
        }

        throw IllegalStateException("The given connection does not support push requests")
    }

    override fun isOpen(): Boolean = !has(STATE_CLOSED)

    override fun isClosed(): Boolean = has(STATE_CLOSED)

    override fun close(): ChannelFuture {
        set(STATE_CLOSED)
        return this.channel.close()
    }

    override fun <T : Any?> attr(key: AttributeKey<T>?): Attribute<T> = this.channel.attr(key)

    override fun <T : Any?> hasAttr(key: AttributeKey<T>?): Boolean = this.channel.hasAttr(key)
}