package com.bethibande.http.impl.http2.context

import com.bethibande.http.impl.http2.Http2Connection
import com.bethibande.http.request.AbstractHttpHeader
import com.bethibande.http.request.HttpResponseContext
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.Headers
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2StreamChannel

class Http2ResponseContext(
    override val connection: Http2Connection,
    override val channel: Http2StreamChannel,
): HttpResponseContext(connection, channel) {

    override fun convertNettyHeaders(headers: Headers<*, *, *>) = AbstractHttpHeader(headers as Http2Headers) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }

    override fun newHeaderInstance() = AbstractHttpHeader(DefaultHttp2Headers()) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }

    override fun frameData(buf: ByteBuf): Any = DefaultHttp2DataFrame(buf).stream(this.channel.stream())

    override fun closeContext() {
        super.channel.close()
    }
}