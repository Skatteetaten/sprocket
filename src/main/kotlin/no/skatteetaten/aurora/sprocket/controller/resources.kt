package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.q3c.rest.hal.HalResource

private val logger = KotlinLogging.logger {}
// TODO: Trenger vi alt dette?
// Tror ikke det, undersøker bruk av openshiftclient, evt kubernetes client
sealed class Try<out A, out B> {
    class Success<A>(val value: A) : Try<A, Nothing>()
    class Failure<B>(val value: B) : Try<Nothing, B>()
}

fun <S : Any, T : Any> List<Try<S, T>>.getSuccessAndFailures(): Pair<List<S>, List<T>> {
    val items = this.mapNotNull {
        if (it is Try.Success) {
            it.value
        } else null
    }

    val failure = this.mapNotNull {
        if (it is Try.Failure) {
            if (it.value is SprocketFailure) logger.debug(it.value.error) { "An error has occurred" }
            it.value
        } else null
    }

    return Pair(items, failure)
}

fun <S, T> Try<S, T>.getSuccessAndFailures(): Pair<List<S>, List<T>> {
    val item = if (this is Try.Success) {
        listOf(this.value)
    } else emptyList()
    val failure = if (this is Try.Failure) {
        listOf(this.value)
    } else emptyList()

    return Pair(item, failure)
}

data class SprocketFailure(
    val url: String,
    @JsonIgnore val error: Throwable? = null
) {
    val errorMessage: String = error?.let { it.message ?: "Unknown error (${it::class.simpleName})" } ?: ""
}

data class AuroraResponse<T : HalResource?>(
    val items: List<T> = emptyList(),
    val failure: List<SprocketFailure> = emptyList(),
    val success: Boolean = true,
    val message: String = "OK",
    val failureCount: Int = failure.size,
    val successCount: Int = items.size,
    val count: Int = failureCount + successCount
) : HalResource()

@Component
class AuroraResponseAssembler {

    fun <T : HalResource> toAuroraResponse(responses: List<Try<T, SprocketFailure>>): AuroraResponse<T> {
        val (items, failures) = responses.getSuccessAndFailures()

        return AuroraResponse(
            success = failures.isEmpty(),
            message = if (failures.isNotEmpty()) failures.first().errorMessage else "Success",
            items = items,
            failure = failures
        )
    }

    fun <T : HalResource> toAuroraResponse(responses: Try<List<T>, SprocketFailure>): AuroraResponse<T> {
        val itemsAndFailure = responses.getSuccessAndFailures()
        val items = itemsAndFailure.first.firstOrNull() ?: emptyList()
        val failures = itemsAndFailure.second

        return AuroraResponse(
            success = failures.isEmpty(),
            message = if (failures.isNotEmpty()) failures.first().errorMessage else "Success",
            items = items,
            failure = failures
        )
    }
}
