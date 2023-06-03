package com.bethibande.web.types

import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.concurrent.Future
import java.net.InetSocketAddress
import java.util.function.BiConsumer

interface IRequestInitiator {

    fun address(): InetSocketAddress

    fun newStream(
        headerCallback: BiConsumer<Http3HeadersFrame, Boolean>,
        dataCallback: BiConsumer<Http3DataFrame, Boolean>
    ): Future<QuicStreamChannel>

}