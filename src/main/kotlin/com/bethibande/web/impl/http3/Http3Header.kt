package com.bethibande.web.impl.http3

import com.bethibande.web.request.AbstractHttpHeader
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpScheme
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3Headers

class Http3Header(
    private val headers: Http3Headers
): AbstractHttpHeader {

    override fun add(key: String, value: Any) {
        this.headers.add(key, value.toString())
    }

    override fun set(key: String, value: Any) {
        this.headers.set(key, value.toString())
    }

    override fun get(key: String): String? = this.headers.get(key)?.toString()

    override fun get(key: String, def: String): String = this.headers.get(key, def).toString()

    override fun getInt(key: String): Int? = this.headers.getInt(key)

    override fun getInt(key: String, def: Int): Int = this.headers.getInt(key, def)

    override fun getLong(key: String): Long? = this.headers.getLong(key)

    override fun getLong(key: String, def: Long): Long = this.headers.getLong(key, def)

    override fun setPath(path: String) {
        this.headers.path(path)
    }

    override fun setScheme(scheme: HttpScheme) {
        this.headers.scheme(scheme.name())
    }

    override fun setMethod(method: HttpMethod) {
        this.headers.method(method.toString())
    }

    override fun setStatus(status: HttpResponseStatus) {
        this.headers.status(status.reasonPhrase())
    }

    override fun setAuthority(authority: String) {
        this.headers.authority(authority)
    }

    override fun setContentLength(length: Long) {
        this.set("content-length", length)
    }

    override fun getScheme(): HttpScheme? {
        val str = this.headers.scheme()

        return if (str == HttpScheme.HTTPS.name()) HttpScheme.HTTPS else HttpScheme.HTTP
    }

    override fun getPath(): String? = this.headers.path()?.toString()

    override fun getMethod(): HttpMethod? = HttpMethod.valueOf(this.headers.method()?.toString())

    override fun getStatus(): HttpResponseStatus? = HttpResponseStatus.valueOf(this.headers.status()?.toString()?.toInt() ?: 0)

    override fun getAuthority(): String? = this.headers.authority()?.toString()

    override fun getContentLength(): Long = this.getLong("content-length", 0L)

    override fun toFrame(): Any = DefaultHttp3HeadersFrame(this.headers)

}