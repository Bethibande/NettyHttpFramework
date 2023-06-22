package com.bethibande.web.types

import com.bethibande.web.request.RequestHook
import io.netty.util.concurrent.Promise

interface CanRequest {

    fun <R> request(consumer: RequestHook<R>): Promise<R>
    fun canRequest(): Boolean

}