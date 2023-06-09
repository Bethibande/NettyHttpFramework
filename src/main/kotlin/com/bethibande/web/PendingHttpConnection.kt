package com.bethibande.web

interface PendingHttpConnection: HttpConnection {

    fun isOpen(): Boolean
    fun isClosed(): Boolean

    fun close()

}