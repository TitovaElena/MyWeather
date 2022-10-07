package com.example.myweather

import kotlinx.serialization.*

@Serializable
data class WeatherData(
    @Serializable(with = WeatherIndicatorsListSerializer::class)
    val list: List<WeatherIndicatorsHourly>
)

