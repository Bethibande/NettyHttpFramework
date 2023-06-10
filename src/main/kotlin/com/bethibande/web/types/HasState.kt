package com.bethibande.web.types

abstract class HasState {

    companion object {

        const val STATE_INITIAL = 0x00
        const val STATE_CLOSED = 0x01

    }

    @Volatile
    private var state = STATE_INITIAL

    protected fun has(state: Int): Boolean = this.state and state == state

    protected fun set(state: Int) {
        this.state = this.state xor state
    }

    protected fun resetState() {
        set(state)
    }

}