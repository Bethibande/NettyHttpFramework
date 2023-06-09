package com.bethibande.web.routes

import com.bethibande.web.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import java.util.function.Consumer

class Route(
    val path: String,
    val method: HttpMethod? = null,
    val handler: Consumer<HttpResponseContext>? = null,
    val pathTokens: Array<String> = path.split(PATH_SEPARATOR).toTypedArray(),
    val vars: Map<String, Int> = this.findVariableIndexes(pathTokens)
) {

    companion object {

        val PATH_SEPARATOR = Regex("//?")
        const val VAR_PREFIX = ":"
        const val VAR_REGEX = "[^\\/]+"

        fun findVariableIndexes(tokens: Array<String>): Map<String, Int> = tokens
            .filter { it.startsWith(VAR_PREFIX) }
            .associate { it.substring(1) to tokens.indexOf(it) }
    }

    init {
        this.replaceVarsWithRegex()
    }

    private fun replaceVarsWithRegex() {
        this.vars.values.forEach {
            this.pathTokens[it] = VAR_REGEX
        }
    }


}