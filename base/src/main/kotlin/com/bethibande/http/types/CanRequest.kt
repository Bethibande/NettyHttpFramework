package com.bethibande.http.types

import com.bethibande.http.request.RequestHook
import io.netty.channel.Channel
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Promise

interface CanRequest {

    fun channel(): Channel

    fun request(consumer: RequestHook): Promise<*> {
        val promise = DefaultPromise<Any>(this.channel().eventLoop())
        this.request(consumer, promise)

        return promise
    }

    fun request(consumer: RequestHook, promise: Promise<Any>)
    fun canRequest(): Boolean

}