package com.bethibande.http.requests

import com.bethibande.http.HttpConnection
import com.bethibande.http.attributes.AttributeList
import com.bethibande.http.request.HttpRequestContext
import com.bethibande.http.request.RequestHook
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpScheme
import io.netty.util.concurrent.Promise

class Request(
    private val method: HttpMethod,
    private val path: String,
    private val scheme: HttpScheme,
    private val hook: RequestHook,
) {

    private fun execute0(ctx: HttpRequestContext) {
        ctx.writeHeader(
            ctx.newHeader(this.method, this.path)
                .setScheme(this.scheme)
        )

        this.hook.handle(ctx)
    }

    fun execute(connection: HttpConnection): Promise<*> {
        if (connection.isClosed()) throw IllegalArgumentException("The given connection is closed")

        val executor = connection.attr(AttributeList.ATTRIBUTE_EXECUTOR).get()
            ?: throw IllegalStateException("Connection has no executor")

        return executor.queue(this::execute0)
    }

}