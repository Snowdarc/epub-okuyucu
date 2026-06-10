package com.seyitali.epubreader.epub

data class Chapter(
    val title: String,
    val html: String
)

data class EpubBook(
    val title: String,
    val author: String,
    val chapters: List<Chapter>
)
