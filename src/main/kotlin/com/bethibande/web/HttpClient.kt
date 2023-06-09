package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.request.HttpRequestContext
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpClient<C: HttpClientConfig>: HttpConnection {

    fun configure(consumer: Consumer<C>)

}