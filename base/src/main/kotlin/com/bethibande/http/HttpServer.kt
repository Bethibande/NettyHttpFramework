package com.bethibande.http

import com.bethibande.http.config.HttpServerConfig
import com.bethibande.http.request.HttpResponseContext
import com.bethibande.http.request.RequestHandler
import com.bethibande.http.routes.Route
import com.bethibande.http.types.Registration
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpMethod
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.AttributeMap
import io.netty.util.DefaultAttributeMap
import java.net.SocketAddress
import java.util.function.Consumer

abstract class HttpServer: RequestHandler(), AttributeMap {

    private val attributeMap = DefaultAttributeMap()

    /**
     * Binds a new network interface
     * @throws IllegalArgumentException if address is already bound
     */
    abstract fun bindInterface(address: SocketAddress): Registration<ChannelFuture>

    /**
     * Stops the webserver, unbinds all interfaces and shuts down the executor
     */
    abstract fun stop()

    /**
     * Register a new route
     */
    fun addRoute(path: String, method: HttpMethod? = null, handler: Consumer<HttpResponseContext>) {
        this.getRoutes().register(Route(path, method, handler))
    }

    abstract fun configure(consumer: Consumer<HttpServerConfig>)

    override fun <T : Any?> attr(p0: AttributeKey<T>?): Attribute<T> = this.attributeMap.attr(p0)

    override fun <T : Any?> hasAttr(p0: AttributeKey<T>?): Boolean = this.attributeMap.hasAttr(p0)
}