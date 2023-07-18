package com.bethibande.http.request

fun interface RequestHook {

    fun handle(context: HttpRequestContext)

}