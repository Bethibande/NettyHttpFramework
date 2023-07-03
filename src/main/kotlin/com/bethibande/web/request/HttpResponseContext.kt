package com.bethibande.web.request

import com.bethibande.web.HttpConnection
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.HttpResponseStatus
import java.nio.ByteBuffer

abstract class HttpResponseContext(
    connection: HttpConnection,
    channel: Channel
): HttpContextBase(connection, channel) {

    private fun writeResponse(status: HttpResponseStatus, data: Any?): ChannelFuture {
        val header = this.newHeader()
        header.setStatus(status)
        if(data is ByteArray) header.setContentLength(data.size.toLong())
        if(data is ByteBuffer) header.setContentLength(data.capacity().toLong())
        if(data is ByteBuf) header.setContentLength(data.capacity().toLong())

        this.writeHeader(header)
        data?.let {
            if(it is ByteArray) this.write(it)
            if(it is ByteBuffer) this.write(it)
            if(it is ByteBuf) this.write(it)
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

    fun response(status: HttpResponseStatus): ChannelFuture = this.writeResponse(status, null)

    fun getRequestHeader() = super.header

    fun newHeader(status: HttpResponseStatus, contentLength: Long): AbstractHttpHeader {
        val header = super.newHeader()
        header.setStatus(status)
        header.setContentLength(contentLength)

        return header
    }

}