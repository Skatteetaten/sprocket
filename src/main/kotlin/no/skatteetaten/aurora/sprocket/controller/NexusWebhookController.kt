package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.service.ImageChangeEventService
import no.skatteetaten.aurora.sprocket.service.OpenShiftService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/nexus")
class NexusWebhookController(
    val openshiftService: OpenShiftService,
    val imageChangeEventService: ImageChangeEventService
) {


    @PostMapping("/repository")
    fun repoEvent(@RequestBody jsonPayload: JsonNode){
        logger.debug("repoPayload=$jsonPayload")
    }

    /*
      This is very MVP, How do we handle robustnes and scale here?
      We need to broadcast this ImageChangeEvent to all sprockets from this sprocket. So there needs to be some async flow in here.
     */
    @PostMapping("/global")
    fun globalEvent(@RequestBody jsonPayload: JsonNode) {

        val imageChangeEvent = imageChangeEventService.fromGlobalNexus(jsonPayload) ?: return
        logger.info("Event=$imageChangeEvent")
        // TODO: Here we need to somehow broadcast this signal to a list of sprockets in other clusters.

        openshiftService.findAffectedImageStreamResource(imageChangeEvent).map {
            openshiftService.importImage(imageChangeEvent, it)
        }
    }
}