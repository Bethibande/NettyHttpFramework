package com.bethibande.web.request

fun interface RequestHook<R> {

    fun handle(context: HttpRequestContext<R>)

}