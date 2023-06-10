package com.bethibande.web.types

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

    private fun consume() {
        this.consumer!!.accept(queue.poll())
        size--
    }

    @Synchronized
    fun offer(value: T) {
        this.queue.offer(value)

        if(size == 0 && this.consumer != null) {
            this.consume()
        }
        size++
    }

    fun hasConsumer(): Boolean = this.consumer != null

    fun consumeAll(consumer: Consumer<Collection<T>>) {
        consumer.accept(this.queue)
    }

}