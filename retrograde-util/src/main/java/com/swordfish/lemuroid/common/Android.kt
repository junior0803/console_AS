package com.swordfish.lemuroid.common

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast

fun Bundle?.dump(): String {
    if (this == null) return "null"

    val builder = StringBuilder("Extras:\n")
    keySet()
        .forEach { key ->
            builder.append(key).append(": ").append(get(key)).append("\n")
        }
    return builder.toString()
}

fun Activity.displayToast(string: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, string, length).show()
}

fun Activity.displayToast(stringId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, stringId, length).show()
}

fun Context.animationDuration(): Int {
    return resources.getInteger(android.R.integer.config_mediumAnimTime)
}

fun Context.shortAnimationDuration(): Int {
    return resources.getInteger(android.R.integer.config_shortAnimTime)
}

fun Context.longAnimationDuration(): Int {
    return resources.getInteger(android.R.integer.config_longAnimTime)
}
