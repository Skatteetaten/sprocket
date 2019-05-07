plugins {
    id("org.springframework.cloud.contract")
    id("org.jetbrains.kotlin.jvm") version "1.3.31"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("org.jlleitschuh.gradle.ktlint") version "8.0.0"

    id("org.springframework.boot") version "2.1.4.RELEASE"
    id("org.asciidoctor.convert") version "2.2.0"

    id("com.gorylenko.gradle-git-properties") version "2.0.0"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.9"

    id("no.skatteetaten.gradle.aurora") version "2.3.1"
}

apply(plugin = "spring-cloud-contract")

dependencies {
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.1")

    implementation("commons-codec:commons-codec:1.12")
    implementation("io.fabric8:openshift-client:4.2.2")
    testImplementation("io.fabric8:openshift-server-mock:4.2.2")
    implementation("com.fkorotkov:kubernetes-dsl:2.0.1")
    implementation("commons-io:commons-io:2.6")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.14")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:0.6.4")
}