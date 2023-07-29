package com.bethibande.http

import com.bethibande.http.connections.ConnectionManager
import com.bethibande.http.crypto.CertificateHelper
import com.bethibande.http.data.Reader
import com.bethibande.http.data.Writer
import com.bethibande.http.impl.http2.Http2Client
import com.bethibande.http.impl.http2.Http2Server
import com.bethibande.http.request.HttpRequestContext
import com.bethibande.http.request.HttpResponseContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.SupportedCipherSuiteFilter
import io.netty.util.ResourceLeakDetector
import java.net.InetSocketAddress
import java.text.NumberFormat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path
import kotlin.io.path.readText

fun serverHandle(ctx: HttpResponseContext) {
    val writer = Writer.forString("Hello ${ctx.variables()["name"]}")

    writer.writeHeader(ctx, HttpResponseStatus.OK)
    writer.write(ctx)
}

fun clientHandle(ctx: HttpRequestContext) {
    ctx.onResponse { h ->
        Reader.forString().read(ctx, h) {
            ctx.setResult(it)
            ctx.close()
        }
    }
}

fun Long.formatted(): String = NumberFormat.getInstance().format(this)
fun Double.formatted(): String = NumberFormat.getInstance().format(this)

fun main() {
    val threads = 12

    val executor1 = Executors.newFixedThreadPool(threads)
    val executor2 = Executors.newFixedThreadPool(threads)

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

    server.routes {
        get("/test/:name", ::serverHandle)
    }

    val client = Http2Client(address, clientSslContext, executor2, threads)

    val manager = ConnectionManager(client)
    manager.setMinConnections(threads)
    manager.setMaxStreams(200)

    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED)

    val request = manager.prepare(HttpMethod.GET, "/test/:name", ::clientHandle)
        .withPathVariable("name" to "Max")
        .build()

    val counter = AtomicInteger(0)
    val times = 5_000_000
    val warmup = 500_000

    val warmupCounter = AtomicInteger(warmup)

    for(i in 1..warmup) {
        request.execute(manager.getNextConnection()).addListener {
                warmupCounter.getAndDecrement()
        }
    }

    while (warmupCounter.get() > 0) {
        LockSupport.parkNanos(1_000_000)
    }

    val start = System.nanoTime()
    for(i in 1..times) {
        request.execute(manager.getNextConnection())
            .addListener {
                val count = counter.incrementAndGet()
                if (count % 10000 == 0) println(count)

                if(count == times) {
                    val end = System.nanoTime()
                    val time = end - start
                    val avg = time / times
                    println("took ${time.formatted()} ns | avg ${avg.formatted()} ns | op/s ${(1_000_000_000.toDouble()/avg).formatted()}")
                }
            }
    }
}