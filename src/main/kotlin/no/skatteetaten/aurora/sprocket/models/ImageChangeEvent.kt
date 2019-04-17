package no.skatteetaten.aurora.sprocket.models

import org.apache.commons.codec.digest.DigestUtils

data class ImageChangeEvent(
    val name: String,
    val tag: String,
    // TODO: Parameterize this
    val repository: String = "utv-container-registry-internal-private-pull.aurora.skead.no:443"
) {

    val sha: String
        get() = "sha1-${DigestUtils.sha1Hex(url)}"

    val url: String
        get() = "$repository/$name:$tag"
}