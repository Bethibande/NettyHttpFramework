package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpResponseStatus

abstract class HttpResponseContext(
    connection: HttpConnection,
    channel: Channel
): HttpContextBase(connection, channel) {

    fun getRequestHeader() = super.header

    fun newHeader(status: HttpResponseStatus, contentLength: Long): AbstractHttpHeader {
        val header = super.newHeader()
        header.setStatus(status)
        header.setContentLength(contentLength)

        return header
    }

}