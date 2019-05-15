package no.skatteetaten.aurora.io

import java.io.ByteArrayInputStream
import java.io.IOException

import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

import org.apache.commons.io.IOUtils

class CachingRequestWrapper
/**
 * Create a new CachingRequestWrapper for the given servlet request.
 *
 * @param request the original servlet request
 */
@Throws(IOException::class)
constructor(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private var payload: ByteArray? = null

    val contentAsByteArray: ByteArray?
        @Throws(IOException::class)
        get() = getPayload()

    @Throws(IOException::class)
    private fun getPayload(): ByteArray? {
        if (payload == null) {
            payload = IOUtils.toByteArray(request.inputStream)
        }
        return payload
    }

    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        val byteArrayInputStream = ByteArrayInputStream(getPayload()!!)
        return object : ServletInputStream() {

            @Throws(IOException::class)
            override fun read(): Int {
                return byteArrayInputStream.read()
            }

            override fun isFinished(): Boolean {
                return false
            }

            override fun isReady(): Boolean {
                return true
            }

            override fun setReadListener(listener: ReadListener) {
                //
            }
        }
    }
}
