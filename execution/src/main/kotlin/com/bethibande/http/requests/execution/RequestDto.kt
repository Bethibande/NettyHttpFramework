package com.bethibande.http.requests.execution

import com.bethibande.http.request.RequestHook
import io.netty.util.concurrent.Promise

data class RequestDto(val hook: RequestHook, val promise: Promise<Any>, var tries: Int = 0)
