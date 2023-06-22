package com.bethibande.web.request

import com.bethibande.web.HttpClient
import com.bethibande.web.routes.Route
import io.netty.handler.codec.http.HttpMethod

class PreparedRequest<R>(
    val method: HttpMethod,
    val path: String,
    val handler: RequestHook<R>,
    val client: HttpClient
) {

    internal val variables: Map<String, Int>
    internal val pathTokens = path.split(Route.PATH_SEPARATOR).toTypedArray()

    init {
        this.variables = parseVariables()
    }

    private fun parseVariables(): Map<String, Int> {
        val tokens = path.split(Route.PATH_SEPARATOR)

        return tokens.filter { it.startsWith(Route.VAR_PREFIX) }
            .associate { it to tokens.indexOf(it) }
    }

    fun request(): Request<R> {
        return Request<R>(this)
    }

}