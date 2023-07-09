package com.bethibande.web.impl.http2.context

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpResponseContext
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.Headers
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2Error
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2StreamChannel

class Http2ResponseContext(
    override val connection: Http2Connection,
    override val channel: Http2StreamChannel,
): HttpResponseContext(connection, channel) {

    override fun convertNettyHeaders(headers: Headers<*, *, *>) = AbstractHttpHeader(headers as Http2Headers) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }

    override fun frameData(buf: ByteBuf): Any = DefaultHttp2DataFrame(buf).stream(this.channel.stream())

    override fun closeContext() {
        super.channel.write(DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR)).addListener { this.channel.close() }
    }

    override fun newHeaderInstance() = AbstractHttpHeader(DefaultHttp2Headers()) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }
}