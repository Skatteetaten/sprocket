package no.skatteetaten.aurora.sprocket.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import no.skatteetaten.aurora.sprocket.jsonMapper
import no.skatteetaten.aurora.sprocket.utils.ResourceLoader
import org.junit.jupiter.api.Test

class ImageChangeEventServiceTest : ResourceLoader() {

    val service = ImageChangeEventService()

    @Test
    fun `should parse global event`() {

        val event = service.fromGlobalNexus(loadJsonResource("globalNexus.json", "events"))
        assertThat(event?.name).isEqualTo("no_skatteetaten_aurora/test")
        assertThat(event?.tag).isEqualTo("1")
    }

    @Test
    fun `should parse global component event with version present`() {
        val event = service.fromGlobalNexus(loadJsonResource("globalNexusNew.json", "events"))
        assertThat(event?.name).isEqualTo("sprocket-test/alpine-test2")
        assertThat(event?.tag).isEqualTo("0.2")
    }

    @Test
    fun `should ignore asset events`() {
        val event = service.fromGlobalNexus(loadJsonResource("globalNexusAsset.json", "events"))
        assertThat(event).isNull()
    }

    @Test
    fun `should ignore json that is not valid nexus payload`() {
        val event = service.fromGlobalNexus(
            jsonMapper.readTree(
                """
            {
             "audit": {
               "domain": "repository.component"
              },
              "foo" : "bar"
            }
            """
            )
        )
        assertThat(event).isNull()
    }
}