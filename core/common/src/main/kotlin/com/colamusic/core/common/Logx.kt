package com.colamusic.core.common

import android.util.Log

/** Tiny logging facade so modules can log without coupling to the diagnostics bus yet. */
object Logx {
    fun d(tag: String, msg: String) { Log.d("cola/$tag", msg) }
    fun i(tag: String, msg: String) { Log.i("cola/$tag", msg) }
    fun w(tag: String, msg: String, t: Throwable? = null) { Log.w("cola/$tag", msg, t) }
    fun e(tag: String, msg: String, t: Throwable? = null) { Log.e("cola/$tag", msg, t) }
}
