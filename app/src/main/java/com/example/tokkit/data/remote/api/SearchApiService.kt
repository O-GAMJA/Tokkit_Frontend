package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {
    /**
     * 풀텍스트 검색 API
     * 노트 내용과 노트 제목에서 주어진 키워드를 검색합니다.
     *
     * @param text 검색할 텍스트 (비어있지 않은 텍스트)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 5, 최대: 100)
     */
    @GET("/search/fullText")
    suspend fun searchFullText(
        @Query("text") text: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 5
    ): ApiResponse<SearchResponse>
}