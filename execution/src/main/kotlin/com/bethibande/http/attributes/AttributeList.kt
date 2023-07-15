package com.bethibande.http.attributes

import com.bethibande.http.requests.execution.RequestExecutor
import io.netty.util.AttributeKey

object AttributeList {

    val ATTRIBUTE_EXECUTOR = AttributeKey.newInstance<RequestExecutor>("requestExecutor")

}