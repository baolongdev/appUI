package com.example.appui.domain.model

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
    val mandatory: Boolean = false,
    val fileSize: Long = 0L,
    val isNewer: Boolean = false
)

data class AppRelease(
    val id: Long,  // ✅ ADDED: GitHub release ID
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val isDraft: Boolean = false,
    val isPrerelease: Boolean = false,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val id: Long,  // ✅ ADDED: Asset ID
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val contentType: String
)
