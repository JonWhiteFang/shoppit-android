package com.shoppit.app.domain.error

/**
 * Fake implementation of ErrorLogger for testing.
 * Records all logged errors for verification in tests.
 */
class FakeErrorLogger : ErrorLogger {
    
    private val _errors = mutableListOf<LoggedError>()
    val errors: List<LoggedError> get() = _errors
    
    private val _warnings = mutableListOf<LoggedMessage>()
    val warnings: List<LoggedMessage> get() = _warnings
    
    private val _infos = mutableListOf<LoggedMessage>()
    val infos: List<LoggedMessage> get() = _infos
    
    override fun logError(
        error: Throwable,
        context: String,
        additionalData: Map<String, Any>
    ) {
        _errors.add(LoggedError(error, context, additionalData))
    }
    
    override fun logWarning(message: String, context: String) {
        _warnings.add(LoggedMessage(message, context))
    }
    
    override fun logInfo(message: String, context: String) {
        _infos.add(LoggedMessage(message, context))
    }
    
    fun clear() {
        _errors.clear()
        _warnings.clear()
        _infos.clear()
    }
    
    data class LoggedError(
        val error: Throwable,
        val context: String,
        val additionalData: Map<String, Any>
    )
    
    data class LoggedMessage(
        val message: String,
        val context: String
    )
}
