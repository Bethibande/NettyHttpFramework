package com.bethibande.http

import com.bethibande.http.types.CanRequest
import com.bethibande.http.types.HasState
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.AttributeMap
import java.net.InetSocketAddress

abstract class HttpConnection(
    protected open val channel: Channel,
): HasState(), AttributeMap, CanRequest {

    init {
        this.channel().closeFuture().addListener { this.set(STATE_CLOSED) }
    }

    override fun channel(): Channel = this.channel
    abstract fun getRemoteAddress(): InetSocketAddress

    fun isOpen(): Boolean = !super.has(STATE_CLOSED)
    fun isClosed(): Boolean = super.has(STATE_CLOSED)

    abstract fun close(): ChannelFuture

    override fun <T : Any?> attr(p0: AttributeKey<T>?): Attribute<T> = this.channel.attr(p0)

    override fun <T : Any?> hasAttr(p0: AttributeKey<T>?): Boolean = this.channel.hasAttr(p0)
}