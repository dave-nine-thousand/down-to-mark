package com.downtomark.data.model

data class Tag(
    val name: String,
    val usageCount: Int = 0,
    val color: String? = null
)
