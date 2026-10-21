package com.example.travelmate.domain.model

import com.example.travelmate.data.local.DestinationEntity
import com.example.travelmate.data.local.WeatherForecastEntity

data class DestinationDetailBundle(
    val destination: DestinationEntity,
    val weather: WeatherForecastEntity? = null,
    val nearbyPlaces: List<Destination> = emptyList()
)
