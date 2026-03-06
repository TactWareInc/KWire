package net.tactware.kwire.ktor.logging

private class ConsoleLogger(private val name: String) : LoggerFacade {
    override val isDebugEnabled: Boolean = true
    override fun trace(message: String, vararg args: Any?) {
        console.log(prefix("TRACE") + format(message, args))
    }

    override fun debug(message: String, vararg args: Any?) {
        console.log(prefix("DEBUG") + format(message, args))
    }

    override fun info(message: String, vararg args: Any?) {
        console.info(prefix("INFO") + format(message, args))
    }

    override fun warn(message: String, vararg args: Any?) {
        console.warn(prefix("WARN") + format(message, args))
    }

    override fun error(message: String, throwable: Throwable?, vararg args: Any?) {
        if (throwable != null) {
            console.error(prefix("ERROR") + format(message, args), throwable)
        } else {
            console.error(prefix("ERROR") + format(message, args))
        }
    }

    private fun prefix(level: String): String = "[${level}][${name}] "

    private fun format(template: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return template
        var idx = 0
        return buildString {
            var i = 0
            while (i < template.length) {
                if (i + 1 < template.length && template[i] == '{' && template[i + 1] == '}') {
                    append(args.getOrNull(idx)?.toString() ?: "null")
                    idx += 1
                    i += 2
                } else {
                    append(template[i])
                    i += 1
                }
            }
        }
    }
}

actual fun createLogger(name: String): LoggerFacade = ConsoleLogger(name)



