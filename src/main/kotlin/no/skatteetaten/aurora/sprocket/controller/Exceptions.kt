package no.skatteetaten.aurora.sprocket.controller

// TODO: Noen trenger man kanskje for logging?
class BadRequestException(
    message: String,
    cause: Throwable? = null
) : SprocketException(message, cause)

class ForbiddenException(
    message: String,
    cause: Throwable? = null
) : SprocketException(message, cause)

open class SprocketException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class SourceSystemException(
    message: String,
    cause: Throwable? = null,
    val sourceSystem: String? = null
) : SprocketException(message, cause)