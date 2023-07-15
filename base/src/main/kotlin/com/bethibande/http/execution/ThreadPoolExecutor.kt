package com.bethibande.http.execution

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class ThreadPoolExecutor(
    private val queueSize: Int = 10_000,
    private val threadLifetime: Long = 60000L,
    private val threadMinCount: Int = 1,
    private val threadMaxCount: Int = 200,
    private val threadFactory: ThreadFactory = ThreadFactory.defaultFactory(),
    private val executionType: ExecutionType,
    private val daemonThreads: Boolean = false,
    private val threadGroup: ThreadGroup = createThreadGroup(executionType)
): Executor {

    companion object {
        private val NIO_COUNTER = AtomicInteger(0)
        private val BACKGROUND_COUNTER = AtomicInteger(0)

        @JvmStatic
        fun createThreadGroup(type: ExecutionType): ThreadGroup {
            val name = when (type) {
                ExecutionType.NIO -> "nio-pool-${NIO_COUNTER.getAndIncrement()}"
                ExecutionType.BACKGROUND -> "background-pool-${BACKGROUND_COUNTER.getAndIncrement()}"
            }

            return ThreadGroup(name)
        }
    }

    private val workers = arrayOfNulls<ThreadWorker>(this.threadMaxCount)
    private val tasks = ArrayBlockingQueue<Task<*>>(this.queueSize)

    @Volatile
    private var state: ExecutorState = ExecutorState.RUNNING
    @Volatile
    private var future: CompletableFuture<Unit>? = null

    init {
        for(i in 1..this.threadMinCount) {
            this.spawnWorker()
        }
    }

    @Synchronized
    private fun spawnWorker() {
        val thread = this.threadFactory.createThread(this)
        thread.start()

        val index = this.workers.indexOf(null)
        this.workers[index] = thread
    }

    private fun findIdleWorker(): ThreadWorker? {
        return this.workers.firstOrNull { it != null && it.state == Thread.State.WAITING }
    }

    internal fun pollTask(): Task<*>? {
        return tasks.poll()
    }

    private fun offerTask(task: Task<*>) {
        if(this.state != ExecutorState.RUNNING) throw IllegalStateException("Invalid state, ThreadPoolExecutor is $state")

        this.tasks.offer(task)
    }

    @Synchronized
    internal fun removeWorker(worker: ThreadWorker) {
        this.workers[this.workers.indexOf(worker)] = null

        if(this.workers.size < this.threadMinCount) {
            this.spawnWorker()
        }
    }

    internal fun signalThreadPark() {
        // if a worker becomes idle during the shutdown procedure and the worker is the last running worker
        // it means the queue is empty and the shutdown has been completed, then it completes the shutdown future to notify
        // all threads awaiting the shutdown
        if(this.state == ExecutorState.SHUTTING_DOWN && threadActiveCount() == 1) {
            this.future?.complete(Unit)
        }
    }

    fun threadLifetime(): Long = this.threadLifetime
    fun threadMinCount(): Int = this.threadMinCount
    fun threadMaxCount(): Int = this.threadMaxCount
    fun daemonThreads(): Boolean = this.daemonThreads
    fun threadGroup(): ThreadGroup = this.threadGroup
    fun executionType(): ExecutionType = this.executionType

    fun state(): ExecutorState = this.state

    fun threadCurrentCount(): Int = this.threadGroup.activeCount()
    fun threadActiveCount(): Int = this.threadCurrentCount() - this.threadIdleCount()
    fun threadIdleCount(): Int = this.workers.size

    fun submit(task: Task<*>) {
        if(this.state != ExecutorState.RUNNING) {
            return
        }

        this.offerTask(task)

        val worker = findIdleWorker() // tries to find and wake an idle worker
        worker?.unpark()

        // if there is no idle worker that can be woken and the current thread count != max thread count, then
        // spawn a new worker thread
        if(worker == null && this.threadCurrentCount() != this.threadMaxCount) {
            this.spawnWorker()
        }
    }

    fun submit(runnable: Runnable): CompletableFuture<Void?> {
        val task = Task.of(runnable)
        submit(task)
        return task.future
    }

    fun <T> submit(callable: Callable<T>): CompletableFuture<T> {
        val task = Task.of(callable)
        submit(task)
        return task.future
    }

    override fun execute(command: Runnable) {
        this.submit(command)
    }

    fun shutdown() {
        if(this.state == ExecutorState.SHUTTING_DOWN || this.state == ExecutorState.SHUT_DOWN) {
            this.future?.join()
            return
        }

        this.state = ExecutorState.SHUTTING_DOWN

        if(this.threadActiveCount() != 0) {
            // wait until all workers are idle, the last worker will complete this future
            this.future = CompletableFuture()
            this.future?.join()
        }

        // unparking threads when state is shutting down will cause the workers to terminate
        this.workers.filterNotNull().forEach { it.unpark() }

        this.state = ExecutorState.SHUT_DOWN
    }

}