package com.example.radarcamera.radar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class RadarStatus(val eventId: Long = 0, val mph: Double = 0.0)

sealed interface RadarReadResult {
    data class Success(val status: RadarStatus) : RadarReadResult
    data object Timeout : RadarReadResult
    data class NetworkError(val exceptionType: String) : RadarReadResult
    data class HttpError(val code: Int) : RadarReadResult
    data object ParseError : RadarReadResult
}

class RadarStatusClient {
    suspend fun read(): RadarReadResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("http://192.168.4.1/status").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 800; readTimeout = 800; useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) return@withContext RadarReadResult.HttpError(responseCode)
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            RadarReadResult.Success(RadarStatus(json.optLong("eventoLive", 0), json.optDouble("velocidadLive", 0.0)))
        } catch (_: SocketTimeoutException) {
            RadarReadResult.Timeout
        } catch (error: IOException) {
            RadarReadResult.NetworkError(error::class.java.simpleName)
        } catch (_: JSONException) {
            RadarReadResult.ParseError
        } catch (error: Exception) {
            RadarReadResult.NetworkError(error::class.java.simpleName)
        } finally { connection?.disconnect() }
    }
}
