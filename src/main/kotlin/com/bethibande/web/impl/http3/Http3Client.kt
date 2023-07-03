package com.bethibande.web.impl.http3

import com.bethibande.web.HttpClient
import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.impl.http3.context.Http3RequestContext
import com.bethibande.web.impl.http3.handler.ClientDataHandler
import com.bethibande.web.impl.http3.handler.ClientPushHandler
import com.bethibande.web.request.*
import com.bethibande.web.routes.Route
import com.bethibande.web.routes.RouteRegistry
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicSslContext
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.incubator.codec.quic.QuicStreamType
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.Promise
import java.net.InetSocketAddress
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class Http3Client(
    private val address: InetSocketAddress,
    private val sslContext: QuicSslContext,
    private val executor: Executor,
    private val maxThreads: Int,
) : HttpClient, RequestHandler() {

    private val config = HttpClientConfig()

    private val group = NioEventLoopGroup(this.maxThreads, this.executor)

    private val connections = mutableListOf<Http3Connection>()

    private val routes = RouteRegistry()

    private fun createHttp3Connection(future: Future<*>, promise: Promise<Http3Connection>) {
        if (!future.isSuccess) {
            promise.setFailure(future.cause())
            return
        }

        try {
            val quicChannel = future.get() as QuicChannel
            val connection = Http3Connection(QuicStreamType.BIDIRECTIONAL, quicChannel)
            connection.updateAddress(this.address)

            this.connections.add(connection)
            quicChannel.closeFuture().addListener { this.connections.remove(connection) }

            promise.setSuccess(connection)
        } catch (th: Throwable) {
            promise.setFailure(th)
        }
    }

    private fun initiateQuicConnection(future: Future<*>, promise: Promise<Http3Connection>) {
        if (!future.isSuccess) {
            promise.setFailure(future.cause())
            return
        }
        if (future !is ChannelFuture) return

        try {
            val channel = future.channel()
            val clientHandler = Http3ClientConnectionHandler(
                null,
                { this.initPushHandler(promise.get()) },
                null,
                null,
                true
            )

            QuicChannel.newBootstrap(channel)
                .handler(clientHandler)
                .remoteAddress(this.address)
                .connect()
                .addListener { this.createHttp3Connection(it, promise) }
        } catch (th: Throwable) {
            promise.setFailure(th)
        }
    }

    override fun newConnection(): Promise<Http3Connection> {
        val promise = DefaultPromise<Http3Connection>(this.group.next())
        println("new connection")

        try {
            val codec = Http3.newQuicClientCodecBuilder()
                .sslContext(sslContext)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1_000_000)
                .initialMaxStreamsUnidirectional(1_000_000)
                .build()

            val bootstrap = Bootstrap()
            bootstrap.group(this.group)
                .channel(NioDatagramChannel::class.java)
                .handler(codec)
                .bind(0)
                .addListener { this.initiateQuicConnection(it, promise) }
        } catch (th: Throwable) {
            promise.setFailure(th)
        }

        return promise
    }

    fun useConnection(fn: (Http3Connection) -> Unit) {
        this.connections.ifEmpty {
            println("wait")
            this.newConnection().sync()
        }
        this.connections.firstOrNull()?.let { fn.invoke(it) }
    }


    override fun getRoutes(): RouteRegistry = this.routes

    private fun initPushHandler(connection: Http3Connection): ChannelHandler = ClientPushHandler(this, connection)

    override fun configure(consumer: Consumer<HttpClientConfig>) {
        consumer.accept(this.config)
    }

    fun getAddress(): InetSocketAddress = this.address

    override fun canRequest(): Boolean {
        return this.connections.any { it.channel().peerAllowedStreams(QuicStreamType.BIDIRECTIONAL) > 0 }
    }

    override fun request(handler: RequestHook): Promise<*> {
        val promise = DefaultPromise<Any>(this.group.next())
        val streamHandler = ClientDataHandler()

        this.useConnection { connection ->
            Http3.newRequestStream(
                connection.channel(),
                streamHandler
            ).addListener { future ->
                val context = Http3RequestContext(connection, future.get() as QuicStreamChannel, promise)
                streamHandler.setContext(context)
                handler.handle(context)
            }
        }

        return promise
    }

    fun prepareRequest(method: HttpMethod, path: String, handler: RequestHook): PreparedRequest {
        return PreparedRequest(method, path, handler, this)
    }

    fun addRoute(path: String, method: HttpMethod, handler: Consumer<HttpResponseContext>) {
        routes.register(Route(path, method, handler))
    }

    override fun getConnections(): Collection<Http3Connection> = this.connections
}