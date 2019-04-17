package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.jsonMapper
import no.skatteetaten.aurora.sprocket.models.ImageChangeEvent
import no.skatteetaten.aurora.sprocket.service.OpenShiftService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/nexus")
class NexusWebhookController(
    val service: OpenShiftService
) {

    @PostMapping("/global")
    fun globalEvent(@RequestBody jsonPayload: JsonNode ) {

        if (jsonPayload.at("/audit/domain").textValue() != "repository.component") {
            return
        }

        val globalEventPayload = try {
            jacksonObjectMapper().convertValue<GlobalEventPayload>(jsonPayload)
        } catch (e: Exception) {
            logger.debug("Failed marshalling payload content into GlobalEvent message=${e.message}", e)
            return
        }
        logger.debug("Payload=$globalEventPayload")
        val imageInfo = globalEventPayload.audit.attributes ?: return

        val imageChangeEvent = imageInfo.toChangeEvent() ?: return
        service.findAffectedImageStreamResource(imageChangeEvent).map {
            service.importImage(imageChangeEvent, it)
        }
        // TODO Should this return all affected imageChanges with notification that they succeeded or not.
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GlobalEventPayload(val audit: DockerAuditEvent)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DockerAuditEvent(val attributes: ImageInfo?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageInfo(
    val name: String?,
    val version: String?,
    @JsonProperty("repository.name") val repository: String?
)

fun ImageInfo.toChangeEvent(): ImageChangeEvent? {
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