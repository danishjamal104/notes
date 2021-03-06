package com.github.danishjamal104.notes.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File

object FileUtil {

    private fun Uri.isSchemeTypeFile(): Boolean = "file".equals(this.scheme!!, ignoreCase = true)
    private fun Uri.isSchemeTypeContent(): Boolean = "content".equals(this.scheme!!, ignoreCase = true)

    fun getPathFromLocalUri(context: Context, uri: Uri): String? {
        val path: String? = try {
            _getPathFromLocalUri(context, uri)
        } catch (exp: CursorIndexOutOfBoundsException) {
            exp.printStackTrace()
            uri.path
        } catch (exp: NullPointerException) {
            exp.printStackTrace()
            uri.path
        } catch (exp: NumberFormatException) {
            exp.printStackTrace()
            uri.path
        }
        return path?.let {
            if (File(it).exists()) {
                path
            } else {
                null
            }
        }
    }

    private fun _getPathFromLocalUri(context: Context, uri: Uri): String? {
        // DocumentProvider
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                // ExternalStorageProvider
                when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split =
                            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        // This is for checking Main Memory
                        if ("primary".equals(type, ignoreCase = true)) {
                            return if (split.size > 1) {
                                context.getExternalFilesDir(null).toString() + "/" + split[1]
                            } else {
                                context.getExternalFilesDir(null).toString() + "/"
                            }
                            // This is for checking SD Card
                        } /*else {
                            val path = "storage" + "/" + docId.replace(":", "/")
                            if (File(path).exists()) {
                                path
                            } else {
                                "/storage/sdcard/" + split[1]
                            }
                        }*/
                    }
                    isDownloadsDocument(uri) -> {
                        val fileName = getFilePath(context, uri)
                        if (fileName != null) {
                            val path = context.getExternalFilesDir(null)
                                .toString() + "/Download/" + fileName
                            if (File(path).exists()) {
                                return path
                            }
                        }

                        var id = DocumentsContract.getDocumentId(uri)
                        if (id.contains(":")) {
                            id = id.split(":")[1]
                        }
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            java.lang.Long.valueOf(id)
                        )
                        return getDataColumn(context, contentUri, null, null)
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split =
                            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        val contentUri: Uri? = getContentUri(type)

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                } // MediaProvider
                // DownloadsProvider
            }
            uri.isSchemeTypeContent() -> {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            }
            uri.isSchemeTypeFile() -> {
                return uri.path
            }
        } // File
        return null
    }

    /**
     * checks the type of uri
     */
    private fun getContentUri(type: String): Uri? {
        when (type) {
            "image" -> return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> return MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } catch (ex: Exception) {
            Log.e("result", ""+ex.localizedMessage)
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getFilePath(context: Context, uri: Uri): String? {

        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}