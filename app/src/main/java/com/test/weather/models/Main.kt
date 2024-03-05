package com.test.weather.models

import java.io.Serializable

data class Main (
    val temp: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val pressure: Double,
    val humidity: Int,
    val seaLevel:Int,
    val grndLevel:Int,
):Serializable
