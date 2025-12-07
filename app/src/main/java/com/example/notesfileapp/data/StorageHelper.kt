package com.example.notesfileapp.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object StorageHelper {
    // ---------- Internal storage (app-specific) ----------
    suspend fun writeInternal(context: Context, fileName: String, content: String)
            = withContext(Dispatchers.IO) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
            out.write(content.toByteArray())
        }
    }
    suspend fun readInternal(context: Context, fileName: String): String? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                context.openFileInput(fileName).use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
            } catch (e: Exception) {
                null
            }
        }
    suspend fun deleteInternal(context: Context, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext context.deleteFile(fileName)
        }
    // ---------- External shared via SAF ----------
    suspend fun writeExternalSAF(contentResolver: ContentResolver, uri: Uri,
                                 content: String) = withContext(Dispatchers.IO) {
        contentResolver.openOutputStream(uri, "rwt").use { out ->
            out?.write(content.toByteArray())
        }
    }
    suspend fun readExternalSAF(contentResolver: ContentResolver, uri: Uri):
            String? = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) null else
                    BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (e: Exception) {
            null
        }
    }
    suspend fun deleteExternalSAF(contentResolver: ContentResolver, uri: Uri):
            Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (e: Exception) {
            false
        }
    }
}