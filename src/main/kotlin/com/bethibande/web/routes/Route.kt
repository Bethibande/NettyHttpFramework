package com.bethibande.web.routes

import io.netty.handler.codec.http.HttpMethod

class Route(
    val path: String,
    val method: HttpMethod? = null,
    val pathTokens: Array<String> = path.split(PATH_SEPARATOR).toTypedArray(),
    val vars: Map<String, Int> = this.findVariableIndexes(pathTokens),
    val handler: RouteHandler? = null
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