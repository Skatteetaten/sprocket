package no.skatteetaten.aurora.sprocket.controller

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockito_kotlin.any
import no.skatteetaten.aurora.mockmvc.extensions.Path
import no.skatteetaten.aurora.mockmvc.extensions.contentTypeJson
import no.skatteetaten.aurora.mockmvc.extensions.header
import no.skatteetaten.aurora.mockmvc.extensions.mock.withContractResponse
import no.skatteetaten.aurora.mockmvc.extensions.post
import no.skatteetaten.aurora.mockmvc.extensions.status
import no.skatteetaten.aurora.mockmvc.extensions.statusIsOk
import no.skatteetaten.aurora.sprocket.ApplicationConfig
import no.skatteetaten.aurora.sprocket.security.NEXUS_SECURITY_HEADER
import no.skatteetaten.aurora.sprocket.service.ImageChangeEventService
import no.skatteetaten.aurora.sprocket.service.OpenShiftService
import no.skatteetaten.aurora.sprocket.utils.ResourceLoader
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureRestDocs
@WebMvcTest(
    value = [NexusWebhookController::class,
        ApplicationConfig::class,
        ImageChangeEventService::class],
    secure = true
)
class ApplicationDeploymentControllerTest(
    @Autowired private val mockMvc: MockMvc
) : ResourceLoader() {

    @MockBean
    private lateinit var service: OpenShiftService

    @Test
    fun `post event with invalid hmac should fail`() {

            mockMvc.post(
                path = Path("/nexus/global"),
                headers = HttpHeaders().contentTypeJson()
                    .header(NEXUS_SECURITY_HEADER, "LukeSkywalker"),
                body = loadJsonResource<JsonNode>("globalNexus.json", "events")
            ) {
                status(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun `post valid event to global nexus hook`() {

        given(
            service.findAffectedImageStreamResource(any())
        ).withContractResponse("nexus/imageStreamList") {
            willReturn(content)
        }

        given(
            service.importImage(any(), any())
        ).withContractResponse("import", "openshift") {
            willReturn(content)
        }


        mockMvc.post(
            path = Path("/nexus/global"),
            headers = HttpHeaders().contentTypeJson()
                .header(NEXUS_SECURITY_HEADER, "0385d9e7e3320bfe348d7e53a6123e1a8c9e2324"),
            body = loadJsonResource<JsonNode>("globalNexus.json", "events")
        ) {
            statusIsOk()
        }
    }
}