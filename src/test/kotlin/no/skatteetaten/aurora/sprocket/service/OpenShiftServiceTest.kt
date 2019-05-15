package no.skatteetaten.aurora.sprocket.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.fabric8.kubernetes.api.model.RootPaths
import io.fabric8.openshift.api.model.ImageStreamList
import io.fabric8.openshift.client.DefaultOpenShiftClient
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.setJsonFileAsBody
import no.skatteetaten.aurora.sprocket.utils.ResourceLoader
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.Test

class OpenShiftServiceTest : ResourceLoader() {

    private val server = MockWebServer()
    private val rootUrl = server.url("/").toString()
    private var mockClient = DefaultOpenShiftClient(rootUrl)
    private val root = RootPaths()
    private val imageStreamList: ImageStreamList = loadJsonResource("imagestreams.json", "openshift")
    private val imageStream = imageStreamList.items.first()
    private val pullUrl = "test.container.registry:443"
    private val service = OpenShiftService(mockClient, pullUrl)

    private val url = "$pullUrl/no_skatteetaten_aurora:test"
    private val sha = "sha1-${DigestUtils.sha1Hex(url)}"

    @Test
    fun `test fetching imageStream for event`() {

        val requests = server.execute(root, imageStreamList) {
            val isList = service.findAffectedImageStreamResource(sha)
            assertThat(isList.size).isEqualTo(2)
        }
        assertThat(requests.size).isEqualTo(2)
        val url = requests[1].requestUrl
        val labelSelector = url.queryParameter("labelSelector")
        assertThat(labelSelector).isEqualTo("skatteetaten.no/sprocket=$sha")
    }

    @Test
    fun `test importing image`() {

        val response = MockResponse().setJsonFileAsBody("openshift/import.json")
        val requests = server.execute(response) {
            val response = service.importImage(imageStream, url)
            assertThat(response).isNotNull()
        }
        assertThat(requests.size).isEqualTo(1)

        val postRequest = requests[0]
        assertThat(postRequest.body).isEqualTo(response.body)
        assertThat(postRequest.requestUrl.toString()).isEqualTo("${rootUrl}apis/image.openshift.io/v1/namespaces/aurora-test/imagestreamimports")
    }
}