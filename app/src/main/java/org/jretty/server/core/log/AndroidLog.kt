package org.jretty.server.core.log

import android.util.Log
import org.eclipse.jetty.util.log.Logger
import java.util.*

class AndroidLog(name: String) : Logger {
    companion object {
        private val __JETTY_TAG = "Jetty"
        val __isIgnoredEnabled = false
    }

    constructor() : this(AndroidLog::class.java.name) {
    }

    override fun getName(): String {
        return name
    }

    override fun warn(msg: String?, vararg args: Any?) {
        if (args!=null) {
            Log.w(__JETTY_TAG, msg + ": " + Arrays.asList(args))
        } else {
            Log.w(__JETTY_TAG, msg!!)
        }
    }

    override fun warn(thrown: Throwable?) {
        Log.w(__JETTY_TAG, "", thrown)
    }

    override fun warn(msg: String?, thrown: Throwable?) {
        Log.w(__JETTY_TAG, msg, thrown)
    }

    override fun info(msg: String?, vararg args: Any?) {
        if (Log.isLoggable(__JETTY_TAG, Log.INFO)) {
            if (args!=null) {
                Log.i(__JETTY_TAG, msg + ": " + Arrays.asList(args))
            } else {
                Log.i(__JETTY_TAG, msg!!)
            }
        }
    }

    override fun info(thrown: Throwable?) {
        if (Log.isLoggable(__JETTY_TAG, Log.INFO)) {
            Log.i(__JETTY_TAG, "", thrown)
        }
    }

    override fun info(msg: String?, thrown: Throwable?) {
        if (Log.isLoggable(__JETTY_TAG, Log.INFO)) {
            Log.i(__JETTY_TAG, msg, thrown)
        }
    }

    override fun isDebugEnabled(): Boolean {
        return Log.isLoggable(__JETTY_TAG, Log.DEBUG)
    }

    override fun setDebugEnabled(enabled: Boolean) {
    }

    override fun debug(msg: String?, vararg args: Any?) {
        if (Log.isLoggable(__JETTY_TAG, Log.DEBUG)) {
            if (args!=null) {
                Log.d(__JETTY_TAG, msg + ": " + Arrays.asList(args))
            } else {
                Log.d(__JETTY_TAG, msg!!)
            }
        }
    }

    override fun debug(thrown: Throwable?) {
        if (Log.isLoggable(__JETTY_TAG, Log.DEBUG)) {
            Log.d(__JETTY_TAG, "", thrown)
        }
    }

    override fun debug(msg: String?, thrown: Throwable?) {
        if (Log.isLoggable(__JETTY_TAG, Log.DEBUG)) {
            Log.d(__JETTY_TAG, msg, thrown)
        }
    }

    override fun getLogger(name: String?): Logger {
        if (name != null) {
            return AndroidLog(name)
        }
        return AndroidLog()
    }

    override fun ignore(ignored: Throwable?) {
        if (__isIgnoredEnabled) Log.w(__JETTY_TAG, "IGNORED", ignored)
    }

}