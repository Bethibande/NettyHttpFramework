package com.bethibande.web

interface HttpConnection {

    fun server(): HttpServer
    fun client(): HttpClient

    fun canPush(): Boolean
    fun canRequest(): Boolean

    fun isClosed(): Boolean
    fun isOpen(): Boolean

}