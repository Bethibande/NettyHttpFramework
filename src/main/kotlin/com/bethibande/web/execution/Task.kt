package com.bethibande.web.execution

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

data class Task<T>(val future: CompletableFuture<T>, val callable: Callable<T>) {

    companion object {

        @JvmStatic
        fun <T> of(callable: Callable<T>): Task<T> {
            return Task(CompletableFuture<T>(), callable)
        }

        @JvmStatic
        fun of(runnable: Runnable): Task<Void?> {
            return Task(CompletableFuture<Void?>()) {
                runnable.run()
                return@Task null
            }
        }
    }
}