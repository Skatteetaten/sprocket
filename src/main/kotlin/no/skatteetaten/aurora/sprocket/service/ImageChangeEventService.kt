package no.skatteetaten.aurora.sprocket.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ImageChangeEventService {
    fun fromGlobalNexus(jsonPayload: JsonNode): ImageChangeEvent? {
        logger.debug("Payload=$jsonPayload")
        if (jsonPayload.at("/audit/domain").textValue() != "repository.asset") {
            return null
        }

        val globalEventPayload = try {
            jacksonObjectMapper().convertValue<GlobalEventPayload>(jsonPayload)
        } catch (e: Exception) {
            logger.debug("Failed marshalling payload content into GlobalEvent message=${e.message}", e)
            return null
        }
        logger.debug("Parsed=$globalEventPayload")
        val initiator = globalEventPayload.initiator.split("/").first()

        val imageInfo = globalEventPayload.audit.attributes
        if (imageInfo.repository.contains("proxy")) {
            return null
        }

        if (imageInfo.name.contains("sha256")) {
            return null
        }

        if (imageInfo.version != null) {
            return ImageChangeEvent(
                name = imageInfo.name,
                tag = imageInfo.version,
                pushRepository = imageInfo.repository,
                initiator = initiator
            )
        }

        val (group, name, version) = imageInfo.name
            .removePrefix("v2/")
            .replace("/manifests", "")
            .split("/", limit = 3)

        return ImageChangeEvent(
            name = "$group/$name", tag = version,
            pushRepository = imageInfo.repository,
            initiator = initiator
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GlobalEventPayload(
    val audit: DockerAuditEvent,
    val initiator: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DockerAuditEvent(val attributes: ImageInfo)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageInfo(
    val name: String,
    val version: String?,
    @JsonProperty("repository.name") val repository: String
)

data class ImageChangeEvent(
    val name: String,
    val tag: String,
    val pushRepository: String,
    val initiator: String
)