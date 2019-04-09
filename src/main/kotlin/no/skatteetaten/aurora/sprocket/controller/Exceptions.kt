package no.skatteetaten.aurora.sprocket.controller

open class SprocketException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// TODO: Sprocket trenger kanskje ikke egne exceptions?
open class RandomException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
