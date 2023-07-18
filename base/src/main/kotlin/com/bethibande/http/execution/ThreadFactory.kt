package com.bethibande.http.execution

import java.util.concurrent.atomic.AtomicLong

interface ThreadFactory {

    companion object {
        private val NIO_COUNTER = AtomicLong(0)
        private val BACKGROUND_COUNTER = AtomicLong(0)

        @JvmStatic
        fun defaultFactory(): ThreadFactory {
            return object : ThreadFactory {
                override fun createThread(executor: ThreadPoolExecutor): ThreadWorker {
                    val name = when (executor.executionType()) {
                        ExecutionType.NIO -> "nio-exec-${NIO_COUNTER.getAndIncrement()}"
                        ExecutionType.BACKGROUND -> "background-exec-${BACKGROUND_COUNTER.getAndIncrement()}"
                    }

                    return ThreadWorker(executor.threadGroup(), name, executor.executionType(), executor)
                }
            }
        }
    }

    fun createThread(executor: ThreadPoolExecutor): ThreadWorker

}