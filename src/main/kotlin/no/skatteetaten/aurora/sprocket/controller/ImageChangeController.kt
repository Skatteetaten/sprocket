package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.service.OpenShiftService
import no.skatteetaten.aurora.utils.sha1
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

@RestController
class ImageChangeController(val service: OpenShiftService) {

    @RequestMapping("/global")
    fun logGlobalEvent(@RequestBody jsonPayload: JsonNode, req: HttpServletRequest) {

        if (jsonPayload.at("$.audit.domain").toString() != "repository.component") {
            return
        }

        val headers = req.headerNames.toList().associate {
            it to req.getHeader(it)
        }
        val headersJson = jacksonObjectMapper().valueToTree<JsonNode>(headers).toString()
        logger.debug("header=$headersJson, body=$jsonPayload")

        val globalEventPayload = try {
            jacksonObjectMapper().convertValue<GlobalEventPayload>(jsonPayload)
        } catch (e: Exception) {
            return
        }
        // logger.debug("Payload=$globalEventPayload")
        val imageInfo = globalEventPayload.audit.attributes ?: return

        val imageChangeEvent = ImageChangeEvent.create(imageInfo) ?: return
        // logger.info("{} url={} sha={}", imageChangeEvent, imageChangeEvent.url, imageChangeEvent.sha)
        val results = service.findAffectedImageStreamResource(imageChangeEvent).map {
            service.importImage(imageChangeEvent, it)
        }
        // results.forEach { logger.info("response=${jsonMapper.writeValueAsString(it)}") }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageInfo(
    val name: String?,
    val version: String?,
    @JsonProperty("repository.name") val repository: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GlobalEventPayload(val audit: DockerAuditEvent)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DockerAuditEvent(val attributes: ImageInfo?)

data class ImageChangeEvent(
    val name: String,
    val tag: String,
    val repository: String = "utv-container-registry-internal-private-pull.aurora.skead.no:443"
) {

    val sha: String
        get() = "sha1-${url.sha1()}"

    val url: String
        get() = "$repository/$name:$tag"

    companion object {

        fun create(imageInfo: ImageInfo): ImageChangeEvent? {
            if (imageInfo.name == null || imageInfo.name.contains("sha256")) {
                return null
            }

            if (imageInfo.version != null) {
                return ImageChangeEvent(imageInfo.name, imageInfo.version)
            }

            val (group, name, version) = imageInfo.name
                .removePrefix("v2/")
                .replace("/manifests", "")
                .split("/", limit = 3)

            return ImageChangeEvent("$group/$name", version)
        }
    }
}
