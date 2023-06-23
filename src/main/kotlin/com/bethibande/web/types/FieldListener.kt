package com.bethibande.web.types

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import kotlin.reflect.KProperty

class FieldListener<T> {

    private val listeners = ConcurrentLinkedQueue<Consumer<T>>()

    @Volatile
    private var value: T? = null;

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(this.value == null) throw IllegalStateException("The value ${property.name} has not yet been initialized")
        return this.value!!
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        this.listeners.iterator().forEach { it.accept(value) }
    }
    
    fun addListener(consumer: Consumer<T>) {
        if(this.value != null) {
            consumer.accept(this.value!!)
        }
        
        this.listeners.add(consumer)
    }

}