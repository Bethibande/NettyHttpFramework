package com.bethibande.web.config

import java.util.function.Supplier

open class HttpClientConfig {

    var basePath: String = ""
    var defaultHeaders: MutableList<Pair<String, Supplier<String>>> = mutableListOf()

}