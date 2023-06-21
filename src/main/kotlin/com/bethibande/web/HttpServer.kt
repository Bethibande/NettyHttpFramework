package com.bethibande.web

import com.bethibande.web.config.HttpServerConfig
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.types.Registration
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpMethod
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpServer {

    companion object {
        val PATH_REGEX = Regex("//?")
    }

    /**
     * Binds a new network interface
     * @throws IllegalArgumentException if address is already bound
     */
    fun bindInterface(address: InetSocketAddress): Registration<ChannelFuture>

    /**
     * Stops the webserver, unbinds all interfaces and shuts down the executor
     */
    fun stop()

    /**
     * Register a new route
     */
    fun addRoute(path: String, method: HttpMethod? = null, handler: Consumer<HttpResponseContext>)

    fun configure(consumer: Consumer<HttpServerConfig>)

}