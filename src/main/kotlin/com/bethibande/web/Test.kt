package com.bethibande.web

import com.bethibande.web.context.HttpClientContext
import com.bethibande.web.context.HttpServerContext
import com.bethibande.web.crypto.KeyHelper
import com.bethibande.web.execution.ExecutionType
import com.bethibande.web.execution.ThreadPoolExecutor
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.function.Consumer

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
        .routes {
            get("", ::helloWorld)
            post("", ::helloName)
        }

    val address = InetSocketAddress("127.0.0.1", 4455)
    server.addInterface(address)

    val client = Http3Client(address)

    client.stream(::handler)
}

fun handler(ctx: HttpClientContext) {
    ctx.sendHeader(ctx.newRequestHeader("/", HttpMethod.GET))

    ctx.onStatus(200) {
        println("length: ${ctx.getContentLength()}")

        ctx.readString {
            println("message: $it")
            ctx.finish()
        }
    }
}

val data = "Hello World!".toByteArray()

fun helloName(ctx: HttpServerContext) {
    ctx.readAsString {
        ctx.respond(200, Unpooled.wrappedBuffer("Hello $it!".toByteArray()))
    }
}

fun helloWorld(ctx: HttpServerContext) {
    ctx.respond(200, Unpooled.wrappedBuffer(data))
}