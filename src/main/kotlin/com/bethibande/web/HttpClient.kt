package com.bethibande.web

import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.types.CanRequest
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpClient<C: HttpClientConfig, CO: HttpConnection, R: HttpRequestContext>: CanRequest<R> {

    fun configure(consumer: Consumer<C>)
    fun getConnections(): Collection<CO>

}