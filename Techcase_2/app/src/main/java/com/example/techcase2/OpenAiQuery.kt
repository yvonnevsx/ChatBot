package com.example.techcase2

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.JsonParser // Import JsonParser from Gson

fun queryOpenAI(text: String, onResponse: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val apiKey =
            "APIKEY"
        val messages = """
            [
                {"role": "user", "content": "$text"}
            ]
        """.trimIndent()

        val postData = """
            {
              "model": "gpt-3.5-turbo",
              "messages": $messages,
              "max_tokens": 150
            }
        """.trimIndent()

        Log.d("OpenAiQuery", "postData: $postData")

        with(url.openConnection() as HttpURLConnection) {
            try {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(postData) }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonResponse = JsonParser.parseString(response).asJsonObject
                    val textResponse = jsonResponse
                        .getAsJsonArray("choices")
                        .first()
                        .asJsonObject
                        .get("message")
                        .asJsonObject
                        .get("content")
                        .asString

                    withContext(Dispatchers.Main) {
                        onResponse(textResponse) // Callback on the main thread with only the response text
                    }
                } else {
                    val errorResponse = errorStream.bufferedReader().use(BufferedReader::readText)
                    withContext(Dispatchers.Main) {
                        onResponse("Error: $responseCode - $errorResponse")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResponse("Error: ${e.message}")
                }
            } finally {
                disconnect()
            }
        }
    }
}
