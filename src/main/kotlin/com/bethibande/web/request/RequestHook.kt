package com.bethibande.web.request

fun interface RequestHook {

    fun handle(context: HttpRequestContext)

}