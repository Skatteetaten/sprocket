package no.skatteetaten.aurora.sprocket.security

import no.skatteetaten.aurora.io.CachingRequestWrapper
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val NEXUS_SECURITY_HEADER = "x-nexus-webhook-signature"
val NEXUS_AUTHORITY = "NEXUS"

// https://github.com/kpavlov/spring-hmac-rest/blob/master/src/main/java/com/github/kpavlov/restws/server/hmac/HmacAuthenticationFilter.java
@EnableWebSecurity
class WebSecurityConfig(
    @Value("\${sprocket.nexus.secret:123456}") val secretKey: String
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {

        http.csrf().disable()

        http
            .addFilterBefore(NexusWebhookSignatureFilter(secretKey), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeRequests()
            .antMatchers("/nexus/**").hasAuthority(NEXUS_AUTHORITY)
            .anyRequest().permitAll()
    }
}

class NexusWebhookSignatureFilter(private val secretKey: String) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val signature = request.getHeader(NEXUS_SECURITY_HEADER)
        if (signature == null) {
            // logger.debug("signature missing, we do not run this filter ${request.requestURI}")
            filterChain.doFilter(request, response)
            return
        }
        val requestWrapper = CachingRequestWrapper(request)
        val body = requestWrapper.contentAsByteArray

        val hmacUtils = HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKey)
        val hmac = hmacUtils.hmacHex(body)
        val stringBody = body?.let { String(it) } ?: ""
        val hmac2 = hmacUtils.hmacHex(stringBody)
        if (signature == hmac) {
            val authentication =
                PreAuthenticatedAuthenticationToken("nexus", signature, listOf(SimpleGrantedAuthority(NEXUS_AUTHORITY)))
            SecurityContextHolder.getContext().authentication = authentication
        } else {
            logger.warn("signature and hmac does not match body=$stringBody signature=$signature hmac=$hmac hmac2=$hmac2")
        }
        // If signature does not match we do not have a valid user.

        try {
            filterChain.doFilter(requestWrapper, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
