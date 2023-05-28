package com.bethibande.web.context

import com.bethibande.web.types.DataProvider
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame
import io.netty.incubator.codec.http3.DefaultHttp3Headers
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3Headers
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.AsciiString
import java.util.function.Consumer
import kotlin.IllegalStateException

class HttpServerContext(
    val path: String,
    val method: HttpMethod,
    var variables: Map<String, String>? = null,
    private var headers: Http3Headers
) {

    companion object {
        const val BUFFER_SIZE = 1024
    }

    private var finished = false
    private var context: ChannelHandlerContext? = null

    private var reader: Consumer<ByteBuf>? = null

    fun variable(name: String): String? {
        return variables?.get(name)
    }

    fun variableOr(name: String, or: String): String {
        return variables?.get(name) ?: or
    }

    internal fun accept(dataFrame: Http3DataFrame, isLast: Boolean) {
        if(this.reader != null) this.reader!!.accept(dataFrame.content())
    }

    internal fun withContext(ctx: ChannelHandlerContext): HttpServerContext {
        this.context = ctx
        return this
    }

    fun read(callback: Consumer<ByteBuf>) {
        this.reader = callback
    }

    fun readAll(func: Consumer<ByteBuf>) {
        this.reader = object: Consumer<ByteBuf> {

            private val buffer = Unpooled.buffer(headers.getInt("content-length", 0))

            override fun accept(t: ByteBuf) {
                buffer.writeBytes(t)

                if(buffer.writerIndex() == buffer.capacity()) {
                    func.accept(this.buffer)
                }
            }
        }
    }

    fun readAsString(func: Consumer<String>) {
        readAll {
            it.resetReaderIndex()

            val array = ByteArray(it.capacity())
            it.readBytes(array)

            func.accept(String(array))
        }
    }

    fun respond(status: Int) {
        val headers = DefaultHttp3Headers()
        headers.status(status.toString())
        headers.setLong("content-length", 0)
        writeAndFlush(DefaultHttp3HeadersFrame(headers)).sync()
    }

    fun respond(status: Int, data: ByteBuf) {
        val headers = DefaultHttp3Headers()
        headers.status(status.toString())
        headers.setInt("content-length", data.writerIndex())
        writeAndFlush(DefaultHttp3HeadersFrame(headers)).sync()

        write(DefaultHttp3DataFrame(data)).addListener {
            this.writeFin()
        }
    }

    fun respond(status: Int, contentLength: Long, data: DataProvider) {
        val headers = DefaultHttp3Headers()
        headers.status(status.toString())
        headers.setLong("content-length", contentLength)
        writeAndFlush(DefaultHttp3HeadersFrame(headers)).sync()

        val buffer = Unpooled.buffer(BUFFER_SIZE)
        var offset: Long = 0

        while(offset < contentLength) {
            data.next(offset, buffer)
            offset += buffer.writerIndex()

            write(DefaultHttp3DataFrame(buffer)).sync()
            buffer.resetWriterIndex()
            buffer.resetReaderIndex()
        }

        buffer.release()
        writeFin()
    }

    private fun writeFin() {
        if(isFinished()) throw IllegalStateException("Cannot write data, request has already been completed")
        if(this.context == null) throw IllegalStateException("Context not found")

        (this.context!!.channel() as QuicStreamChannel).shutdownOutput().sync()
    }

    private fun writeAndFlush(obj: Any): ChannelFuture {
        if(isFinished()) throw IllegalStateException("Cannot write data, request has already been completed")
        if(this.context == null) throw IllegalStateException("Context not found")

        return this.context!!.writeAndFlush(obj)
    }

    private fun write(obj: Any): ChannelFuture {
        if(isFinished()) throw IllegalStateException("Cannot write data, request has already been completed")
        if(this.context == null) throw IllegalStateException("Context not found")

        return this.context!!.write(obj)
    }

    fun isFinished(): Boolean = this.finished

}