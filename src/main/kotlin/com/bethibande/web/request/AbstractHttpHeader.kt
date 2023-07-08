package com.bethibande.web.request

import io.netty.handler.codec.Headers
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpScheme
import io.netty.handler.codec.http2.Http2Headers
import io.netty.incubator.codec.http3.Http3Headers

// TODO: add, get, set, getAll methods
class AbstractHttpHeader(
    private val headers: Headers<CharSequence, *, *>,
    private val toFrame: (Headers<CharSequence, *, *>) -> Any,
) {

    companion object {
        const val HEADER_CONTENT_LENGTH = "content-length"
    }

    private val pseudoHeaderStrategy: PseudoHeaderStrategy

    init {
        when (this.headers) {
            is Http2Headers -> this.pseudoHeaderStrategy = Http2PseudoStrat(this.headers)
            is Http3Headers -> this.pseudoHeaderStrategy = Http3PseudoStrat(this.headers)
            else -> throw RuntimeException("Invalid header type '${this.headers.javaClass.name}'")
        }
    }

    fun setScheme(scheme: HttpScheme): AbstractHttpHeader {
        this.pseudoHeaderStrategy.scheme(scheme)
        return this
    }

    fun setPath(path: String): AbstractHttpHeader {
        this.pseudoHeaderStrategy.path(path)
        return this
    }

    fun setMethod(method: HttpMethod): AbstractHttpHeader {
        this.pseudoHeaderStrategy.method(method)
        return this
    }
    fun setStatus(status: HttpResponseStatus): AbstractHttpHeader {
        this.pseudoHeaderStrategy.status(status)
        return this
    }
    fun setAuthority(authority: String): AbstractHttpHeader {
        this.pseudoHeaderStrategy.authority(authority)
        return this
    }

    fun setContentLength(length: Long): AbstractHttpHeader {
        this.headers.setLong(HEADER_CONTENT_LENGTH, length)
        return this
    }

    fun getScheme(): HttpScheme = this.pseudoHeaderStrategy.scheme()
    fun getPath(): String = this.pseudoHeaderStrategy.path()
    fun getMethod(): HttpMethod = this.pseudoHeaderStrategy.method()
    fun getStatus(): HttpResponseStatus = this.pseudoHeaderStrategy.status()
    fun getAuthority(): String = this.pseudoHeaderStrategy.authority()
    fun getContentLength(): Long = this.headers.getLong(HEADER_CONTENT_LENGTH) ?: 0L

    fun asNettyHeader(): Headers<*, *, *> = this.headers

    fun toFrame(): Any = this.toFrame.invoke(this.headers)

    private interface PseudoHeaderStrategy {

        fun scheme(scheme: HttpScheme)
        fun method(method: HttpMethod)
        fun status(status: HttpResponseStatus)
        fun path(path: String)
        fun authority(authority: String)

        fun scheme(): HttpScheme
        fun method(): HttpMethod
        fun status(): HttpResponseStatus
        fun path(): String
        fun authority(): String

    }

    private class Http2PseudoStrat(
        private val headers: Http2Headers,
    ): PseudoHeaderStrategy {

        override fun scheme(scheme: HttpScheme) {
            this.headers.scheme(scheme.name())
        }

        override fun method(method: HttpMethod) {
            this.headers.method(method.asciiName())
        }

        override fun status(status: HttpResponseStatus) {
            this.headers.status(status.code().toString())
        }

        override fun path(path: String) {
            this.headers.path(path)
        }

        override fun authority(authority: String) {
            this.headers.authority(authority)
        }

        override fun scheme(): HttpScheme = when (this.headers.scheme()) {
            HttpScheme.HTTPS.name() -> HttpScheme.HTTPS
            HttpScheme.HTTP.name() -> HttpScheme.HTTP
            else -> throw IllegalArgumentException("Unknown scheme '${this.headers.scheme()}'")
        }

        override fun method(): HttpMethod = HttpMethod.valueOf(this.headers.method().toString())

        override fun status(): HttpResponseStatus = HttpResponseStatus.valueOf(this.headers.status().toString().toInt())

        override fun path(): String = this.headers.path().toString()

        override fun authority(): String = this.headers.path().toString()
    }

    private class Http3PseudoStrat(
        private val headers: Http3Headers,
    ): PseudoHeaderStrategy {

        override fun scheme(scheme: HttpScheme) {
            this.headers.scheme(scheme.name())
        }

        override fun method(method: HttpMethod) {
            this.headers.method(method.asciiName())
        }

        override fun status(status: HttpResponseStatus) {
            this.headers.status(status.code().toString())
        }

        override fun path(path: String) {
            this.headers.path(path)
        }

        override fun authority(authority: String) {
            this.headers.authority(authority)
        }

        override fun scheme(): HttpScheme = when (this.headers.scheme()) {
            HttpScheme.HTTPS.name() -> HttpScheme.HTTPS
            HttpScheme.HTTP.name() -> HttpScheme.HTTP
            else -> throw IllegalArgumentException("Unknown scheme '${this.headers.scheme()}'")
        }

        override fun method(): HttpMethod = HttpMethod.valueOf(this.headers.method().toString())

        override fun status(): HttpResponseStatus = HttpResponseStatus.valueOf(this.headers.status().toString().toInt())

        override fun path(): String = this.headers.path().toString()

        override fun authority(): String = this.headers.path().toString()
    }

}