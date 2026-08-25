package com.example.epubreader.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class HardcoverBook(
    val id: Int,
    val title: String,
    val author: String = "Unknown Author"
)

/**
 * Client for interacting with the Hardcover GraphQL API.
 */
class HardcoverApiClient(private val token: String) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val endpoint = "https://api.hardcover.app/v1/graphql"

    /**
     * Searches for books by title on Hardcover and returns a list of matches.
     * Throws an exception with the error message if something fails.
     *
     * @param query Title of the book to search.
     * @return List of HardcoverBook.
     */
    suspend fun searchBooks(query: String): List<HardcoverBook> = withContext(Dispatchers.IO) {
        val graphqlQuery = """
            query searchBooks(${'$'}title: String!) {
                books(where: {title: {_ilike: ${'$'}title}}, limit: 15, order_by: {users_count: desc}) {
                    id
                    title
                    contributions {
                        author {
                            name
                        }
                    }
                }
            }
        """.trimIndent()

        // Replace spaces and punctuation with % to allow fuzzy matching
        val fuzzyTitle = "%" + query.replace(Regex("[^a-zA-Z0-9]+"), "%") + "%"
        val variables = buildJsonObject {
            put("title", fuzzyTitle)
        }

        val requestBody = buildJsonObject {
            put("query", graphqlQuery)
            put("variables", variables)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${token}")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val bodyStr = response.body?.string() ?: throw Exception("Empty response body")
            
            val root = json.parseToJsonElement(bodyStr).jsonObject
            if (root.containsKey("errors")) {
                val errors = root["errors"]?.toString()
                throw Exception("GraphQL Error: $errors")
            }

            val data = root["data"]?.jsonObject
            val booksArray = data?.get("books") as? kotlinx.serialization.json.JsonArray ?: return@withContext emptyList()
            
            val result = mutableListOf<HardcoverBook>()
            for (item in booksArray) {
                val bookObj = item.jsonObject
                val id = bookObj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
                val title = bookObj["title"]?.jsonPrimitive?.content ?: "Unknown Title"
                
                var authorName = "Unknown Author"
                val contributions = bookObj["contributions"] as? kotlinx.serialization.json.JsonArray
                if (contributions != null && contributions.isNotEmpty()) {
                    val firstContrib = contributions[0].jsonObject
                    val authorObj = firstContrib["author"]?.jsonObject
                    val name = authorObj?.get("name")?.jsonPrimitive?.content
                    if (!name.isNullOrBlank()) {
                        authorName = name
                    }
                }
                
                result.add(HardcoverBook(id, title, authorName))
            }
            return@withContext result
        }
    }

    /**
     * Adds or updates a book in the user's Hardcover list.
     *
     * @param bookId Hardcover Book ID.
     * @param statusId Reading status ID (e.g., 1=Want to Read, 2=Currently Reading, 3=Read).
     * @param rating Rating from 1 to 5, or null if not rated.
     * @return True if successful, false otherwise.
     */
    suspend fun updateUserBook(bookId: Int, statusId: Int, rating: Float?): Boolean = withContext(Dispatchers.IO) {
        // We use insert_user_books_one to add/update user book entry
        val mutation = """
            mutation updateUserBook(${'$'}book_id: Int!, ${'$'}status_id: Int, ${'$'}rating: numeric) {
                insert_user_books_one(
                    object: { book_id: ${'$'}book_id, status_id: ${'$'}status_id, rating: ${'$'}rating },
                    on_conflict: { constraint: user_books_book_id_user_id_key, update_columns: [status_id, rating] }
                ) {
                    id
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("book_id", bookId)
            put("status_id", statusId)
            if (rating != null) {
                put("rating", rating)
            }
        }

        val requestBody = buildJsonObject {
            put("query", mutation)
            put("variables", variables)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${token}")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bodyStr = response.body?.string() ?: return@withContext false
                
                val root = json.parseToJsonElement(bodyStr).jsonObject
                if (root.containsKey("errors")) {
                    println("Hardcover GraphQL Errors: ${root["errors"]}")
                    return@withContext false
                }
                
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
