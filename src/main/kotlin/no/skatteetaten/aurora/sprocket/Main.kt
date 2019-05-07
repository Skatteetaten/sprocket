package no.skatteetaten.aurora.sprocket

import io.fabric8.openshift.client.DefaultOpenShiftClient
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication
class Main

fun main(args: Array<String>) {
    SpringApplication.run(Main::class.java, *args)
}

@Configuration
class ApplicationConfig {

    @Bean
    fun openShiftClient() = DefaultOpenShiftClient()

    @Bean
    fun hmacUtils(
        @Value("\${sprocket.nexus.secret:123456}") secretKey: String
    ): HmacUtils {
        return HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKey)
    }
}
