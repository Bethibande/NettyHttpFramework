package com.bethibande.http.impl.http2.handler

import com.bethibande.http.impl.http2.context.Http2ResponseContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.util.AttributeKey

class ServerStreamInitializer: ChannelInitializer<Http2StreamChannel>() {

    companion object {

        val ATTRIB_CONTEXT: AttributeKey<Http2ResponseContext> = AttributeKey.newInstance("response_context")

    }

    private val dataHandler = ServerDataHandler()

    override fun initChannel(ch: Http2StreamChannel) {
        val server = ch.parent().attr(APNHandler.ATTRIB_SERVER).get()
        val connection = ch.parent().attr(APNHandler.ATTRIB_CONNECTION).get()

        val context = Http2ResponseContext(
            connection,
            ch
        )

        ch.attr(ATTRIB_CONTEXT).set(context)

        ch.pipeline().addLast(this.dataHandler)
        server.handleRequest(context)
    }
}