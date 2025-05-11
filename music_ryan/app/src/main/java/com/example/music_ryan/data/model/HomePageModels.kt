package com.example.music_ryan.data.model

data class HomePageResponse(
    val code: Int,
    val msg: String,
    val data: HomePageData
)

data class HomePageData(
    val records: List<ModuleConfig>,
    val total: Int,
    val size: Int,
    val current: Int,
    val pages: Int
)

data class ModuleConfig(
    val moduleConfigId: Int,
    val moduleName: String,
    val style: Int,
    val musicInfoList: List<Song>
) 