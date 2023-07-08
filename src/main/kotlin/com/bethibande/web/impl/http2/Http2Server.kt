package com.bethibande.web.impl.http2

import com.bethibande.web.HttpServer
import com.bethibande.web.config.HttpServerConfig
import com.bethibande.web.impl.http2.handler.ServerHandlerInitializer
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.routes.Route
import com.bethibande.web.routes.RouteRegistry
import com.bethibande.web.types.Registration
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.ssl.SslContext
import java.net.InetSocketAddress
import java.util.concurrent.Executor
import java.util.function.Consumer

class Http2Server(
    private val executor: Executor,
    private val maxThreads: Int,
    private val sslContext: SslContext,
): HttpServer {

    private val eventGroup = NioEventLoopGroup(this.maxThreads, this.executor)
    private val interfaces = mutableListOf<Http2Interface>()

    private val routes = RouteRegistry()

    override fun bindInterface(address: InetSocketAddress): Registration<ChannelFuture> {
        val bootstrap = ServerBootstrap()
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
        bootstrap.group(this.eventGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(ServerHandlerInitializer(this.sslContext, this))

        val channel = bootstrap.bind(address).sync().channel()
        val inter = Http2Interface(channel)

        this.interfaces.add(inter)
        channel.closeFuture().addListener { this.interfaces.remove(inter) }

        return object: Registration<ChannelFuture> {
            override fun remove(): ChannelFuture {
                this@Http2Server.interfaces.remove(inter)
                return inter.channel.close()
            }
        }
    }

    override fun stop() {
        this.interfaces.forEach { it.channel.close() }
    }

    override fun addRoute(path: String, method: HttpMethod?, handler: Consumer<HttpResponseContext>) {
        this.routes.register(Route(path, method, handler))
    }

    override fun configure(consumer: Consumer<HttpServerConfig>) {
        TODO("Not yet implemented")
    }

    private data class Http2Interface(
        val channel: Channel,
    )

}