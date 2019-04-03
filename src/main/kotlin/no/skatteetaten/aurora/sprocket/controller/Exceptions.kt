package no.skatteetaten.aurora.sprocket.controller

open class SprocketException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

open class RandomException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)