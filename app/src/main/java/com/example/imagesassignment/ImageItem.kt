package com.example.imagesassignment

data class ImageItem(

    var id: String? = null,
    var title: String? = null,
    var language: String? = null,
    var thumbnail: Thumbnail? = Thumbnail(),
    var mediaType: Int? = null,
    var coverageURL: String? = null,
    var publishedAt: String? = null,
    var publishedBy: String? = null

)

data class Thumbnail(

    var id: String? = null,
    var version: Int? = null,
    var domain: String? = null,
    var basePath: String? = null,
    var key: String? = null,
    var qualities: ArrayList<Int> = arrayListOf(),
    var aspectRatio: Float? = null

)

data class ListImageItem(
    var imgItemList:List<ImageItem>
)