package com.bethibande.web.impl.http3

import com.bethibande.web.HttpServer
import com.bethibande.web.config.HttpServerConfig
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.impl.http3.handler.ServerConnectionHandler
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.request.RequestHandler
import com.bethibande.web.routes.Route
import com.bethibande.web.routes.RouteRegistry
import com.bethibande.web.types.Registration
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler
import io.netty.incubator.codec.quic.QuicSslContext
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class Http3Server(
    private val executor: ThreadPoolExecutor,
    private val sslContext: QuicSslContext
): HttpServer, RequestHandler() {

    companion object {
        const val INITIAL_MAX_DATA: Long = 4096
        const val INITIAL_MAX_DATA_BID_LOCAL: Long = 4096
        const val INITIAL_MAX_DATA_BID_REMOTE: Long = 4096
        const val INITIAL_MAX_STREAMS_BID: Long = 4096
    }

    private val group = NioEventLoopGroup(this.executor.threadMinCount(), this.executor)
    private var codec: ChannelHandler

    private val interfaces = mutableListOf<Channel>()
    private val routes = RouteRegistry()
    private val connections = mutableListOf<Http3Connection>()

    init {
        this.codec = this.initCodec()
    }

    private fun initCodec(): ChannelHandler {
        return Http3.newQuicServerCodecBuilder() // TODO: configuration
            .sslContext(this.sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(INITIAL_MAX_DATA)
            .initialMaxStreamDataBidirectionalLocal(INITIAL_MAX_DATA_BID_LOCAL)
            .initialMaxStreamDataBidirectionalRemote(INITIAL_MAX_DATA_BID_REMOTE)
            .initialMaxStreamsBidirectional(INITIAL_MAX_STREAMS_BID)
            .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
            .handler(ServerConnectionHandler(this))
            .build()
    }

    override fun getRoutes(): RouteRegistry = this.routes

    internal fun addConnection(connection: Http3Connection) {
        this.connections.add(connection)
        connection.channel().closeFuture().addListener { this.removeConnection(connection) }
    }

    private fun removeConnection(connection: Http3Connection) {
        this.connections.add(connection)
    }

    override fun bindInterface(address: InetSocketAddress): Registration<ChannelFuture> {
        val bs = Bootstrap()
        val channel = bs.group(this.group)
            .channel(NioDatagramChannel::class.java)
            .handler(this.codec)
            .bind(address)
            .sync()
            .channel()

        this.interfaces.add(channel)

        return object: Registration<ChannelFuture> {
            override fun remove(): ChannelFuture {
                interfaces.remove(channel)
                return channel.close()
            }
        }
    }

    override fun stop() {
        this.interfaces.forEach { it.close() }
        this.interfaces.forEach { it.closeFuture().sync() }
        group.shutdownGracefully().sync()
    }

    override fun addRoute(path: String, method: HttpMethod?, handler: Consumer<HttpResponseContext>) {
        routes.register(Route(path, method, handler))
    }

    override fun configure(consumer: Consumer<HttpServerConfig>) {
        TODO("Not yet implemented")
    }
}