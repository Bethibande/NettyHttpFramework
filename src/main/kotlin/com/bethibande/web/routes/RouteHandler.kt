package com.bethibande.web.routes

import com.bethibande.web.context.HttpResponseContext

fun interface RouteHandler {

    fun handle(ctx: HttpResponseContext)

}