package com.bethibande.http.requests.execution

import com.bethibande.http.HttpConnection
import com.bethibande.http.attributes.AttributeList
import com.bethibande.http.connections.ConnectionManager
import com.bethibande.http.request.RequestHook
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Promise
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class RequestExecutor(
    private val connection: HttpConnection,
) {

    private var maxRetries = 5

    private var maxStreams = 5
    private val currentStreams = AtomicInteger(0)

    private val queue = LinkedBlockingQueue<RequestDto>()

    init {
        this.connection.channel().closeFuture().addListener {
            if (queue.isNotEmpty()) {
                this.manager()?.submitQueue(this.queue)
            }
        }
    }

    fun maxRetries() = this.maxRetries
    fun maxRetries(retries: Int) {
        this.maxRetries = retries
    }

    fun maxStreams() = this.maxStreams
    fun setMaxStreams(streams: Int) {
        this.maxStreams = streams
    }

    fun currentStreams() = this.currentStreams.get()

    private fun manager(): ConnectionManager? {
        return this.connection.attr(AttributeList.ATTRIBUTE_MANAGER).get()
    }

    private fun execute() {
        this.currentStreams.incrementAndGet()

        if (!this.connection.isOpen()) return

        val request = this.queue.poll()

        if (request == null) {
            this.currentStreams.decrementAndGet()
            return
        }

        this.connection.request(request.hook, request.promise)

        request.promise.addListener {
            if (!it.isSuccess && request.tries < this.maxRetries) {
                ++request.tries

                this.submit(request)
            }

            this.currentStreams.decrementAndGet()
            this.execute()
        }
    }

    fun submit(dto: RequestDto) {
        this.queue.offer(dto)
        if (this.currentStreams() < this.maxStreams) this.execute()
    }

    fun queue(request: RequestHook): Promise<Any> {
        val promise = DefaultPromise<Any>(this.connection.channel().eventLoop())
        this.submit(RequestDto(request, promise))

        return promise
    }

}