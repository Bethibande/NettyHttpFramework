package com.bethibande.web

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture

interface PendingHttpConnection: HttpConnection {

    fun channel(): Channel

    fun isOpen(): Boolean
    fun isClosed(): Boolean

    fun close(): ChannelFuture

}