// data/remote/github/dto/GitHubReleaseDto.kt
package com.example.appui.data.remote.github.dto

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("body")
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("assets")
    val assets: List<AssetDto>,
    @SerializedName("prerelease")
    val prerelease: Boolean
)

data class AssetDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("size")
    val size: Long,
    @SerializedName("content_type")
    val contentType: String
)

data class UpdateJsonDto(
    @SerializedName("latestVersion")
    val latestVersion: String,
    @SerializedName("latestVersionCode")
    val latestVersionCode: Int,
    @SerializedName("downloadUrl")
    val downloadUrl: String,
    @SerializedName("releaseNotes")
    val releaseNotes: String,
    @SerializedName("mandatory")
    val mandatory: Boolean,
    @SerializedName("releasedAt")
    val releasedAt: String
)
