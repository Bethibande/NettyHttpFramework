package com.bethibande.http.data

import com.bethibande.http.request.AbstractHttpHeader
import com.bethibande.http.request.HttpContextBase
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

interface Reader<T> {

    companion object {

        fun forString(charset: Charset = StandardCharsets.UTF_8): Reader<String> = StringReader(charset)

    }

    fun read(ctx: HttpContextBase, contentLength: Long, contentType: String?, consumer: Consumer<T>)

    fun read(ctx: HttpContextBase, header: AbstractHttpHeader, consumer: Consumer<T>) {
        this.read(ctx, header.getContentLength(), header.get(Writer.HEADER_CONTENT_TYPE), consumer)
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

    // TODO: ProgressivePromise
    override fun read(ctx: HttpContextBase, contentLength: Long, contentType: String?, consumer: Consumer<String>) {
        if (contentLength > Int.MAX_VALUE) throw IllegalArgumentException("Content-Length for StringReader must not exceed ${Int.MAX_VALUE}!")

        val length = contentLength.toInt()
        val buffer = ctx.alloc().buffer(length)

        ctx.onData {
            buffer.writeBytes(it)

            if (buffer.writerIndex() == length) {
                consumer.accept(this.readString(buffer, length, charset))
                buffer.release()
            }
        }
    }
}