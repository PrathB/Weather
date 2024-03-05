package com.test.weather.models

import java.io.Serializable

data class WeatherResponse(
    val coord : Coord,
    val weather : List<Weather>,
    val base: String,
    val main: Main,
    val visibility: String,
    val wind: Wind,
    val rain: Rain,
    val snow: Snow,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
) : Serializable