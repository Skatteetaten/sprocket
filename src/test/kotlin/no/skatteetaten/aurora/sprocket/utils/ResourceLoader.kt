package no.skatteetaten.aurora.sprocket.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.sprocket.jsonMapper
import okio.Buffer
import org.springframework.util.ResourceUtils
import java.io.InputStream
import java.net.URL

open class ResourceLoader {

    fun loadResource(resourceName: String, folder: String = "${this.javaClass.`package`.name}/${this.javaClass.simpleName}"): String =
        getResourceUrl(resourceName, folder).readText()

    fun getResourceUrl(resourceName: String, folder: String = "${this.javaClass.`package`.name}/${this.javaClass.simpleName}"): URL {
        val pck = folder.replace(".", "/")
        val path = "src/test/resources/$pck/$resourceName"
        return ResourceUtils.getURL(path)
    }


    inline fun <reified T>loadJsonResource(resourceName: String, folder: String = "${this.javaClass.`package`.name}/${this.javaClass.simpleName}"): T =
        jacksonObjectMapper().readValue(loadResource(resourceName, folder))

    fun loadByteResource(resourceName: String, folder: String = this.javaClass.simpleName): ByteArray {
        return getResourceUrl(resourceName, folder).openStream().readBytes()
    }



    fun loadStreamResource(resourceName: String, folder: String = "${this.javaClass.`package`.name}/${this.javaClass.simpleName}"): InputStream? {
        return getResourceUrl(resourceName, folder).openStream()
    }

    fun loadBufferResource(resourceName: String, folder: String = "${this.javaClass.`package`.name}/${this.javaClass.simpleName}"): Buffer {
        return Buffer().readFrom(getResourceUrl(resourceName, folder).openStream())
    }

    fun compareJson(jsonNode: JsonNode, target: JsonNode): Boolean {
        val writer = jsonMapper.writerWithDefaultPrettyPrinter()
        val targetString = writer.writeValueAsString(target)
        val nodeString = writer.writeValueAsString(jsonNode)
        assertThat(targetString).isEqualTo(nodeString)
        return true
    }
}