package com.bethibande.http

import com.bethibande.http.config.HttpClientConfig
import com.bethibande.http.request.RequestHandler
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.AttributeMap
import io.netty.util.DefaultAttributeMap
import io.netty.util.concurrent.Promise
import java.util.function.Consumer

abstract class HttpClient: RequestHandler(), AttributeMap {

    private val attributeMap = DefaultAttributeMap()

    abstract fun newConnection(): Promise<out HttpConnection>
    abstract fun configure(consumer: Consumer<HttpClientConfig>)
    abstract fun getConnections(): List<HttpConnection>

    override fun <T : Any?> attr(p0: AttributeKey<T>?): Attribute<T> = this.attributeMap.attr(p0)

    override fun <T : Any?> hasAttr(p0: AttributeKey<T>?): Boolean = this.attributeMap.hasAttr(p0)

}