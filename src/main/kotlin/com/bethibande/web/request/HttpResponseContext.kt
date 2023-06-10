package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.channel.Channel

abstract class HttpResponseContext<H: AbstractHttpHeader, C: HttpConnection>(
    connection: C,
    channel: Channel
): HttpContextBase<H, C>(connection, channel) {

}