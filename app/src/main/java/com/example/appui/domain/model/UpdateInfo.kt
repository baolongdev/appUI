// domain/model/UpdateInfo.kt
package com.example.appui.domain.model

import java.time.Instant

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val releasedAt: Instant,
    val mandatory: Boolean,
    val fileSize: Long = 0L,
    val isNewer: Boolean = false
)

data class AppRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val contentType: String
)
