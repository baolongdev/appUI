// data/remote/github/GitHubApiService.kt
package com.example.appui.data.remote.github

import com.example.appui.data.remote.github.dto.GitHubReleaseDto
import com.example.appui.data.remote.github.dto.UpdateJsonDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface GitHubApiService {

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubReleaseDto>>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubReleaseDto>

    @GET
    suspend fun getUpdateJson(@Url url: String): Response<UpdateJsonDto>
}
