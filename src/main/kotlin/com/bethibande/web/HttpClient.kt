package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.types.CanRequest
import io.netty.util.concurrent.Promise
import java.util.function.Consumer

interface HttpClient: CanRequest {

    fun newConnection(): Promise<out HttpConnection>
    fun configure(consumer: Consumer<HttpClientConfig>)
    fun getConnections(): Collection<HttpConnection>

}