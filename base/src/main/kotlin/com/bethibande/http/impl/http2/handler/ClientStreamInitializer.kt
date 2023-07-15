package com.bethibande.http.impl.http2.handler

import com.bethibande.http.impl.http2.Http2Connection
import com.bethibande.http.impl.http2.context.Http2RequestContext
import com.bethibande.http.request.RequestHook
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.util.AttributeKey
import io.netty.util.concurrent.Promise

class ClientStreamInitializer: ChannelInitializer<Http2StreamChannel>() {

    companion object {

        val ATTRIB_PROMISE: AttributeKey<Promise<Any>> = AttributeKey.newInstance("request_promise")
        val ATTRIB_CONNECTION: AttributeKey<Http2Connection> = AttributeKey.newInstance("http2_client_connection")
        val ATTRIB_HOOK: AttributeKey<RequestHook> = AttributeKey.newInstance("http2_hook")

        val ATTRIB_CONTEXT: AttributeKey<Http2RequestContext> = AttributeKey.newInstance("request_context")

    }

    private val dataHandler = ClientDataHandler()

    override fun initChannel(ch: Http2StreamChannel) {
        val connection = ch.attr(ATTRIB_CONNECTION).get()
        val promise = ch.attr(ATTRIB_PROMISE).get()
        val hook = ch.attr(ATTRIB_HOOK).get()

        val context = Http2RequestContext(connection, ch, promise)

        ch.attr(ATTRIB_CONTEXT).set(context)

        ch.pipeline().addLast(this.dataHandler)
        hook.handle(context)
    }
}