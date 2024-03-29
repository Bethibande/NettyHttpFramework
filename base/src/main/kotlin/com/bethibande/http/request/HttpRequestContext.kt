package com.bethibande.http.request

import com.bethibande.http.HttpConnection
import io.netty.channel.Channel
import io.netty.channel.ChannelProgressivePromise
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpScheme
import io.netty.util.concurrent.Promise
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

abstract class HttpRequestContext(
    connection: HttpConnection,
    channel: Channel,
    private val promise: Promise<Any>
): HttpContextBase(connection, channel) {

    fun responseAsString(): ChannelProgressivePromise = super.readAllString({
        this.setResult(it)
        super.close()
    }, StandardCharsets.UTF_8)

    fun responseAsBytes(): ChannelProgressivePromise = super.readAllBytes {
        this.setResult(it)
        super.close()
    }

    fun onResponse(consumer: Consumer<AbstractHttpHeader>) {
        super.onHeader(consumer)
    }

    fun onStatus(status: HttpResponseStatus, fn: (AbstractHttpHeader) -> Unit) {
        this.onResponse { header ->
            if(status == header.getStatus()) fn.invoke(header)
        }
    }

    fun setResult(result: Any) {
        val state = promise.trySuccess(result)
        if(!state) throw IllegalStateException("A result has already been provided")
    }

    fun newHeader(method: HttpMethod, path: String): AbstractHttpHeader {
        val header = super.newHeader()
        header.setScheme(HttpScheme.HTTPS)
        header.setMethod(method)
        header.setPath(path)
        header.setAuthority(connection.getRemoteAddress().hostString)

        return header
    }

}