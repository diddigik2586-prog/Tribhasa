package org.tribhasa.sodhamitra.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tribhasa.sodhamitra.R

object Feedback {
    const val EMAIL = "dvkrishna92@gmail.com"
    const val SUBJECT = "Śodhamitra App Feedback"

    /** A pre-addressed draft: recipient and subject are filled in; the user writes, reviews and sends it. Never sent automatically. */
    fun draftIntent(): Intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$EMAIL?subject=${Uri.encode(SUBJECT)}")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, SUBJECT)
    }

    /** true when an email app took the draft; false when this device has no email app. */
    fun open(context: Context): Boolean = try {
        context.startActivity(draftIntent()); true
    } catch (e: ActivityNotFoundException) { false }
}

object Clip {
    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

object ProfilePhotos {
    /** Copies the chosen photograph into the app's private storage (scaled down, JPEG). Returns the new file name. */
    suspend fun import(context: Context, dir: File, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { dec, info, _ ->
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > 1024) dec.setTargetSampleSize((longest / 1024).coerceAtLeast(1))
                    dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            }
            dir.mkdirs()
            val name = "profile_photo_${System.currentTimeMillis()}.jpg"
            File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        } catch (e: Exception) { null }
    }

    fun delete(dir: File, name: String) { if (name.isNotBlank()) File(dir, name).delete() }

    fun load(dir: File, name: String): Bitmap? =
        if (name.isBlank()) null else File(dir, name).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
}

object PdfFiles {
    fun exportDir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    fun viewIntent(context: Context, file: File): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriFor(context, file), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun shareIntent(context: Context, file: File): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        context.getString(R.string.share),
    )
}
