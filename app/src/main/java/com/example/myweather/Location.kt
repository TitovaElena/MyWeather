package com.example.myweather

import kotlinx.serialization.Serializable

@Serializable
data class Location(val lat: Float, val lon: Float) {

}