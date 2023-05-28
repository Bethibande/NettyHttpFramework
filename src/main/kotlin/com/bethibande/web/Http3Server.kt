package com.bethibande.web

import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.handler.ServerChannelHandler
import com.bethibande.web.routes.Path
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler
import io.netty.incubator.codec.quic.QuicSslContext
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import java.net.InetSocketAddress
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

class Http3Server(
    private val executor: ThreadPoolExecutor,
    private val privateKey: PrivateKey,
    private val certificate: X509Certificate
) {

    companion object {
        const val INITIAL_MAX_DATA: Long = 4096
        const val INITIAL_MAX_DATA_BID_LOCAL: Long = 4096
        const val INITIAL_MAX_DATA_BID_REMOTE: Long = 4096
        const val INITIAL_MAX_STREAMS_BID: Long = 4096
    }

    private val interfaces = mutableListOf<Interface>()
    private val group = NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), this.executor)

    private val sslContext: QuicSslContext
    private val codec: ChannelHandler

    private val path = Path("/")

    init {
        this.sslContext = this.initSslContext()
        this.codec = this.initCodec()
    }

    private fun initSslContext(): QuicSslContext {
        return QuicSslContextBuilder.forServer(this.privateKey, null, this.certificate)
            .applicationProtocols(*Http3.supportedApplicationProtocols())
            .build()
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

    fun addInterface(socketAddress: InetSocketAddress): Registration<ChannelFuture> {
        val bs = Bootstrap()
        val channel = bs.group(this.group)
            .channel(NioDatagramChannel::class.java)
            .handler(this.codec)
            .bind(socketAddress)
            .sync()
            .channel()

        val int = Interface(bs, channel)
        this.interfaces.add(int)

        return object: Registration<ChannelFuture> {
            override fun remove(): ChannelFuture {
                interfaces.remove(int)
                return channel.close()
            }
        }
    }

    fun routes(init: Path.() -> Unit): Http3Server {
        init.invoke(this.path)
        return this
    }

    fun closeAllInterfaces(): SharedChannelFuture {
        val futures = this.interfaces.map { it.channel.close() }.toTypedArray()
        this.interfaces.clear()

        return SharedChannelFuture(*futures)
    }

    fun closeExecutor() {
        this.group.shutdownGracefully().get()
        this.executor.shutdown()
    }

    data class Interface(val bootstrap: Bootstrap, val channel: Channel)

}