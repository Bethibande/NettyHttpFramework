package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpScheme
import io.netty.util.concurrent.Promise
import java.util.function.Consumer

abstract class HttpRequestContext<R>(
    connection: HttpConnection,
    channel: Channel,
    private val promise: Promise<R>
): HttpContextBase(connection, channel) {

    fun onResponse(consumer: Consumer<AbstractHttpHeader>) {
        super.onHeader(consumer)
    }

    fun onStatus(status: HttpResponseStatus, fn: (AbstractHttpHeader) -> Unit) {
        this.onResponse { header ->
            if(status == header.getStatus()) fn.invoke(header)
        }
    }

    fun setResult(result: R) {
        val state = promise.trySuccess(result)
        if(!state) throw IllegalStateException("A result has already been provided")
    }

    fun newHeader(method: HttpMethod, path: String): AbstractHttpHeader {
        val header = super.newHeader()
        header.setScheme(HttpScheme.HTTPS)
        header.setMethod(method)
        header.setPath(path)
        header.setAuthority(connection.getAddress().hostString)

        return header
    }

}