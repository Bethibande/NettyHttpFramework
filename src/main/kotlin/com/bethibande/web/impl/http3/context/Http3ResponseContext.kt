package com.bethibande.web.impl.http3.context

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.request.AbstractHttpHeader
import com.bethibande.web.request.HttpResponseContext
import com.bethibande.web.types.FieldListener
import com.bethibande.web.types.HasState
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.quic.QuicStreamChannel
import java.util.function.Consumer

class Http3ResponseContext(
    connection: Http3Connection,
    channel: QuicStreamChannel,
): HttpResponseContext(connection, channel) {

    companion object {

        const val STATE_HEADER_RECEIVED = 0x02
        const val STATE_HEADER_SENT = 0x04

    }

    private val headerListener = FieldListener<AbstractHttpHeader>()
    private val headers: AbstractHttpHeader by headerListener

    internal fun headerCallback(headersFrame: Http3HeadersFrame) {
        if(has(STATE_HEADER_RECEIVED)) throw IllegalStateException("Stream already received a header")
        set(STATE_HEADER_RECEIVED)

        TODO("IMPLEMENT")
    }

    internal fun dataCallback(dataFrame: Http3DataFrame) {
        TODO("IMPLEMENT")
    }

    fun onHeaders(consumer: Consumer<AbstractHttpHeader>) {
        headerListener.addListener(consumer)
    }

}