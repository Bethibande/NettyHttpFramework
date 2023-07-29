package com.bethibande.http.routes

import com.bethibande.http.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

class Route(
    val path: String,
    val method: HttpMethod? = null,
    val handler: Consumer<HttpResponseContext>? = null,
    val pathTokens: Array<PathNode> = pathToTokens(path),
    private val vars: Map<String, Int> = findVariableIndexes(pathTokens)
) {

    companion object {

        val PATH_SEPARATOR = Regex("//?")
        const val VAR_PREFIX = ":"

        fun pathToTokens(path: String): Array<PathNode> {
            return path.split(PATH_SEPARATOR)
                .map(::toNode)
                .toTypedArray()
        }

        private fun toNode(node: String): PathNode {
            if(node.startsWith(VAR_PREFIX)) {
                return VarPathNode(
                    node.substring(1),
                    ".*"
                )
            }

            return PathNode(node)
        }

        fun findVariableIndexes(tokens: Array<PathNode>): Map<String, Int> =
            tokens.mapIndexed { index, pathNode -> pathNode to index }
            .filter { it.first is VarPathNode }
            .associate { it.first.value to it.second }
    }

    fun parseVariables(path: Array<String>): Map<String, String> {
        return vars.mapValues { entry -> URLDecoder.decode(path[entry.value], StandardCharsets.UTF_8) }
    }

}