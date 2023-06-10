package com.bethibande.web.impl.http3

import com.bethibande.web.HttpClient
import com.bethibande.web.config.HttpClientConfig
import com.bethibande.web.request.HttpRequestContext
import io.netty.incubator.codec.quic.QuicSslContext
import java.net.InetSocketAddress
import java.util.function.Consumer

class Http3Client(
    private val address: InetSocketAddress,
    private val sslContext: QuicSslContext
): HttpClient<HttpClientConfig> {

    private val config = HttpClientConfig()

    override fun configure(consumer: Consumer<HttpClientConfig>) {
        consumer.accept(this.config)
    }

    override fun getAddress(): InetSocketAddress = this.address

    override fun canRequest(): Boolean {

    }

    override fun newRequest(request: Consumer<HttpRequestContext>) {
        TODO("Not yet implemented")
    }
}