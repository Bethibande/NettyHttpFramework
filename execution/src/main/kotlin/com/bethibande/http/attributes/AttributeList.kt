package com.bethibande.http.attributes

import com.bethibande.http.connections.ConnectionManager
import com.bethibande.http.requests.execution.RequestExecutor
import io.netty.util.AttributeKey

object AttributeList {

    val ATTRIBUTE_EXECUTOR = AttributeKey.newInstance<RequestExecutor>("requestExecutor")
    val ATTRIBUTE_MANAGER = AttributeKey.newInstance<ConnectionManager>("connection_manager")

}