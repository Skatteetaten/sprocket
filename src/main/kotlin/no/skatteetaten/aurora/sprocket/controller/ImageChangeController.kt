package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.jsonMapper
import no.skatteetaten.aurora.sprocket.service.OpenShiftService
import no.skatteetaten.aurora.utils.sha1
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

@RestController
class ImageChangeController(
    val service: OpenShiftService,
    @Value("\${sprocket.nexus.nodeId}") val nodeId: String
) {

    @RequestMapping("/global")
    fun logGlobalEvent(
        @RequestHeader(value = "x-nexus-webhook-signature", required = true) signature: String,
        @Value("\${sprocket.nexus.secret:123456}") secretKey: String,
        request: HttpServletRequest
    ) {

        val body = request.inputStream.readAllBytes()
        val jsonPayload: JsonNode = jsonMapper.readValue(body)

        if (jsonPayload.at("/audit/domain").textValue() != "repository.component") {
            return
        }

        // spring security.
        if (jsonPayload.at("/nodeId").textValue() != nodeId) {
            return
        }
        val hmac = HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKey).hmacHex(body)
        logger.debug(signature)
        logger.debug(hmac)
        logger.debug("body=$jsonPayload")
        // end spring security

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
        get() = "sha1-${DigestUtils.sha1Hex(url)}"

    val sha_old: String
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
