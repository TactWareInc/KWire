package net.tactware.kwire.ktor.logging

import org.slf4j.LoggerFactory

private class Slf4jLogger(private val delegate: org.slf4j.Logger) : LoggerFacade {
    override val isDebugEnabled: Boolean
        get() = delegate.isDebugEnabled

    override fun trace(message: String, vararg args: Any?) {
        delegate.trace(message, *args)
    }

    override fun debug(message: String, vararg args: Any?) {
        delegate.debug(message, *args)
    }

    override fun info(message: String, vararg args: Any?) {
        delegate.info(message, *args)
    }

    override fun warn(message: String, vararg args: Any?) {
        delegate.warn(message, *args)
    }

    override fun error(message: String, throwable: Throwable?, vararg args: Any?) {
        if (throwable != null) {
            delegate.error(message, throwable)
        } else {
            delegate.error(message, *args)
        }
    }
}

actual fun createLogger(name: String): LoggerFacade = Slf4jLogger(LoggerFactory.getLogger(name))



