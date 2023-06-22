package com.bethibande.web.request

import io.netty.util.concurrent.Promise
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Request<R>(
    val preparedRequest: PreparedRequest<R>
) {

    private val variableValues = mutableMapOf<String, String>()

    fun variable(key: String, value: String) {
        if(value !in preparedRequest.variables.keys) throw IllegalArgumentException("The given key is not a known variable")
        variableValues[key] = value
    }

    fun execute(): Promise<R> {
        if(variableValues.size != preparedRequest.variables.size) throw IllegalStateException("Not all variable have been set")
        val pathTokens = preparedRequest.pathTokens.clone()

        preparedRequest.variables.forEach { (key, index) ->
            val value = URLEncoder.encode(this.variableValues[key], StandardCharsets.UTF_8)
            pathTokens[index] = value
        }

        val path = pathTokens.joinToString(separator = "/", prefix = "/")

        return this.preparedRequest.client.request {
            val header = it.newHeader(this@Request.preparedRequest.method, path)
            it.writeHeader(header)

            this@Request.preparedRequest.handler.handle(it)
        }
    }

}