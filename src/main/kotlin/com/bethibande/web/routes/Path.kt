package com.bethibande.web.routes

import com.bethibande.web.context.HttpServerContext
import io.netty.handler.codec.http.HttpMethod

class Path(
    val path: String,
    val method: HttpMethod? = null,
    val handler: PathHandler? = null
) {

    companion object {
        val PATH_SEPARATOR = Regex("//?")
    }

    private val tokens: Array<String> = this.path.split(PATH_SEPARATOR).toTypedArray()
    private val variables: Array<PathVariable> = this.collectVariables()

    private val children = arrayListOf<Path>()

    private fun collectVariables(): Array<PathVariable> {
        return this.tokens.filter { it.startsWith(":") }
            .map { PathVariable(it.substring(1), this.tokens.indexOf(it)) }
            .toTypedArray()
    }

    fun variables(): Array<PathVariable> = this.variables
    fun tokens(): Array<String> = this.tokens

    private fun extractVariables(path: String): Map<String, String> {
        val pathTokens = path.split(PATH_SEPARATOR)
        return this.variables.associate { Pair(it.name, pathTokens[it.index]) }
    }

    fun process(ctx: HttpServerContext) {
        this.handler?.let {
            ctx.variables = this.extractVariables(ctx.path)
            it.handle(ctx)
        }


        if(!ctx.isFinished()) {
            this.children.filter { it.method == null || ctx.method == it.method }
                         .forEach { it.process(ctx) }
        }
    }

    data class PathVariable(val name: String, val index: Int)

    fun path(path: String, method: HttpMethod?, handler: PathHandler?, init: Path.() -> Unit) {
        val new = Path(path, method, handler)
        init.invoke(new)

        this.children.add(new)
    }

    fun path(path: String, init: Path.() -> Unit) = this.path(path, null, null, init)

    fun get(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.GET, handler)
        this.children.add(new)
    }

    fun post(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.POST, handler)
        this.children.add(new)
    }

    fun options(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.OPTIONS, handler)
        this.children.add(new)
    }

    fun connect(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.CONNECT, handler)
        this.children.add(new)
    }

    fun delete(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.DELETE, handler)
        this.children.add(new)
    }

    fun put(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.PUT, handler)
        this.children.add(new)
    }

    fun patch(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.PATCH, handler)
        this.children.add(new)
    }

    fun head(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.HEAD, handler)
        this.children.add(new)
    }

    fun trace(path: String, handler: PathHandler) {
        val new = Path(path, HttpMethod.TRACE, handler)
        this.children.add(new)
    }

}