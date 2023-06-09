package com.bethibande.web

import com.bethibande.web.request.HttpRequestContext
import java.net.InetSocketAddress
import java.util.function.Consumer

interface HttpConnection {

    fun getAddress(): InetSocketAddress

    fun canRequest(): Boolean
    fun newRequest(request: Consumer<HttpRequestContext>)

}