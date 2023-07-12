package com.bethibande.web.impl.http2

import com.bethibande.web.HttpClient
import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.impl.http2.context.Http2RequestContext
import com.bethibande.web.impl.http2.handler.ClientDataHandler
import com.bethibande.web.impl.http2.handler.ClientHandlerInitializer
import com.bethibande.web.impl.http2.handler.ClientStreamInitializer
import com.bethibande.web.request.PreparedRequest
import com.bethibande.web.request.RequestHook
import com.bethibande.web.routes.RouteRegistry
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.handler.ssl.SslContext
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Promise
import java.net.InetSocketAddress
import java.util.concurrent.Executor
import java.util.function.Consumer


class Http2Client(
    private val address: InetSocketAddress,
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


    override fun request(consumer: RequestHook): Promise<*> = useConnection { connection ->
        val promise = DefaultPromise<Any>(connection.channel().eventLoop())

        Http2StreamChannelBootstrap(connection.channel())
            .handler(ClientStreamInitializer(promise, connection as Http2Connection, consumer))
            .open()

        return@useConnection promise
    }

    fun prepareRequest(method: HttpMethod, path: String, handler: RequestHook): PreparedRequest {
        return PreparedRequest(method, path, handler, this)
    }

    override fun getRoutes(): RouteRegistry {
        TODO("Not yet implemented")
    }

    override fun canRequest(): Boolean = true // TODO: implement
}