package com.example.myweather

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class WeatherBasicInfo(val temp: Float, val temp_max: Float, val temp_min: Float)

