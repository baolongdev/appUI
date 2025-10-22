package com.example.appui.data.mapper

import com.example.appui.data.remote.github.dto.AssetDto
import com.example.appui.data.remote.github.dto.GitHubReleaseDto
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.ReleaseAsset
import com.example.appui.domain.model.UpdateInfo

object GitHubMapper {

    fun toAppRelease(dto: GitHubReleaseDto): AppRelease {
        return AppRelease(
            id = dto.id,
            tagName = dto.tagName,
            name = dto.name ?: dto.tagName,
            body = dto.body ?: "",
            publishedAt = dto.publishedAt,
            isDraft = dto.draft,
            isPrerelease = dto.prerelease,
            assets = dto.assets.map { toReleaseAsset(it) }
        )
    }

    fun toUpdateInfo(dto: GitHubReleaseDto, currentVersionCode: Int): UpdateInfo {
        val latestVersionCode = parseVersionCode(dto.tagName)
        val isNewer = latestVersionCode > currentVersionCode

        val apkAsset = dto.assets.firstOrNull { it.name.endsWith(".apk") }

        return UpdateInfo(
            version = dto.tagName.removePrefix("v"),
            versionCode = latestVersionCode,
            releaseNotes = dto.body ?: "Không có thông tin cập nhật",
            downloadUrl = apkAsset?.browserDownloadUrl ?: "",
            fileSize = apkAsset?.size ?: 0L,
            isNewer = isNewer,
            publishedAt = dto.publishedAt,
            mandatory = false
        )
    }

    private fun toReleaseAsset(dto: AssetDto): ReleaseAsset {
        return ReleaseAsset(
            id = dto.id,
            name = dto.name,
            downloadUrl = dto.browserDownloadUrl,
            size = dto.size,
            contentType = dto.contentType
        )
    }

    private fun parseVersionCode(tagName: String): Int {
        val version = tagName.removePrefix("v")
        val parts = version.split(".")

        return try {
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            0
        }
    }
}
