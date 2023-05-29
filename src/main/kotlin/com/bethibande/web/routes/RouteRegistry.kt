package com.bethibande.web.routes

import com.bethibande.web.types.tree.TreeMap

class RouteRegistry {

    private val routes = TreeMap<String, Route>()

    fun register(route: Route) {
        routes.put(route.pathTokens, route)
    }

    fun get(path: Array<String>): List<Route> {
        return routes.find(path)
    }

}