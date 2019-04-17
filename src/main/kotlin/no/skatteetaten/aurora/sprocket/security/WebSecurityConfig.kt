package no.skatteetaten.aurora.sprocket.security

import no.skatteetaten.aurora.io.CachingRequestWrapper
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken



val NEXUS_SECURITY_HEADER = "x-nexus-webhook-signature"

// https://github.com/kpavlov/spring-hmac-rest/blob/master/src/main/java/com/github/kpavlov/restws/server/hmac/HmacAuthenticationFilter.java
@EnableWebSecurity
class WebSecurityConfig(
    val hmacUtils: HmacUtils
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {

        http.csrf().disable()

        http
            .addFilterBefore(SignatureHeaderFilter(hmacUtils), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeRequests()
            .antMatchers("/nexus/**").authenticated()
            .anyRequest().permitAll()
    }

}

class SignatureHeaderFilter(private val hmacUtils: HmacUtils) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val signature = request.getHeader(NEXUS_SECURITY_HEADER)
        if (signature == null) {
            logger.debug("signature missing, we do not run this filter")
            filterChain.doFilter(request, response)
            return
        }
        val requestWrapper = CachingRequestWrapper(request)
        val body = requestWrapper.contentAsByteArray

        val hmac = hmacUtils.hmacHex(body)

        if (signature != hmac) {
            throw BadCredentialsException("HmacAccessFilter.badSignature")
        }

        val authentication = PreAuthenticatedAuthenticationToken("nexus", null, listOf())

        SecurityContextHolder.getContext().authentication = authentication
        try {
            filterChain.doFilter(requestWrapper, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
        logger.debug("Authenticated")

        filterChain.doFilter(requestWrapper, response)
    }
}


