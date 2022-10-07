package com.example.myweather

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

@Serializable
data class WeatherIndicatorsHourly(
    val main: WeatherBasicInfo,
    val weather: List<Weather>,
    val dt_txt: String
)

object WeatherIndicatorsListSerializer :
    JsonTransformingSerializer<List<WeatherIndicatorsHourly>>(ListSerializer(WeatherIndicatorsHourly.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}