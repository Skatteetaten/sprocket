package no.skatteetaten.aurora.sprocket

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import kotlinx.coroutines.newFixedThreadPoolContext
import mu.KotlinLogging
import no.skatteetaten.aurora.filter.logging.AuroraHeaderFilter
import no.skatteetaten.aurora.filter.logging.RequestKorrelasjon
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import reactor.netty.tcp.TcpClient
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig {

    @Bean
    fun threadPoolContext(@Value("\${sprocket.threadPoolSize:6}") threadPoolSize: Int) =
        newFixedThreadPoolContext(threadPoolSize, "sprocket")

    @Bean
    fun webClient(
        builder: WebClient.Builder,
        tcpClient: TcpClient,
        @Value("\${spring.application.name}") applicationName: String
    ) =
        builder
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("KlientID", applicationName)
            .defaultHeader(AuroraHeaderFilter.KORRELASJONS_ID, RequestKorrelasjon.getId())
            .exchangeStrategies(exchangeStrategies())
            .filter(ExchangeFilterFunction.ofRequestProcessor {
                val bearer = it.headers()[HttpHeaders.AUTHORIZATION]?.firstOrNull()?.let { token ->
                    val t = token.substring(0, min(token.length, 11)).replace("Bearer", "")
                    "bearer=$t"
                } ?: ""
                logger.debug("HttpRequest method=${it.method()} url=${it.url()} token=$bearer")
                it.toMono()
            })
            .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient).compress(true))).build()

    private fun exchangeStrategies(): ExchangeStrategies {
        val objectMapper = createObjectMapper()

        return ExchangeStrategies
            .builder()
            .codecs {
                it.defaultCodecs().jackson2JsonDecoder(
                    Jackson2JsonDecoder(
                        objectMapper,
                        MediaType.valueOf("application/vnd.docker.distribution.manifest.v1+json"),
                        MediaType.valueOf("application/vnd.docker.distribution.manifest.v1+prettyjws"),
                        MediaType.valueOf("application/vnd.docker.distribution.manifest.v2+json"),
                        MediaType.valueOf("application/vnd.docker.container.image.v1+json"),
                        MediaType.valueOf("application/json")
                    )
                )
                it.defaultCodecs().jackson2JsonEncoder(
                    Jackson2JsonEncoder(
                        objectMapper,
                        MediaType.valueOf("application/json")
                    )
                )
            }
            .build()
    }

    @Bean
    fun tcpClient(
        @Value("\${sprocket.httpclient.readTimeout:5000}") readTimeout: Long,
        @Value("\${sprocket.httpclient.writeTimeout:5000}") writeTimeout: Long,
        @Value("\${sprocket.httpclient.connectTimeout:5000}") connectTimeout: Int,
        trustStore: KeyStore?
    ): TcpClient {
        val trustFactory = TrustManagerFactory.getInstance("X509")
        trustFactory.init(trustStore)

        val sslProvider = SslProvider.builder().sslContext(
            SslContextBuilder
                .forClient()
                .trustManager(trustFactory)
                .build()
        ).build()
        return TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
            .secure(sslProvider)
            .doOnConnected { connection ->
                connection
                    .addHandlerLast(ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
            }
    }

    @ConditionalOnMissingBean(KeyStore::class)
    @Bean
    fun localKeyStore(): KeyStore? = null

    @Profile("openshift")
    @Primary
    @Bean
    fun openshiftSSLContext(@Value("\${trust.store}") trustStoreLocation: String): KeyStore? =
        KeyStore.getInstance(KeyStore.getDefaultType())?.let { ks ->
            try {
                ks.load(FileInputStream(trustStoreLocation), "changeit".toCharArray())
                val fis = FileInputStream("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt")
                CertificateFactory.getInstance("X509").generateCertificates(fis).forEach {
                    ks.setCertificateEntry((it as X509Certificate).subjectX500Principal.name, it)
                }
                logger.debug("SSLContext successfully loaded")
            } catch (e: Exception) {
                logger.debug(e) { "SSLContext failed to load" }
                throw e
            }
            ks
        }
}
