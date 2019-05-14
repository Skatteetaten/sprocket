package no.skatteetaten.aurora.sprocket.controller

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.apache.commons.codec.digest.HmacUtils
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
class NexusWebhookControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val hmac: HmacUtils
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
    fun `test hmac`() {

        val content =
            """{"nodeId":"601A9C7C-DB5525A1-889F9A97-14DA6363-83976FD4","initiator":"builder-aurora-build/10.209.2.52","audit":{"domain":"repository.asset","type":"updated","context":"v2/no_skatteetaten_aurora_demo/whoami/manifests/0.0.14","attributes":{"repository.name":"internal-hosted-releases","format":"docker","name":"v2/no_skatteetaten_aurora_demo/whoami/manifests/0.0.14"}}}"""
        val hex = hmac.hmacHex(content)
        assertThat(hex).isEqualTo("")
    }

    @Test
    fun `post valid event to global nexus hook`() {

        val body = loadJsonResource<JsonNode>("globalNexus.json", "events")
        val expectedHmac = hmac.hmacHex(jacksonObjectMapper().writeValueAsBytes(body))

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
                .header(NEXUS_SECURITY_HEADER, expectedHmac),
            body = body
        ) {
            statusIsOk()
        }
    }
}