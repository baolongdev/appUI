package com.example.appui.data.remote.github.dto

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("body")
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("draft")
    val draft: Boolean = false,
    @SerializedName("prerelease")
    val prerelease: Boolean = false,
    @SerializedName("assets")
    val assets: List<AssetDto>
)

data class AssetDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("size")
    val size: Long,
    @SerializedName("content_type")
    val contentType: String
)
