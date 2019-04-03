package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class ImageChangeController {

    @RequestMapping("/global")
    fun logGlobalEvent(@RequestBody globalEventPayload: GlobalEventPayload) {
        val imageInfo = globalEventPayload.audit.attributes ?: return

        val imageChangeEvent = ImageChangeEvent.create(imageInfo) ?: return
        logger.error {
            jacksonObjectMapper().writeValueAsString(imageChangeEvent)
        }
    }

    data class ImageInfo(
        val name: String,
        val version: String?,
        @JsonProperty("repository.name") val repository: String
    )

    data class GlobalEventPayload(val audit: DockerAuditEvent)

    data class DockerAuditEvent(val attributes: ImageInfo?)

    data class ImageChangeEvent(
        val repository: String,
        val name: String, // group/name
        val tag: String
    ) {
        companion object {
            fun create(imageInfo: ImageInfo): ImageChangeEvent? {
                if (imageInfo.name.contains("sha256")) {
                    return null
                }
                val repository = imageInfo.repository
                val (name, tag) = retrieveImageInfo(imageInfo)

                return ImageChangeEvent(repository, name, tag)
            }

            fun retrieveImageInfo(imageInfo: ImageInfo): List<String> {
                if (imageInfo.version != null) {
                    return listOf(imageInfo.name, imageInfo.version)
                }

                val imageInfoName = imageInfo.name.removePrefix("v2/").split("/").filter { it != "manifests" }
                val tag = imageInfoName.takeLast(1).first()
                val name = imageInfoName.dropLast(1).joinToString("/")
                return listOf(name, tag)
            }
        }
    }

    // ${pullRepositories[repository]}/${name}:${tag}
}