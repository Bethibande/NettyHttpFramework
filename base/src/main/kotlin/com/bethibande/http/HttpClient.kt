package com.bethibande.http

import com.bethibande.http.config.HttpClientConfig
import com.bethibande.http.request.RequestHandler
import io.netty.util.concurrent.Promise
import java.util.function.Consumer

abstract class HttpClient: RequestHandler() {

    abstract fun newConnection(): Promise<out HttpConnection>
    abstract fun configure(consumer: Consumer<HttpClientConfig>)
    abstract fun getConnections(): List<HttpConnection>

}