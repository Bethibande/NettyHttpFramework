package com.bethibande.http.connections

import com.bethibande.http.HttpClient
import com.bethibande.http.HttpConnection
import com.bethibande.http.attributes.AttributeList
import com.bethibande.http.request.RequestHook
import com.bethibande.http.requests.RequestBuilder
import com.bethibande.http.requests.execution.RequestExecutor
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpScheme
import io.netty.util.concurrent.Promise
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.getOrSet

class ConnectionManager(
    private val client: HttpClient,
) {

    private var maxStreams = 5

    private var minConnections: Int = 5

    private var scheme = HttpScheme.HTTPS

    private val connectionCounter = AtomicInteger(0)

    private val localConnection = ThreadLocal<HttpConnection>()

    /**
     * Http scheme used by [prepare]
     * @see setScheme
     */
    fun scheme() = this.scheme

    /**
     * Http scheme used by [prepare]
     * @see scheme
     */
    fun setScheme(scheme: HttpScheme) {
        this.scheme = scheme
    }

    /**
     * Set max concurrent streams per connections, only applies to http/2 and 3
     * @see setMaxStreams
     */
    fun maxStreams() = this.maxStreams

    /**
     * Set max concurrent streams per connections, only applies to http/2 and 3
     * @see maxStreams
     */
    fun setMaxStreams(streams: Int) {
        this.maxStreams = streams
    }

    fun minConnections() = this.minConnections

    fun setMinConnections(connections: Int) {
        this.minConnections = connections
    }

    private fun newExecutor(connection: HttpConnection): RequestExecutor {
        val executor = RequestExecutor(connection)
        executor.setMaxStreams(this.maxStreams)

        return executor
    }

    fun newConnection(): Promise<out HttpConnection> {
        val futureConnection = this.client.newConnection()
        futureConnection.addListener {
            val connection = it.get() as HttpConnection
            connection.attr(AttributeList.ATTRIBUTE_EXECUTOR).set(this.newExecutor(connection))
        }

        return futureConnection
    }

    fun getNextConnection(): HttpConnection {
        val num = this.connectionCounter.getAndIncrement()
        val connections = this.client.getConnections()

        while (connections.size < this.minConnections) {
            this.newConnection().sync()
        }

        return connections[num % this.minConnections]
    }

    fun getConnection(): HttpConnection {
        var connection = this.localConnection.getOrSet(this::getNextConnection)

        if (connection.isClosed()) { // check if the connection has been terminated or timed out since being assigned
            this.localConnection.set(this.getNextConnection())
            connection = this.localConnection.get()
        }

        return connection
    }

    fun prepare(method: HttpMethod, path: String, hook: RequestHook): RequestBuilder {
        return RequestBuilder(method, path, this.scheme, hook)
    }

}