package com.bethibande.web.routes

import com.bethibande.web.context.HttpServerContext

fun interface RouteHandler {

    fun handle(ctx: HttpServerContext)

}