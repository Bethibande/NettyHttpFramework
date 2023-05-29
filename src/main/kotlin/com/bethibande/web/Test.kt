package com.bethibande.web

import com.bethibande.web.context.HttpClientContext
import com.bethibande.web.context.HttpServerContext
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

/*fun main() {
    val tree = TreeMap<String, Int>()

    tree.put("a/b/c/d".split(), 1)
    tree.put("a/b/c/d".split(), 2)
    tree.put("a".split(), 2)
    tree.put("a".split(), 6)
    tree.put("a".split(), 5)
    tree.put("a/b/c".split(), 3)
    tree.put("a/b/d".split(), 5)
    tree.put("a/b/f".split(), 7)
    tree.put("a/b/g".split(), 6)
    tree.put("b/b".split(), 4)
    tree.put("b/e".split(), 3)
    tree.put("b/f".split(), 6)
    tree.put("b/f".split(), 78)
    tree.put("b/k".split(), 2)
    tree.put("b/i".split(), 9)

    println(tree.find("a".split()))

    val times = 1_000_000
    val key = "/a/b/c/d".split()
    var total: Long = 0
    for(i in 1..times) {
        val start = System.nanoTime()
        tree.find(key)
        val end = System.nanoTime()
        total += end - start
    }

    println("avg ${total/times} nano seconds")
}

fun String.split(): Array<String> = this.split(Regex("/")).toTypedArray()
*/
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

fun handler(ctx: HttpClientContext) {
    ctx.sendHeader(ctx.newRequestHeader("/world", HttpMethod.GET))

    ctx.onStatus(200) {
        println("length: ${ctx.getContentLength()}")

        ctx.readAsString {
            println("message: $it")
            ctx.finish()
        }
    }
}

val data = "Hello World!".toByteArray()

fun helloName(ctx: HttpServerContext) {
    ctx.readAllAsString {
        val data = "Hello $it!".toByteArray()
        ctx.sendHeader(ctx.newResponseHeader(HttpResponseStatus.OK, data.size.toLong()))
        ctx.write(data)
        ctx.finish()
    }
}

fun helloWorld(ctx: HttpServerContext) {
    val data = "Hello World!".toByteArray()
    ctx.sendHeader(ctx.newResponseHeader(HttpResponseStatus.OK, data.size.toLong()))
    ctx.write(data)
    ctx.finish()
}
