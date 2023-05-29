package com.bethibande.web.types.tree

class TreeMap<K, V: Any> {

    private val root = hashMapOf<K, TreeEntry<K, V>>()

    fun put(key: Array<K>, value: V) {
        if(key.isEmpty()) throw IllegalArgumentException("The given key must have at least one element")
        if(!this.root.containsKey(key[0])) this.root[key[0]] = TreeEntry(key[0], arrayOf(), arrayOf())

        this.root[key[0]]!!.put(key, 0, value)
    }

    fun find(key: Array<K>): List<V> {
        if(key.isEmpty()) throw IllegalArgumentException("The given key must have at least one element")
        val results = ArrayList<V>()

        this.root[key[0]]?.find(key, 0, results)

        return results
    }

}