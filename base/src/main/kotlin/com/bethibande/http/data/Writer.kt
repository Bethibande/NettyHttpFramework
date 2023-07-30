package com.bethibande.http.data

import com.bethibande.http.request.HttpContextBase
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelProgressivePromise
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

abstract class Writer(
    private val contentType: String,
) {

    companion object {

        const val HEADER_CONTENT_TYPE = "content-type"

        fun forString(string: String): Writer = StringWriter(string)
        fun forString(string: String, charset: Charset): Writer = StringWriter(string, charset)
        fun forStream(stream: InputStream, length: Long): Writer = StreamWriter(stream, length)
        fun forStream(stream: InputStream, length: Long, contentType: String): Writer = StreamWriter(stream, length, contentType)

    }

    fun contentType(): String = this.contentType
    abstract fun contentLength(): Long

    abstract fun write(ctx: HttpContextBase, promise: ChannelProgressivePromise)

    fun writeHeaderAndData(ctx: HttpContextBase, status: HttpResponseStatus) {
        this.writeHeader(ctx, status)
        this.write(ctx)
    }

    fun writeHeaderAndData(ctx: HttpContextBase, path: String) {
        this.writeHeader(ctx, path)
        this.write(ctx)
    }

    fun write(ctx: HttpContextBase) {
        val promise = ctx.channel().newProgressivePromise()
        this.write(ctx, promise)
    }

    fun writeHeader(ctx: HttpContextBase, status: HttpResponseStatus) {
        val header = ctx.newHeader()
            .setStatus(status)
            .setContentLength(this.contentLength())
            .set(HEADER_CONTENT_TYPE, this.contentType())

        ctx.writeHeader(header)
    }

    fun writeHeader(ctx: HttpContextBase, path: String) {
        val header = ctx.newHeader()
            .setPath(path)
            .setContentLength(this.contentLength())
            .set(HEADER_CONTENT_TYPE, this.contentType())

        ctx.writeHeader(header)
    }

}

class StringWriter(
    private val string: String,
    private val charset: Charset = StandardCharsets.UTF_8
): Writer("text/plain") {

    private val data = this.string.toByteArray(this.charset)

    override fun contentLength(): Long = this.data.size.toLong()

    override fun write(ctx: HttpContextBase, promise: ChannelProgressivePromise) {
        val buf = Unpooled.wrappedBuffer(this.data)
        buf.writerIndex(this.data.size)

        ctx.write(buf)
        promise.setSuccess()
    }
}

class StreamWriter(
    private val stream: InputStream,
    private val contentLength: Long,
    contentType: String = "binary",
    private val bufferSize: Int = 8192,
): Writer(contentType) {

    override fun contentLength(): Long = this.contentLength

    override fun write(ctx: HttpContextBase, promise: ChannelProgressivePromise) {
        val buffer = ByteArray(this.bufferSize)
        val buf = Unpooled.wrappedBuffer(buffer)
        var total = this.contentLength

        while (total > 0) {
            val read = this.stream.read(buffer)
            buf.writerIndex(read)
            buf.resetReaderIndex()

            total -= read

            ctx.write(buf)

            promise.setProgress(total, this.contentLength)
        }

        promise.setSuccess()
    }
}