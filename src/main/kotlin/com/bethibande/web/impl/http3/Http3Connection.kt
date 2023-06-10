package com.bethibande.web.impl.http3

import com.bethibande.web.PendingHttpConnection
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.types.HasState
import io.netty.channel.ChannelFuture
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamType
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.AttributeMap
import java.net.InetSocketAddress
import java.util.function.Consumer

class Http3Connection(
    private val requestStreamType: QuicStreamType,
    private val channel: QuicChannel
): PendingHttpConnection, AttributeMap, HasState() {

    private var address: InetSocketAddress? = null

    private val streams = mutableListOf<Http3ResponseContext>()

    internal fun updateAddress(address: InetSocketAddress) {
        this.address = address
    }

    internal fun addStream(context: Http3ResponseContext) {
        this.streams.add(context)
        context.closeFuture().addListener { this.streams.remove(context) }
    }

    // TODO: spilling internals, channel should not be exposed to users
    override fun channel(): QuicChannel {
        return channel
    }

    override fun getAddress(): InetSocketAddress {
        return this.address ?: throw IllegalStateException("Address not yet set")
    }

    override fun canRequest(): Boolean = this.channel.peerAllowedStreams(this.requestStreamType) > 0

    override fun newRequest(request: Consumer<HttpRequestContext>) {
        TODO("Not yet implemented")
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