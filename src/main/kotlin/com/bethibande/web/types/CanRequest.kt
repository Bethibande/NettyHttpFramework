package com.bethibande.web.types

import com.bethibande.web.request.HttpRequestContext
import java.util.function.Consumer

interface CanRequest {

    fun request(consumer: Consumer<HttpRequestContext>)
    fun canRequest(): Boolean

}