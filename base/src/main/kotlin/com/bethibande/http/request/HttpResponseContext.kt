package com.bethibande.http.request

import com.bethibande.http.HttpConnection
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import java.nio.ByteBuffer

abstract class HttpResponseContext(
    connection: HttpConnection,
    channel: Channel
): HttpContextBase(connection, channel) {

    private fun writeResponse(status: HttpResponseStatus, data: ByteArray?): ChannelFuture {
        if (data != null) {
            val buf = this.channel.alloc().buffer(data.size)
            buf.writeBytes(data)

            return this.writeResponse(status, DefaultHttp2DataFrame(buf), data.size.toLong())
        }
        return this.writeResponse(status, null, null)
    }

    private fun writeResponse(status: HttpResponseStatus, data: ByteBuffer?): ChannelFuture {
        if (data != null) {
            data.flip()
            val buf = this.channel.alloc().buffer(data.limit())
            buf.writeBytes(data)

            return this.writeResponse(status, DefaultHttp2DataFrame(buf), data.limit().toLong())
        }
        return this.writeResponse(status, null, null)
    }

    private fun writeResponse(status: HttpResponseStatus, data: ByteBuf?): ChannelFuture {
        if (data != null) {
            return this.writeResponse(status, DefaultHttp2DataFrame(data), data.writerIndex().toLong())
        }
        return this.writeResponse(status, null, null)
    }

    private fun writeResponse(status: HttpResponseStatus, data: Any?, size: Long?): ChannelFuture {
        val header = this.newHeader()
        header.setStatus(status)
        size?.let { header.setContentLength(size) }

        this.writeHeader(header)
        data?.let {
            this.write(data)
        }

        this.flush()
        this.close()
        return this.lastWrite!!
    }

    fun response(buf: ByteBuf): ChannelFuture = this.writeResponse(HttpResponseStatus.OK, buf)

    fun response(buffer: ByteBuffer): ChannelFuture = this.writeResponse(HttpResponseStatus.OK, buffer)

    fun response(string: String): ChannelFuture {
        val bytes = string.toByteArray()
        return this.writeResponse(HttpResponseStatus.OK, bytes)
    }

    fun response(status: HttpResponseStatus): ChannelFuture = this.writeResponse(status, null, null)

    fun getRequestHeader() = super.header

    fun newHeader(status: HttpResponseStatus, contentLength: Long): AbstractHttpHeader {
        val header = super.newHeader()
        header.setStatus(status)
        header.setContentLength(contentLength)

        return header
    }

}