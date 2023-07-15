package com.bethibande.http.impl.http2

import com.bethibande.http.HttpConnection
import com.bethibande.http.impl.http2.handler.ClientStreamInitializer
import com.bethibande.http.request.RequestHook
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Promise
import java.net.InetSocketAddress

class Http2Connection(
    channel: Channel
): HttpConnection(channel) {

    private val streamInitializer = ClientStreamInitializer()

    override fun getRemoteAddress(): InetSocketAddress = this.channel.remoteAddress() as InetSocketAddress

    override fun close(): ChannelFuture {
        if (super.has(STATE_CLOSED)) throw IllegalStateException("Connection is already closed")
        super.set(STATE_CLOSED)

        return this.channel.close()
    }

    override fun request(consumer: RequestHook, promise: Promise<Any>) {
        Http2StreamChannelBootstrap(this.channel)
            .attr(ClientStreamInitializer.ATTRIB_CONNECTION, this)
            .attr(ClientStreamInitializer.ATTRIB_PROMISE, promise)
            .attr(ClientStreamInitializer.ATTRIB_HOOK, consumer)
            .handler(this.streamInitializer)
            .open()
    }

    override fun canRequest(): Boolean = true
}