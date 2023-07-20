package com.bethibande.http.data

import com.bethibande.http.request.AbstractHttpHeader
import com.bethibande.http.request.HttpContextBase
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelProgressivePromise
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

interface Reader<T> {

    companion object {

        fun forString(charset: Charset = StandardCharsets.UTF_8): Reader<String> = StringReader(charset)

        fun forStream(stream: OutputStream): Reader<Unit> = StreamReader(stream)

    }

    fun read(
        ctx: HttpContextBase,
        contentLength: Long,
        contentType: String?,
        consumer: Consumer<T>,
        promise: ChannelProgressivePromise
    )

    fun read(ctx: HttpContextBase, header: AbstractHttpHeader, consumer: Consumer<T>) {
        val promise = ctx.channel().newProgressivePromise()
        this.read(ctx, header.getContentLength(), header.get(Writer.HEADER_CONTENT_TYPE), consumer, promise)
    }

}

class StringReader(
    private val charset: Charset
): Reader<String> {

    /**
     * Faster than [ByteBuf.readCharSequence].toString()
     */
    private fun readString(buf: ByteBuf, length: Int, charset: Charset): String {
        if (buf.isDirect) {
            val array = ByteArray(length)
            buf.readBytes(array, 0, length)

            return String(array, charset)
        }

        return String(buf.array(), 0, length, charset)
    }

    override fun read(
        ctx: HttpContextBase,
        contentLength: Long,
        contentType: String?,
        consumer: Consumer<String>,
        promise: ChannelProgressivePromise
    ) {
        if (contentLength > Int.MAX_VALUE) throw IllegalArgumentException("Content-Length for StringReader must not exceed ${Int.MAX_VALUE}!")

        val length = contentLength.toInt()
        val buffer = ctx.alloc().buffer(length)

        ctx.onData {
            buffer.writeBytes(it)
            promise.setProgress(buffer.writerIndex().toLong(), length.toLong())

            if (buffer.writerIndex() == length) {
                consumer.accept(this.readString(buffer, length, charset))
                buffer.release()

                promise.setSuccess()
            }
        }
    }
}

class StreamReader(
    private val stream: OutputStream,
): Reader<Unit> {

    override fun read(
        ctx: HttpContextBase,
        contentLength: Long,
        contentType: String?,
        consumer: Consumer<Unit>,
        promise: ChannelProgressivePromise
    ) {
        val read = AtomicLong(0)

        ctx.onData {
            it.readBytes(this.stream, it.writerIndex())

            val readBytes = read.addAndGet(it.writerIndex().toLong())
            promise.setProgress(readBytes, contentLength)

            if (readBytes == contentLength) {
                consumer.accept(Unit)
                promise.setSuccess()
            }
        }
    }
}