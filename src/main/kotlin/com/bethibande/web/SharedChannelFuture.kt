package com.bethibande.web

import io.netty.channel.ChannelFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class SharedChannelFuture(vararg futures: ChannelFuture): CompletableFuture<Unit>() {

    private val state = AtomicInteger(futures.size)

    init {
        futures.forEach {
            it.addListener { this.completed() }
        }
    }

    private fun completed() {
        val now = this.state.decrementAndGet()

        if(now != 0) return

        this.complete(Unit)
    }

}