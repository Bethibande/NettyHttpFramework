package com.bethibande.web.routes

open class PathNode(
    val value: String,
) {

    override fun equals(other: Any?): Boolean {
        return this.value.equals(other)
    }

}

class VarPathNode(
    value: String,
    val regex: String,
): PathNode(value) {

    private val _regex: Regex = Regex(this.regex)

    override fun equals(other: Any?): Boolean {
        return other is CharSequence && this._regex.matches(other)
    }
}