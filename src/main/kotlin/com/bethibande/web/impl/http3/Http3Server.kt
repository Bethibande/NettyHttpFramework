package com.bethibande.web.impl.http3

import com.bethibande.web.HttpServer
import com.bethibande.web.PendingHttpConnection
import com.bethibande.web.config.HttpServerConfig
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.impl.http3.handler.ServerChannelHandler
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.routes.RouteRegistry
import com.bethibande.web.types.Registration
import io.netty.bootstrap.Bootstrap
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
): HttpServer<HttpServerConfig> {

    companion object {
        const val INITIAL_MAX_DATA: Long = 4096
        const val INITIAL_MAX_DATA_BID_LOCAL: Long = 4096
        const val INITIAL_MAX_DATA_BID_REMOTE: Long = 4096
        const val INITIAL_MAX_STREAMS_BID: Long = 4096
    }

    private val group = NioEventLoopGroup(this.executor.threadMinCount(), this.executor)
    private var codec: ChannelHandler

    private val routes = RouteRegistry()
    private val connections = mutableListOf<PendingHttpConnection>()

    init {
        this.codec = this.initCodec()
    }

    private fun initCodec(): ChannelHandler {
        return Http3.newQuicServerCodecBuilder()
            .sslContext(this.sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(INITIAL_MAX_DATA)
            .initialMaxStreamDataBidirectionalLocal(INITIAL_MAX_DATA_BID_LOCAL)
            .initialMaxStreamDataBidirectionalRemote(INITIAL_MAX_DATA_BID_REMOTE)
            .initialMaxStreamsBidirectional(INITIAL_MAX_STREAMS_BID)
            .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
            .handler(ServerChannelHandler(this))
            .build()
    }

    override fun bindInterface(address: InetSocketAddress): Registration<ChannelFuture> {
        val bs = Bootstrap()
        val channel = bs.group(this.group)
            .channel(NioDatagramChannel::class.java)
            .handler(this.codec)
            .bind(address)
            .sync()
            .channel()

        return object: Registration<ChannelFuture> {
            override fun remove(): ChannelFuture {
                return channel.close()
            }
        }
    }

    override fun stop() {
        group.shutdownGracefully().sync()
    }

    override fun addRoute(path: String, method: HttpMethod?, handler: Consumer<HttpResponseContext>) {
        TODO("Not yet implemented")
    }

    override fun configure(consumer: Consumer<HttpServerConfig>) {
        TODO("Not yet implemented")
    }
}