package com.bethibande.web.impl.http3

import com.bethibande.web.HttpClient
import com.bethibande.web.HttpServer
import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.impl.http3.handler.ClientDataHandler
import com.bethibande.web.impl.http3.handler.ClientPushHandler
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.request.RequestHandler
import com.bethibande.web.routes.Route
import com.bethibande.web.routes.RouteRegistry
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicSslContext
import io.netty.incubator.codec.quic.QuicStreamType
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class Http3Client(
    private val address: InetSocketAddress,
    private val sslContext: QuicSslContext,
    private val executor: ThreadPoolExecutor
): HttpClient, RequestHandler() {

    private val config = HttpClientConfig()

    private val group = NioEventLoopGroup(this.executor.threadMaxCount(), this.executor)

    private val codec = Http3.newQuicClientCodecBuilder()
        .sslContext(sslContext)
        .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
        .initialMaxData(10000000)
        .initialMaxStreamDataBidirectionalLocal(1000000)
        .initialMaxStreamsUnidirectional(1000000)
        .build()

    private val bs = Bootstrap()

    private val channel: Channel = bs.group(this.group)
        .channel(NioDatagramChannel::class.java)
        .handler(codec)
        .bind(0)
        .sync()
        .channel()

    private val quicChannel: QuicChannel = QuicChannel.newBootstrap(channel) // TODO: configuration
        .handler(Http3ClientConnectionHandler(
            null,
            this::initPushHandler,
            null,
            null,
            true
        ))
        .remoteAddress(this.address)
        .connect()
        .get()

    private val connection = Http3Connection(QuicStreamType.BIDIRECTIONAL, this.quicChannel)

    private val routes = RouteRegistry()

    override fun getRoutes(): RouteRegistry = this.routes

    private fun initPushHandler(pushId: Long): ChannelHandler = ClientPushHandler(this)

    override fun configure(consumer: Consumer<HttpClientConfig>) {
        consumer.accept(this.config)
    }

    fun getAddress(): InetSocketAddress = this.address

    override fun canRequest(): Boolean = this.quicChannel.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL) > 0

    override fun request(handler: Consumer<HttpRequestContext>) {
        Http3.newRequestStream(
            this.quicChannel,
            ClientDataHandler(this, handler)
        )
    }

    fun addRoute(path: String, method: HttpMethod, handler: Consumer<HttpResponseContext>) {
        routes.register(Route(path, method, handler))
    }

    fun connection(): Http3Connection = this.connection

    override fun getConnections(): Collection<Http3Connection> = listOf(this.connection)
}