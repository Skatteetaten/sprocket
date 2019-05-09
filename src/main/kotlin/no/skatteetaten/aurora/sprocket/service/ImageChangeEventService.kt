package no.skatteetaten.aurora.sprocket.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.models.ImageChangeEvent
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ImageChangeEventService {
    fun fromGlobalNexus(jsonPayload: JsonNode): ImageChangeEvent? {
        logger.debug("Payload=$jsonPayload")
        if (jsonPayload.at("/audit/domain").textValue() != "repository.component") {
            return null
        }

        val globalEventPayload = try {
            jacksonObjectMapper().convertValue<GlobalEventPayload>(jsonPayload)
        } catch (e: Exception) {
            logger.debug("Failed marshalling payload content into GlobalEvent message=${e.message}", e)
            return null
        }
        logger.debug("Payload=$globalEventPayload")
        val imageInfo = globalEventPayload.audit.attributes
        return imageInfo.toChangeEvent()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GlobalEventPayload(val audit: DockerAuditEvent)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DockerAuditEvent(val attributes: ImageInfo)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageInfo(
    val name: String?,
    val version: String?,
    @JsonProperty("repository.name") val repository: String?
)

fun ImageInfo.toChangeEvent(): ImageChangeEvent? {
    // TODO: Is this dead code? Not tested for now, want to investigate
    if (this.name == null || this.name.contains("sha256")) {
        return null
    }

    if (this.version != null) {
        return ImageChangeEvent(this.name, this.version)
    }

    val (group, name, version) = this.name
        .removePrefix("v2/")
        .replace("/manifests", "")
        .split("/", limit = 3)

    return ImageChangeEvent("$group/$name", version)
}