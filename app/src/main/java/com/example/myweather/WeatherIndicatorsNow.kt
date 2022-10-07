package com.example.myweather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherIndicatorsNow(
    val main: WeatherBasicInfo,
    val weather: List<Weather>
)