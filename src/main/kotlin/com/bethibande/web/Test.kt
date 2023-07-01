package com.bethibande.web

import com.bethibande.web.crypto.CertificateHelper
import com.bethibande.web.execution.ExecutionType
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.impl.http3.Http3Client
import com.bethibande.web.impl.http3.Http3Server
import com.bethibande.web.request.HttpRequestContext
import com.bethibande.web.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path
import kotlin.io.path.readText

fun serverHandle(ctx: HttpResponseContext) {
    val response = "Hello ${ctx.variables()[":name"]}".toByteArray()
    ctx.writeHeader(ctx.newHeader(HttpResponseStatus.OK, response.size.toLong()))
    ctx.write(response)
    ctx.close()
}

fun clientHandle(ctx: HttpRequestContext<String>) {
    ctx.onHeader { _ ->
        ctx.readAllString({ str ->
            ctx.setResult(str)
            ctx.close()
        })
    }
}

fun main() {
    val executor = ThreadPoolExecutor(executionType = ExecutionType.NIO, threadMaxCount = 12)

    val key = CertificateHelper.getPrivateKeyFromString(Path("./cert/key_pkcs8.pem").readText())
    val cert = CertificateHelper.getCertificateFromString(Path("./cert/cert.pem"))
    val serverSslContext = QuicSslContextBuilder.forServer(key, "password", cert)
        .applicationProtocols(*Http3.supportedApplicationProtocols())
        .build()
    val clientSslContext = QuicSslContextBuilder.forClient()
        .keyManager(key, "password", cert)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .applicationProtocols(*Http3.supportedApplicationProtocols())
        .build()

    val address = InetSocketAddress("127.0.0.1", 2345)

    val server = Http3Server(executor, serverSslContext)
    server.bindInterface(address)
    server.addRoute("/test/:name", HttpMethod.GET, ::serverHandle)

    val client = Http3Client(address, clientSslContext, executor)

    val preparedRequest = client.prepareRequest(HttpMethod.GET, "/test/:name", ::clientHandle)
    preparedRequest.request()
        .variable("name", "Max")
        .execute()
        .addListener { println("Response: ${it.get() as String}") }

    // connection timeout is 5000, the old implementation would have thrown an exception when sending the request after this 5 sek wait
    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5005))

    preparedRequest.request()
        .variable("name", "Joshua")
        .execute()
        .addListener { println("Response: ${it.get() as String}") }
}