package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.request.RequestHandler
import com.bethibande.web.types.CanRequest
import io.netty.util.concurrent.Promise
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max

abstract class HttpClient: RequestHandler(), CanRequest {

    private var connectionCounter = AtomicInteger(0)
    private var minConnections: Int = 1

    fun minConnections() = this.minConnections

    fun setMinConnections(connections: Int) {
        if(connections <= 0) throw IllegalArgumentException("Value must be >= 1")
        this.minConnections = connections
    }

    abstract fun newConnection(): Promise<out HttpConnection>
    abstract fun configure(consumer: Consumer<HttpClientConfig>)
    abstract fun getConnections(): List<HttpConnection>

    fun <R> useConnection(fn: (HttpConnection) -> R): R {
        val connections = this.getConnections()

        if (connections.size < this.minConnections) this.newConnection().sync()
        val connection = connections[this.connectionCounter.getAndIncrement() % connections.size]
        return fn.invoke(connection)
    }

}