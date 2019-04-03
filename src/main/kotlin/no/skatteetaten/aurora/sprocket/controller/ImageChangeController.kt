package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger{}

@RestController
class ImageChangeController {

    @RequestMapping("/global")
    fun logGlobalEvent(@RequestBody eventPayload: JsonNode) {
        logger.info {
            jacksonObjectMapper().writeValueAsString(eventPayload)
        }
    }

    @RequestMapping("/repo")
    fun logRepositoryEvent(@RequestBody eventPayload: JsonNode) {
        logger.info {
            jacksonObjectMapper().writeValueAsString(eventPayload)
        }
    }

}