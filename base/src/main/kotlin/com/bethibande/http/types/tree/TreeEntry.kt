package com.bethibande.http.types.tree

import java.util.*

class TreeEntry<K, V : Any>(
    val key: K,
    var values: Array<Any>,
    var children: Array<TreeEntry<K, V>>
) {

    fun addChild(entry: TreeEntry<K, V>) {
        this.children = arrayOf(*this.children, entry)
    }

    fun addValue(value: V) {
        this.values = arrayOf(*this.values, value)
    }

    fun put(key: Array<K>, index: Int, value: V) {
        if(key[index] != this.key) return
        if(index == key.lastIndex) {
            this.addValue(value)
            return
        }


        val nextIndex = index + 1
        val nextKey = key[nextIndex]
        val nextEntry = this.children.find { it.key == nextKey }

        if(nextEntry != null) {
            nextEntry.put(key, nextIndex, value)
            return
        }

        val entry = TreeEntry<K, V>(nextKey, arrayOf(), arrayOf())

        addChild(entry)
        entry.put(key, nextIndex, value)
    }

    @Suppress("UNCHECKED_CAST")
    fun find(key: Array<*>, index: Int, results: MutableList<V>) {
        if(Objects.equals(key[index], this.key)) return
        results.addAll(this.values as Array<out V>)

        if(index != key.lastIndex) {
            val newIndex = index + 1
            this.children.forEach { it.find(key, newIndex, results) }
        }
    }

}