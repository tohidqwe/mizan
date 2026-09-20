package com.mizan.civilleitner.security

import android.os.Build
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

object AdminProvisioningClient {
    data class EnrollmentResult(
        val deviceId: String,
        val fingerprint: String,
    )

    fun enroll(
        baseUrl: String,
        enrollmentToken: String,
    ): EnrollmentResult {
        require(baseUrl.startsWith("https://")) { "Admin API must use HTTPS" }
        AdminDeviceKeyStore.ensureKey()

        val url = URL(baseUrl.trimEnd('/') + "/v1/admin/device/enroll")
        val body = JSONObject()
            .put("deviceName", Build.MANUFACTURER + " " + Build.MODEL)
            .put("publicKeyPem", AdminDeviceKeyStore.publicKeyPem())
            .put("enrollmentToken", enrollmentToken)
            .toString()

        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()

        if (code !in 200..299) {
            throw IllegalStateException("Enrollment failed: HTTP " + code + " " + text.take(300))
        }
        val json = JSONObject(text)
        return EnrollmentResult(
            deviceId = json.getString("deviceId"),
            fingerprint = json.getString("fingerprint"),
        )
    }
}
