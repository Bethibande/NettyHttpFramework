package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpMethod
import io.netty.util.concurrent.Future

abstract class HttpRequestContext(
    connection: HttpConnection,
    channel: Channel
): HttpContextBase(connection, channel) {

    fun newHeader(method: HttpMethod, path: String): AbstractHttpHeader {
        val header = super.newHeader()
        header.setMethod(method)
        header.setPath(path)
        header.setAuthority(connection.getAddress().hostString)

        return header
    }

}