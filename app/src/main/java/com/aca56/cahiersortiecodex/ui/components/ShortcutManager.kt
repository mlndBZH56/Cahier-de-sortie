package com.aca56.cahiersortiecodex.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.aca56.cahiersortiecodex.MainActivity
import com.aca56.cahiersortiecodex.R

fun createPinnedShortcutWithIcon(context: Context, imageUri: Uri) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        return
    }

    val inputStream = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
    
    // On redimensionne un peu pour éviter des icônes trop lourdes
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
    }

    val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "custom_launcher_icon")
        .setShortLabel(context.getString(R.string.app_name))
        .setIcon(IconCompat.createWithBitmap(scaledBitmap))
        .setIntent(intent)
        .build()

    ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
}
