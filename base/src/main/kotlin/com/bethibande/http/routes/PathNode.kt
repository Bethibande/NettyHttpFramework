package com.bethibande.http.routes

open class PathNode(
    val value: String,
) {

    override operator fun equals(other: Any?): Boolean {
        if(other == null || other !is PathNode) return false

        return this.value.equals(other.value)
    }

}

class VarPathNode(
    value: String,
    val regex: String,
): PathNode(value) {

    private val _regex: Regex = Regex(this.regex)

    override fun equals(other: Any?): Boolean {
        return other is PathNode && other.value.matches(this._regex)
    }
}