package com.bethibande.web

import com.bethibande.web.context.HttpClientContext
import com.bethibande.web.handler.ClientChannelHandler
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.incubator.codec.http3.*
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.concurrent.Future
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer


class Http3Client(
    val address: InetSocketAddress
) {

    private val group = NioEventLoopGroup(1)

    private val sslContext = QuicSslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .applicationProtocols(*Http3.supportedApplicationProtocols()).build()

    private val codec = Http3.newQuicClientCodecBuilder()
        .sslContext(sslContext)
        .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
        .initialMaxData(10000000)
        .initialMaxStreamDataBidirectionalLocal(1000000)
        .build()

    private val bs = Bootstrap()

    private val channel: Channel = bs.group(group)
        .channel(NioDatagramChannel::class.java)
        .handler(codec)
        .bind(0).sync().channel()

    private val quicChannel = QuicChannel.newBootstrap(channel)
        .handler(Http3ClientConnectionHandler())
        .remoteAddress(this.address)
        .connect()
        .get()

    private val streams = mutableListOf<QuicStreamChannel>()

    internal fun newStream(headerCallback: BiConsumer<Http3HeadersFrame, Boolean>,
                           dataCallback: BiConsumer<Http3DataFrame, Boolean>): Future<QuicStreamChannel> {
        val future = Http3.newRequestStream(
            this.quicChannel,
            ClientChannelHandler(
                headerCallback,
                dataCallback
            )
        )

        future.addListener {
            val stream = future.get()
            this.streams.add(stream)

            stream.closeFuture().addListener {
                streams.remove(stream)
            }
        }

        return future
    }

    fun stream(handler: Consumer<HttpClientContext>) {
        val ctx = HttpClientContext(this)
        ctx.connect().addListener { handler.accept(ctx) }
    }

}