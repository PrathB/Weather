package com.test.weather.models

import java.io.Serializable

data class Wind (
    val speed: Double,
    val degree: Int,
    val gust: Double
):Serializable
