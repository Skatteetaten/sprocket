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
import no.skatteetaten.aurora.sprocket.jsonMapper
import no.skatteetaten.aurora.sprocket.service.ImageStreamImportGenerator.create
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class OpenShiftService(
    val client: DefaultOpenShiftClient,
    @Value("\${sprocket.docker.pull:container-registry-internal-private-pull.aurora.skead.no:443}") val pullUrl: String
) {

    @Async
    fun findAndImportAffectedImages(event: ImageChangeEvent) {
        val url = "$pullUrl/${event.name}:${event.tag}"
        val sha = "sha1-${DigestUtils.sha1Hex(url)}"

        logger.info("Searching for resources with sprocket=$sha url=$url")
        val affectedImages = findAffectedImageStreamResource(sha)

        affectedImages.map {
            importImage(it, url)
        }
        /*findAffectedDeployments(sha).map {
            patchDeployment(it, url)
        }*/
    }

    fun findAffectedImageStreamResource(sha: String): List<ImageStream> =
        client.imageStreams().inAnyNamespace().withLabel("skatteetaten.no/sprocket", sha).list().items

    /*
    fun patchDeployment(deployment:Deployment, url:String): Boolean {
        deployment.metadata.labels.put("updatedAt", )
        return client.apps().deployments()
            .inNamespace(deployment.metadata.namespace)
            .withName(deployment.metadata.name)
            .buildMetadata().labels
    }

    fun findAffectedDeployments(sha: String): List<Deployment> {
        return client.apps().deployments().inAnyNamespace().withLabel("skatteetaten.no/sprocket", sha).list()
            .items.also {
            logger.info("Found items={} deployment", it.count())
        }
    }

    */
    fun importImage(
        imageStream: ImageStream,
        url: String
    ): ImageStreamImport {

        val import = create(
            dockerImageUrl = url,
            imageStreamName = imageStream.metadata.name,
            isiNamespace = imageStream.metadata.namespace,
            tag = "default"
        )

        logger.info("Importing image with app=${import.metadata.namespace}/${import.metadata.name} url=$url")
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