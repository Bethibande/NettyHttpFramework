package com.bethibande.web.execution

enum class ExecutionType(val priority: Int) {

    NIO(6),

    /**
     * Not used by the framework, may be used to run background tasks.
     * Worker threads of this execution type will have a thread priority of 5
     */
    BACKGROUND(5)

}