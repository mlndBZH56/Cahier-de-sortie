package com.aca56.cahiersortiecodex.data.local.entity

private const val RemarkPhotoSeparator = "\n||\n"

fun decodeRemarkPhotoPaths(photoPath: String?): List<String> {
    return photoPath
        ?.split(RemarkPhotoSeparator)
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
}

fun encodeRemarkPhotoPaths(photoPaths: List<String>): String? {
    val normalized = photoPaths
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    return normalized.takeIf { it.isNotEmpty() }?.joinToString(RemarkPhotoSeparator)
}
