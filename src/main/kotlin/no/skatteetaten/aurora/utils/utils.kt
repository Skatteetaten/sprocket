package no.skatteetaten.aurora.utils

fun String.sha1() = Digester.hexDigest(this, "SHA-1")
