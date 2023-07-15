package com.bethibande.http.request

import com.bethibande.http.HttpServer
import com.bethibande.http.routes.RouteRegistry

abstract class RequestHandler {

    abstract fun getRoutes(): RouteRegistry

    internal fun handleRequest(context: HttpResponseContext) {
        context.onHeader { header ->
            val path = header.getPath()!!.split(HttpServer.PATH_REGEX).toTypedArray()
            val routes = getRoutes().get(path).iterator()

            while (routes.hasNext()) {
                val route = routes.next()

                if(route.method != null && route.method != header.getMethod()) continue
                if(route.handler == null) continue

                context.variables(route.parseVariables(path))
                route.handler.accept(context)

                if(!context.isOpen()) break
            }
        }
    }

}