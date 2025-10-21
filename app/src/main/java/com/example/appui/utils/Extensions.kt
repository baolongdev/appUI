package com.example.appui.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Show a toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.toast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, duration).show()
}

fun String?.orEmpty(): String = this ?: ""

fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()