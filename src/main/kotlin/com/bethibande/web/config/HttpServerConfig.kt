package com.bethibande.web.config

import java.util.function.Supplier

open class HttpServerConfig {

    var defaultHeaders: MutableList<Pair<String, Supplier<String>>> = mutableListOf()

}