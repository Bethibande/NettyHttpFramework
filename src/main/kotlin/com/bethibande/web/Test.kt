package com.bethibande.web

import com.bethibande.web.context.HttpRequestContext
import com.bethibande.web.context.HttpResponseContext
import com.bethibande.web.crypto.KeyHelper
import com.bethibande.web.execution.ExecutionType
import com.bethibande.web.execution.ThreadPoolExecutor
import com.bethibande.web.routes.Route
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun main() {
    val executor = ThreadPoolExecutor(
        queueSize = 100,
        executionType = ExecutionType.NIO,
        threadMinCount = 2,
        threadMaxCount = Runtime.getRuntime().availableProcessors()
    )

    val cert = FileInputStream("cert/cert.pem").use { inStream ->
        val cf = CertificateFactory.getInstance("X.509")
        return@use cf.generateCertificate(inStream) as X509Certificate
    }

    val key = FileInputStream("cert/key_pkcs8.pem").use { inStream ->
        val txt = String(inStream.readAllBytes())
        return@use KeyHelper.getPrivateKeyFromString(txt)
    }

    val server = Http3Server(executor, key, cert)
    server.getRoutes().register(Route("/world", HttpMethod.GET, handler = ::helloWorld))
    server.getRoutes().register(Route("/name", HttpMethod.POST, handler = ::helloName))

    val address = InetSocketAddress("127.0.0.1", 4455)
    server.addInterface(address)

    val client = Http3Client(address)

    client.stream(::handler)
}

fun handler(ctx: HttpRequestContext) {
    ctx.sendHeader(ctx.newRequestHeader("/name", HttpMethod.POST, 3))
    ctx.write("Max").addListener { ctx.flush() }

    ctx.onStatus(200) {
        println("length: ${ctx.getContentLength()}")

        ctx.readAsString {
            println("message: $it")
            ctx.finish()
        }
    }
}

val data = "Hello World!".toByteArray()

fun helloName(ctx: HttpResponseContext) {
    ctx.readAllAsString {
        val data = "Hello $it!".toByteArray()
        ctx.sendHeader(ctx.newResponseHeader(HttpResponseStatus.OK, data.size.toLong()))
        ctx.write(data)
        ctx.flush()
        ctx.finish()

        ctx.connection().stream { request ->
            request.sendHeader(request.newRequestHeader("/hello", HttpMethod.GET, 17))
            request.write("Hello from Server").addListener { request.flush() }
            request.finish()
        }
    }
}

fun helloWorld(ctx: HttpResponseContext) {
    val data = "Hello World!".toByteArray()
    ctx.sendHeader(ctx.newResponseHeader(HttpResponseStatus.OK, data.size.toLong()))
    ctx.write(data)
    ctx.flush()
    ctx.finish()
}
