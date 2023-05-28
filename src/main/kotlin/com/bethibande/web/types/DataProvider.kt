package com.bethibande.web.types

import io.netty.buffer.ByteBuf

fun interface DataProvider {

    fun next(offset: Long, buffer: ByteBuf)

}