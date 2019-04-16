package no.skatteetaten.aurora.sprocket.security

import mu.KotlinLogging
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val logger = KotlinLogging.logger {}

@EnableWebSecurity
class WebSecurityConfig(
    @Value("\${management.server.port}") val managementPort: Int,
    @Value("\${sprocket.nexus.secret:123456}") val secretKey: String
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {

        http.csrf().disable()

        http
            .addFilterBefore(SignatureHeaderFilter(secretKey), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeRequests()
            .requestMatchers(forPort(managementPort)).permitAll()
    }

    private fun forPort(port: Int) = RequestMatcher { request: HttpServletRequest -> port == request.localPort }
}

class SignatureHeaderFilter(val secretKey: String) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val signature = request.getHeader("x-nexus-webhook-signature") ?: return

        val requestWrapper = ContentCachingRequestWrapper(request)
        try {
            filterChain.doFilter(requestWrapper, response)
        } finally {
            val body = requestWrapper.inputStream

            val hmac = HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKey).hmacHex(body)

            if (signature != hmac) {
                response.sendError(403, "Forbidden operation. HMAC mismatch")
                return
            }
        }
    }
}