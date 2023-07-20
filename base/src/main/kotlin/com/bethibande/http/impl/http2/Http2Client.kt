package com.bethibande.http.impl.http2

import com.bethibande.http.HttpClient
import com.bethibande.http.config.HttpClientConfig
import com.bethibande.http.impl.http2.handler.ClientHandlerInitializer
import com.bethibande.http.routes.RouteRegistry
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.util.concurrent.Promise
import java.net.SocketAddress
import java.util.concurrent.Executor
import java.util.function.Consumer


class Http2Client(
    private val address: SocketAddress,
    private val sslContext: SslContext,
    private val executor: Executor,
    private val executorThreads: Int,
) : HttpClient() {

    private val eventGroup = NioEventLoopGroup(this.executorThreads, this.executor)
    private val connections = mutableListOf<Http2Connection>()

    private fun connect(channelFuture: ChannelFuture, promise: Promise<Http2Connection>) {
        if (!channelFuture.isSuccess) {
            promise.setFailure(channelFuture.cause())
            return
        }

        try {
            val connection = Http2Connection(channelFuture.channel())

            this.connections.add(connection)
            channelFuture.channel().closeFuture().addListener { this.connections.remove(connection) }

            promise.setSuccess(connection)
        } catch (th: Throwable) {
            promise.setFailure(th)
            return
        }
    }

    override fun newConnection(): Promise<out Http2Connection> {
        val promise = this.eventGroup.next().newPromise<Http2Connection>()

        try {
            val b = Bootstrap()
            b.group(this.eventGroup)
            b.channel(NioSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.remoteAddress(this.address)
            b.handler(ClientHandlerInitializer(this.sslContext))

            b.connect().addListener { this.connect(it as ChannelFuture, promise) }
        } catch (th: Throwable) {
            promise.setFailure(th)
        }

        return promise
    }

    override fun configure(consumer: Consumer<HttpClientConfig>) {
        TODO("Not yet implemented")
    }

    override fun getConnections(): List<Http2Connection> = this.connections

    override fun getRoutes(): RouteRegistry {
        TODO("Not yet implemented")
    }
}