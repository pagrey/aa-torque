package com.aatorque.utils

class RepeatCounter<in T>(init: T) {
    private var last = init
    var count = 0
        private set

    fun append(new: T) {
        if (new == last) {
            count++
        } else {
            last = new
            count = 0
        }
    }

    fun flushIfOver(check: Int): Boolean {
        return if (count > check) {
            count = 0
            true
        } else false
    }
}