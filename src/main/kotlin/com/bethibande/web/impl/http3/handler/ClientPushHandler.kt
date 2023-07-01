package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Client
import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import io.netty.incubator.codec.http3.Http3PushStreamClientInitializer
import io.netty.incubator.codec.quic.QuicStreamChannel

class ClientPushHandler(
    private val client: Http3Client,
    private val connection: Http3Connection,
): Http3PushStreamClientInitializer() {

    override fun initPushStream(ch: QuicStreamChannel) {
        val context = Http3ResponseContext(this.connection, ch)

        client.handleRequest(context)
    }
}