package com.bethibande.http.requests

import com.bethibande.http.request.RequestHook
import com.bethibande.http.routes.Route
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpScheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RequestBuilder(
    private val method: HttpMethod,
    private val path: String,
    private val scheme: HttpScheme,
    private val hook: RequestHook,
) {

    private val pathTokens: Collection<String>
    private val pathVariables: Map<String, Int>

    private val variables = mutableMapOf<String, String>()

    init {
        if (this.path.isEmpty()) throw IllegalArgumentException("The given path must have at least one character")

        this.pathTokens = this.path.substring(1).split(Route.PATH_SEPARATOR)
        this.pathVariables = this.pathTokens.filter { it.startsWith(Route.VAR_PREFIX) }
            .associate { it.substring(1) to this.pathTokens.indexOf(it) }
    }

    fun withPathVariable(pair: Pair<String, String>): RequestBuilder {
        this.variables[pair.first] = pair.second
        return this
    }

    fun build(): Request {
        val pathArray = ArrayList(this.pathTokens)

        this.variables.forEach { (key, value) ->
            this.pathVariables[key]?.let { index ->
                pathArray[index] = URLEncoder.encode(value, StandardCharsets.UTF_8)
            }
        }

        val path = pathArray.joinToString(prefix = "/", separator = "/")

        return Request(this.method, path, this.scheme, this.hook)
    }

}