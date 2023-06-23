package com.bethibande.web.request

import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpScheme

// TODO: as builder
// TODO: as class, as wrapper for Headers<T1, T2, T3>
interface AbstractHttpHeader {

    fun add(key: String, value: Any)
    fun set(key: String, value: Any)

    fun get(key: String): String?
    fun get(key: String, def: String): String
    fun getInt(key: String): Int?
    fun getInt(key: String, def: Int): Int
    fun getLong(key: String): Long?
    fun getLong(key: String, def: Long): Long

    fun setScheme(scheme: HttpScheme)
    fun setPath(path: String)
    fun setMethod(method: HttpMethod)
    fun setStatus(status: HttpResponseStatus)
    fun setAuthority(authority: String)
    fun setContentLength(length: Long)

    fun getScheme(): HttpScheme?
    fun getPath(): String?
    fun getMethod(): HttpMethod?
    fun getStatus(): HttpResponseStatus?
    fun getAuthority(): String?
    fun getContentLength(): Long

    fun toFrame(): Any

}