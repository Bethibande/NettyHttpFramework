package com.bethibande.web.routes

import com.bethibande.web.context.HttpServerContext

fun interface PathHandler {

    fun handle(ctx: HttpServerContext)

}