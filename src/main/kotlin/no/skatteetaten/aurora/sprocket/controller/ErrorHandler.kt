package no.skatteetaten.aurora.sprocket.controller

import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val errorLogger = KotlinLogging.logger {}

@ControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(RuntimeException::class)
    fun handleGenericError(e: RuntimeException, request: WebRequest): ResponseEntity<Any>? {
        return handleException(e, request, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsError(e: RuntimeException, request: WebRequest): ResponseEntity<Any>? {
        return handleException(e, request, HttpStatus.UNAUTHORIZED)
    }
    private fun handleException(e: Exception, request: WebRequest, httpStatus: HttpStatus): ResponseEntity<Any>? {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        errorLogger.debug("Handle exception", e)
        return handleExceptionInternal(e, e, headers, httpStatus, request)
    }
}
