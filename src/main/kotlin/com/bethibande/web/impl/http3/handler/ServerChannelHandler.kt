package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Server
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.quic.QuicChannel

class ServerChannelHandler(
    private val server: Http3Server
): ChannelInitializer<QuicChannel>() {

    override fun initChannel(p0: QuicChannel?) {

    }

}