package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.types.CanRequest
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpClient: CanRequest {

    fun configure(consumer: Consumer<HttpClientConfig>)
    fun getConnections(): Collection<HttpConnection>

}