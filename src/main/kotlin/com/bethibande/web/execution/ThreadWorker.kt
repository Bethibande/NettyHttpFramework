package com.bethibande.web.execution

import java.util.concurrent.locks.LockSupport

class ThreadWorker(
    group: ThreadGroup,
    name: String,
    type: ExecutionType,
    private val executor: ThreadPoolExecutor
): Thread(group, name) {

    private val death = System.currentTimeMillis() + executor.threadLifetime()

    init {
        isDaemon = this.executor.daemonThreads()
        this.priority = type.priority
    }

    fun unpark() {
        LockSupport.unpark(this)
    }

    override fun run() {
        while(true) {
            val task = this.executor.pollTask()

            task?.let {
                this.run0(it)
            }

            if(System.currentTimeMillis() >= death) {
                break
            }

            if(task == null) {
                this.executor.signalThreadPark()
                LockSupport.park()
            }
        }

        this.executor.removeWorker(this)
    }

    private fun <T> run0(task: Task<T>) {
        val value = task.callable.call()
        task.future.complete(value)
    }

}