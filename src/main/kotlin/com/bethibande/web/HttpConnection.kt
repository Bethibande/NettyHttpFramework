package com.bethibande.web

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import java.net.InetSocketAddress

interface HttpConnection {

    fun channel(): Channel
    fun getAddress(): InetSocketAddress

    fun isOpen(): Boolean
    fun isClosed(): Boolean

    fun close(): ChannelFuture

}