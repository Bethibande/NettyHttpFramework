package com.bethibande.http.requests.execution

import com.bethibande.http.HttpConnection
import com.bethibande.http.request.RequestHook
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Promise
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class RequestExecutor(
    private val connection: HttpConnection,
) {

    private var maxStreams = 5
    private val currentStreams = AtomicInteger(0)

    private val queue = LinkedBlockingQueue<RequestDto>()

    fun maxStreams() = this.maxStreams
    fun setMaxStreams(streams: Int) {
        this.maxStreams = streams
    }

    fun currentStreams() = this.currentStreams.get()

    private fun execute() {
        this.currentStreams.incrementAndGet()

        val request = this.queue.poll()

        if (request == null) {
            this.currentStreams.decrementAndGet()
            return
        }

        this.connection.request(request.hook, request.promise)

        request.promise.addListener {
            this.currentStreams.decrementAndGet()
            this.execute()
        }
    }

    fun queue(request: RequestHook): Promise<Any> {
        val promise = DefaultPromise<Any>(this.connection.channel().eventLoop())
        this.queue.offer(RequestDto(request, promise))

        if (this.currentStreams() < this.maxStreams) this.execute()

        return promise
    }

    private data class RequestDto(val hook: RequestHook, val promise: Promise<Any>)

}