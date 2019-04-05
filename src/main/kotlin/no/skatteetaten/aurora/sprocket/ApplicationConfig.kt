package no.skatteetaten.aurora.sprocket

import io.fabric8.openshift.client.DefaultOpenShiftClient
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig {

    @Bean
    fun openShiftClient() = DefaultOpenShiftClient()
}
