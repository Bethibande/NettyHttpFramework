package com.bethibande.web.types

import com.bethibande.web.request.HttpRequestContext
import java.util.function.Consumer

interface CanRequest<T: HttpRequestContext> {

    fun newRequest(consumer: Consumer<T>)
    fun canRequest(): Boolean

}