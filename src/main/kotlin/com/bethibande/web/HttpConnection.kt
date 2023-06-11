package com.bethibande.web

import io.netty.channel.ChannelFuture
import java.net.InetSocketAddress

interface HttpConnection {

    fun getAddress(): InetSocketAddress

    fun isOpen(): Boolean
    fun isClosed(): Boolean

    fun close(): ChannelFuture

}