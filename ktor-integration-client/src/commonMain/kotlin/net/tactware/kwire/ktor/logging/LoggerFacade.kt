package net.tactware.kwire.ktor.logging

interface LoggerFacade {
    val isDebugEnabled: Boolean

    fun trace(message: String, vararg args: Any?)
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, throwable: Throwable? = null, vararg args: Any?)
}

expect fun createLogger(name: String): LoggerFacade



