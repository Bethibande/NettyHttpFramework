package com.bethibande.web

import com.bethibande.web.request.HttpRequestContext
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpClient {

    fun getTargetAddress(): InetSocketAddress

    fun newRequest(request: Consumer<HttpRequestContext>)

}