package com.bethibande.http

import com.bethibande.http.config.HttpServerConfig
import com.bethibande.http.request.HttpResponseContext
import com.bethibande.http.types.Registration
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpMethod
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpServer {

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