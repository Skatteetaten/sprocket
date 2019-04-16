package no.skatteetaten.aurora.sprocket.service

import com.fkorotkov.openshift.from
import com.fkorotkov.openshift.metadata
import com.fkorotkov.openshift.newImageImportSpec
import com.fkorotkov.openshift.newImageStreamImport
import com.fkorotkov.openshift.spec
import com.fkorotkov.openshift.to
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.openshift.api.model.ImageStream
import io.fabric8.openshift.api.model.ImageStreamImport
import io.fabric8.openshift.client.DefaultOpenShiftClient
import mu.KotlinLogging
import no.skatteetaten.aurora.sprocket.controller.ImageChangeEvent
import no.skatteetaten.aurora.sprocket.jsonMapper
import no.skatteetaten.aurora.sprocket.service.ImageStreamImportGenerator.create
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class OpenShiftService(val client: DefaultOpenShiftClient) {

    fun findAffectedImageStreamResource(event: ImageChangeEvent): List<ImageStream> {

        logger.debug("Searching for imagestreams with sprocket=${event.sha} url=${event.url}")
        return client.imageStreams().inAnyNamespace().withLabel("skatteetaten.no/sprocket", event.sha).list()
            .items.also {
            logger.debug("Found items={} imagestreams", it.count())
        }
    }

    fun importImage(event: ImageChangeEvent, imageStream: ImageStream): ImageStreamImport {

        val import = create(
            dockerImageUrl = event.url,
            imageStreamName = imageStream.metadata.name,
            isiNamespace = imageStream.metadata.namespace,
            tag = "default" // because binary
        )

        logger.debug("Importing image with spec=$import")
        return client.importImage(import)
    }
}

fun DefaultOpenShiftClient.importImage(import: ImageStreamImport): ImageStreamImport {
    val url =
        this.openshiftUrl.toURI()
            .resolve("/apis/image.openshift.io/v1/namespaces/${import.metadata.namespace}/imagestreamimports")
    // logger.debug("Requesting url={}", url)
    return try {

        val body = jsonMapper.writeValueAsString(import)
        // logger.debug("body=$body")
        val json = MediaType.get("application/json; charset=utf-8")
        val request = Request.Builder()
            .url(url.toString())
            .post(RequestBody.create(json, body))
            .build()

        val response = this.httpClient.newCall(request).execute()
        jsonMapper.readValue(response.body()?.bytes(), ImageStreamImport::class.java)
            ?: throw KubernetesClientException("Error occurred while fetching list of applications in namespace=$namespace")
    } catch (e: Exception) {
        throw KubernetesClientException("Error occurred while fetching list of applications namespace=$namespace", e)
    }
}

object ImageStreamImportGenerator {

    fun create(
        dockerImageUrl: String,
        imageStreamName: String,
        isiNamespace: String,
        tag: String = "default"
    ): ImageStreamImport {
        return newImageStreamImport {

            metadata {
                name = imageStreamName
                namespace = isiNamespace
            }

            spec {
                import = true
                images = listOf(newImageImportSpec {
                    from {
                        kind = "DockerImage"
                        name = dockerImageUrl
                    }

                    to {
                        name = tag
                    }
                })
            }
        }
    }
}