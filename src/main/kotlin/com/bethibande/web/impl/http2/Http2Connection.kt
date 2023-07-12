package com.bethibande.web.impl.http2

import com.bethibande.web.HttpConnection
import com.bethibande.web.types.HasState
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import java.net.InetSocketAddress

class Http2Connection(
    private val channel: Channel
): HttpConnection, HasState() {

    override fun channel(): Channel = this.channel

    override fun getAddress(): InetSocketAddress = this.channel.remoteAddress() as InetSocketAddress

    override fun isOpen(): Boolean = !super.has(STATE_CLOSED)

    override fun isClosed(): Boolean = super.has(STATE_CLOSED)

    override fun close(): ChannelFuture {
        if (super.has(STATE_CLOSED)) throw IllegalStateException("Connection is already closed")
        super.set(STATE_CLOSED)

        return this.channel.close()
    }
}