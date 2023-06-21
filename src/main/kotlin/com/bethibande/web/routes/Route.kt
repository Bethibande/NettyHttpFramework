package com.bethibande.web.routes

import com.bethibande.web.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import java.util.function.Consumer

class Route(
    val path: String,
    val method: HttpMethod? = null,
    val handler: Consumer<HttpResponseContext>? = null,
    val pathTokens: Array<PathNode> = pathToTokens(path),
    val vars: Map<String, Int> = this.findVariableIndexes(pathTokens)
) {

    companion object {

        val PATH_SEPARATOR = Regex("/+")
        const val VAR_PREFIX = ":"

        fun pathToTokens(path: String): Array<PathNode> {
            return path.split(PATH_SEPARATOR)
                .map(::toNode)
                .toTypedArray()
        }

        fun toNode(node: String): PathNode {
            if(node.startsWith(VAR_PREFIX)) {
                return VarPathNode(
                    node,
                    ".*"
                )
            }

            return PathNode(node)
        }

        fun findVariableIndexes(tokens: Array<PathNode>): Map<String, Int> = tokens
            .filterIsInstance<VarPathNode>()
            .associate { it.value to tokens.indexOf(it) }
    }

    fun parseVariables(path: Array<String>): Map<String, String> {
        return vars.mapValues { entry -> path[entry.value] }
    }

}