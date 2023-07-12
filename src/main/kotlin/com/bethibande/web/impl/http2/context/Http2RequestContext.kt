package com.bethibande.web.impl.http2.context

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpRequestContext
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.Headers
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2Error
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.util.concurrent.Promise

class Http2RequestContext(
    connection: Http2Connection,
    override val channel: Http2StreamChannel,
    promise: Promise<Any>
): HttpRequestContext(connection, channel, promise) {

    internal fun channel() = this.channel

    override fun convertNettyHeaders(headers: Headers<*, *, *>) = AbstractHttpHeader(headers as Http2Headers) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }

    override fun newHeaderInstance(): AbstractHttpHeader = AbstractHttpHeader(DefaultHttp2Headers()) {
        DefaultHttp2HeadersFrame(it as Http2Headers).stream(this.channel.stream())
    }

    override fun frameData(buf: ByteBuf): Any = DefaultHttp2DataFrame(buf).stream(this.channel.stream())

    override fun closeContext() {
        super.channel.close()
    }
}