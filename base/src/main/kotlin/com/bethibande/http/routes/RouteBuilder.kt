package com.bethibande.http.routes

import com.bethibande.http.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import java.util.function.Consumer

typealias ResponseHook = Consumer<HttpResponseContext>

class RouteBuilder(
    private val registry: RouteRegistry,
    private val path: String,
) {

    companion object {
        private val PATH_REPLACE_REGEX = Regex("//+")
    }

    private fun contactPath(path: String?): String {
        if (path == null) return this.path
        return (this.path + path).replace(PATH_REPLACE_REGEX, "/")
    }

    fun get(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.GET, handler))
    }

    fun post(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.POST, handler))
    }

    fun delete(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.DELETE, handler))
    }

    fun patch(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.PATCH, handler))
    }

    fun put(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.PUT, handler))
    }

    fun head(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.HEAD, handler))
    }

    fun options(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.OPTIONS, handler))
    }

    fun connect(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.CONNECT, handler))
    }

    fun trace(path: String? = null, handler: ResponseHook) {
        this.registry.register(Route(this.contactPath(path), HttpMethod.TRACE, handler))
    }

    fun path(path: String, handler: ResponseHook? = null, fn: (RouteBuilder.() -> Unit)? = null) {
        val pathAbsolute = this.contactPath(path)

        handler?.let { this.registry.register(Route(pathAbsolute, null, handler)) }
        fn?.let { fn.invoke(RouteBuilder(this.registry, pathAbsolute)) }
    }

}