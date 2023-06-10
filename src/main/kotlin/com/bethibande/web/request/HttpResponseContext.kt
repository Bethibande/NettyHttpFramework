package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import com.bethibande.web.types.HasState
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture

abstract class HttpResponseContext(
    protected open val connection: HttpConnection,
    protected open val channel: Channel
): HasState() {

    fun connection(): HttpConnection = this.connection
    fun closeFuture(): ChannelFuture = this.channel.closeFuture()

}