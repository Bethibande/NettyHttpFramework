package com.bethibande.web.impl.http3.context

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.Http3Header
import com.bethibande.web.request.HttpResponseContext
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.Headers
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import io.netty.incubator.codec.http3.Http3Headers
import io.netty.incubator.codec.quic.QuicStreamChannel

class Http3ResponseContext(
    connection: Http3Connection,
    override val channel: QuicStreamChannel,
): HttpResponseContext(connection, channel) {

    override fun convertNettyHeaders(headers: Headers<*, *, *>): Http3Header = Http3Header(headers as Http3Headers)

    override fun frameData(buf: ByteBuf): Any = DefaultHttp3DataFrame(buf)

    override fun closeContext() {
        this.channel.shutdownOutput().addListener { this.channel.close() }
    }

    override fun newHeaderInstance(): Http3Header = Http3Header(DefaultHttp3Headers())
}