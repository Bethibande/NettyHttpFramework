package com.bethibande.web

import com.bethibande.web.crypto.CertificateHelper
import com.bethibande.web.execution.ExecutionType
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.impl.http2.Http2Client
import com.bethibande.web.impl.http2.Http2Server
import com.bethibande.web.impl.http3.Http3Client
import com.bethibande.web.impl.http3.Http3Server
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.*
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import java.net.InetSocketAddress
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.readText

fun serverHandle(ctx: HttpResponseContext) {
    println("server")
    ctx.response("Hello ${ctx.variables()[":name"]}")
}

fun clientHandle(ctx: HttpRequestContext) {
    println("client")
    ctx.onStatus(HttpResponseStatus.OK) { _ ->
        println("client rec")
        ctx.responseAsString()
    }
}

fun Long.formatted(): String = NumberFormat.getInstance().format(this)
fun Double.formatted(): String = NumberFormat.getInstance().format(this)

fun main() {
    val threads = 12
    val executor1 = ThreadPoolExecutor(executionType = ExecutionType.NIO, threadMaxCount = threads)
    val executor2 = ThreadPoolExecutor(executionType = ExecutionType.NIO, threadMaxCount = threads)

    val key = CertificateHelper.getPrivateKeyFromString(Path("./cert/key_pkcs8.pem").readText())
    val cert = CertificateHelper.getCertificateFromString(Path("./cert/cert.pem"))

    val serverSslContext = SslContextBuilder.forServer(key, cert)
        .sslProvider(SslProvider.JDK)
        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
        .applicationProtocolConfig(ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2
        )).build()
    val clientSslContext = SslContextBuilder.forClient()
        .sslProvider(SslProvider.JDK)
        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
        .trustManager(cert)
        .applicationProtocolConfig(ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2
        )).build()

    val address = InetSocketAddress("127.0.0.1", 4543)

    val server = Http2Server(executor1, threads, serverSslContext)
    server.bindInterface(address)
    server.addRoute("/test/:name", HttpMethod.GET, ::serverHandle)

    val client = Http2Client(address, clientSslContext, executor2, threads)
    client.prepareRequest(HttpMethod.GET, "/test/:name", ::clientHandle)
        .request()
        .variable("name", "Max")
        .execute()
        .addListener { println("Result: ${it.get()}") }
    /*val serverSslContext = QuicSslContextBuilder.forServer(key, "password", cert)
        .applicationProtocols(*Http3.supportedApplicationProtocols())
        .build()
    val clientSslContext = QuicSslContextBuilder.forClient()
        .keyManager(key, "password", cert)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .applicationProtocols(*Http3.supportedApplicationProtocols())
        .build()

    val address = InetSocketAddress("127.0.0.1", 2345)

    val server = Http3Server(executor1, threads, serverSslContext)
    server.bindInterface(address)
    server.addRoute("/test/:name", HttpMethod.GET, ::serverHandle)

    val client = Http3Client(address, clientSslContext, executor2, threads)

    val preparedRequest = client.prepareRequest(HttpMethod.GET, "/test/:name", ::clientHandle)

    val counter = AtomicInteger(0)
    val times = 100_000
    val start = System.nanoTime()
    for(i in 1..times) {
        preparedRequest.request()
            .variable("name", "Max")
            .execute()
            .addListener {
                val count = counter.incrementAndGet()
                if (count % 1000 == 0) println(count)

                if(count == times) {
                    val end = System.nanoTime()
                    val time = end - start
                    val avg = time / times
                    println("took ${time.formatted()} ns | avg ${avg.formatted()} ns | op/s ${(1_000_000_000.toDouble()/avg).formatted()}")
                }
            }
    }*/
}