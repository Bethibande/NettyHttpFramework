package com.bethibande.web.impl.http3

import com.bethibande.web.PendingHttpConnection
import com.bethibande.web.request.HttpRequestContext
import io.netty.incubator.codec.quic.QuicChannel
import java.net.InetSocketAddress
import java.util.function.Consumer

class Http3Connection(
    private val server: Http3Server,
    private val channel: QuicChannel
): PendingHttpConnection {

    override fun getAddress(): InetSocketAddress {
        val conId = channel.remoteAddress()

    }

    override fun canRequest(): Boolean {
        TODO("Not yet implemented")
    }

    override fun newRequest(request: Consumer<HttpRequestContext>) {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isClosed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}