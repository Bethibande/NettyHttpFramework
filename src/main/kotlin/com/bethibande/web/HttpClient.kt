package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.types.CanRequest
import java.util.function.Consumer

interface HttpClient: CanRequest {

    fun configure(consumer: Consumer<HttpClientConfig>)
    fun getConnections(): Collection<HttpConnection>

}