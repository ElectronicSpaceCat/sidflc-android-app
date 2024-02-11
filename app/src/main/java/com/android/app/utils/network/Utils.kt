package com.android.app.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object Utils {
    suspend fun httpGet(myURL: String?): String {
        val result = withContext(Dispatchers.IO) {
            val inputStream: InputStream

            try{
                // create URL
                val url = URL(myURL)
                // create HttpURLConnection
                val conn: HttpURLConnection = url.openConnection() as HttpURLConnection

                // make GET request to the given URL
                conn.connect()

                // receive response as inputStream
                inputStream = conn.inputStream
                convertInputStreamToString(inputStream)
            }
            catch (e : IOException) {
                "Did not work!"
            }
        }

        return result
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line:String? = bufferedReader.readLine()
        var result = ""

        while (line != null) {
            result += line
            line = bufferedReader.readLine()
        }

        inputStream.close()
        return result
    }

    fun download(link: String, path: String) {
        URL(link).openStream().use { input ->
            FileOutputStream(File(path)).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun download(link: String, path: String, progress: ((Long, Long) -> Unit)? = null): Long {
        val url = URL(link)
        val connection = url.openConnection()
        connection.connect()
        val length = connection.contentLengthLong
        url.openStream().use { input ->
            FileOutputStream(File(path)).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = input.read(buffer)
                var bytesCopied = 0L
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    progress?.invoke(bytesCopied, length)
                    bytesRead = input.read(buffer)
                    delay(1)
                }
                return bytesCopied
            }
        }
    }
}