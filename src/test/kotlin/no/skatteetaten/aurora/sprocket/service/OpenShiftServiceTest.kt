package no.skatteetaten.aurora.sprocket.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.fabric8.kubernetes.api.model.RootPaths
import io.fabric8.openshift.api.model.ImageStreamList
import io.fabric8.openshift.client.DefaultOpenShiftClient
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.sprocket.models.ImageChangeEvent
import no.skatteetaten.aurora.sprocket.utils.ResourceLoader
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

class OpenShiftServiceTest : ResourceLoader() {

    private val server = MockWebServer()
    private val rootUrl = server.url("/").toString()
    private var mockClient = DefaultOpenShiftClient(rootUrl)
    private val root = RootPaths()
    private val imageStreamList: ImageStreamList = loadJsonResource("imagestreams.json", "openshift")
    private val imageStream= imageStreamList.items.first()
    private val service = OpenShiftService(mockClient)
    private val event = ImageChangeEvent("no_skatteetaten_aurora", "test", "test.container.registry:443")

    @Test
    fun `test fetching imageStream for event`() {


        val requests = server.execute(root, imageStreamList) {
            val isList = service.findAffectedImageStreamResource(event)
            assertThat(isList.size).isEqualTo(2)
        }
        assertThat(requests.size).isEqualTo(2)
        val url = requests[1].requestUrl
        val labelSelector = url.queryParameter("labelSelector")
        assertThat(labelSelector).isEqualTo("skatteetaten.no/sprocket=${event.sha}")
    }

    @Test
    fun `test importing image`() {

        val import= loadBufferResource("import.json", "openshift")
        val requests= server.execute(import) {
            val response= service.importImage(event, imageStream)
            assertThat(response).isNotNull()
        }
        assertThat(requests.size).isEqualTo(1)

        val postRequest = requests[0]
        assertThat(postRequest.body).isEqualTo(import)
        assertThat(postRequest.requestUrl.toString()).isEqualTo("${rootUrl}apis/image.openshift.io/v1/namespaces/aurora-test/imagestreamimports")
    }
}