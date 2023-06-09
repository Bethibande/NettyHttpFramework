package com.bethibande.web

import io.netty.handler.ssl.SslContext
import io.netty.incubator.codec.quic.QuicSslContext
import java.util.concurrent.TimeUnit

enum class HttpServiceType {
    SERVER,
    CLIENT
}

enum class HttpVersion {
    HTTP_1,
    HTTP_2,
    HTTP_3
}

class HttpServiceBuilder(
    private val type: HttpServiceType,
    private val version: HttpVersion
) {

    private var threadsMin: Int = 1
    private var threadsMax: Int = 1
    private var threadsLifetime: Long = 60000L
    private var threadsDaemon: Boolean = false
    private var threadsQueueSize: Int = 10_000

    private var sslContext: SslContext? = null

    init {
        threadsMin = when (type) {
            HttpServiceType.SERVER -> Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            HttpServiceType.CLIENT -> 2
        }
        threadsMax = threadsMin
    }

    fun withMinThreadCount(count: Int): HttpServiceBuilder {
        this.threadsMin = count
        return this
    }

    fun withMaxThreadCount(count: Int): HttpServiceBuilder {
        this.threadsMax = count
        return this
    }

    fun withThreadLifetime(lifetime: Long, timeUnit: TimeUnit): HttpServiceBuilder {
        this.threadsLifetime = timeUnit.toMillis(lifetime)
        return this
    }

    fun withDaemonThreads(): HttpServiceBuilder {
        this.threadsDaemon = true
        return this
    }

    fun withQueueSize(size: Int): HttpServiceBuilder {
        this.threadsQueueSize = size
        return this
    }

    fun withSslContext(context: SslContext): HttpServiceBuilder {
        val isValidType = when (this.version) {
            HttpVersion.HTTP_1 -> false
            HttpVersion.HTTP_2 -> false
            HttpVersion.HTTP_3 -> context is QuicSslContext
        }

        if(!isValidType) throw IllegalArgumentException("Given ssl context is not applicable for specified http version")
        this.sslContext = context

        return this
    }

    fun buildServer(): HttpServer {
        if(this.type == HttpServiceType.CLIENT) throw IllegalStateException("This builder is building a client not a server")

        TODO("not yet implemented")
    }

    fun buildClient(): HttpClient {
        if(this.type == HttpServiceType.SERVER) throw IllegalStateException("This builder is building a server not a client")

        TODO("not yet implemented")
    }

}