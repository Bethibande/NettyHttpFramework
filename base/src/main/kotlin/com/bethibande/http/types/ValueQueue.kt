package com.bethibande.http.types

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.function.Consumer

class ValueQueue<T> {

    private val queue = ConcurrentLinkedDeque<T>()
    @Volatile
    private var size = 0

    @Volatile
    private var consumer: Consumer<T>? = null

    fun consume(consumer: Consumer<T>) {
        this.consumer = consumer

        while(size != 0) {
            this.consume()
        }
    }

    @Synchronized
    private fun consume() {
        if(this.consumer == null && size != 0) return

        size--
        val value = queue.poll()
        this.consumer!!.accept(value)
    }

    fun offer(value: T) {
        this.queue.offer(value)
        size++

        this.consume()
    }

    fun hasConsumer(): Boolean = this.consumer != null

    fun consumeAll(consumer: Consumer<Collection<T>>) {
        consumer.accept(this.queue)
    }

}