package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpMethod
import io.netty.util.concurrent.Promise

abstract class HttpRequestContext<R>(
    connection: HttpConnection,
    channel: Channel,
    private val promise: Promise<R>
): HttpContextBase(connection, channel) {

    fun setResult(result: R) {
        val state = promise.trySuccess(result)
        if(!state) throw IllegalStateException("A result has already been provided")
    }

    fun newHeader(method: HttpMethod, path: String): AbstractHttpHeader {
        val header = super.newHeader()
        header.setMethod(method)
        header.setPath(path)
        header.setAuthority(connection.getAddress().hostString)

        return header
    }

}