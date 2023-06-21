package com.bethibande.web.types.tree

class TreeMap<K, V: Any>(
    private val rootKey: K
) {

    private val root = TreeEntry<K, V>(rootKey, arrayOf(), arrayOf())

    fun put(key: Array<K>, value: V) {
        if(key.isEmpty()) throw IllegalArgumentException("The given key must have at least one element")

        this.root.put(key, 0, value)
    }

    fun find(key: Array<*>): List<V> {
        if(key.isEmpty()) throw IllegalArgumentException("The given key must have at least one element")
        val results = ArrayList<V>()

        this.root.find(key, 0, results)

        return results
    }

}