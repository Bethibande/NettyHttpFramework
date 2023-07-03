package com.bethibande.web.types

import com.bethibande.web.request.RequestHook
import io.netty.util.concurrent.Promise

interface CanRequest {

    fun request(consumer: RequestHook): Promise<*>
    fun canRequest(): Boolean

}