package com.example.travelmate.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val displayName: String, val icon: String) {
    BEACH("Beach", "🏖️"),
    CULTURE("Culture", "🏛️"),
    NATURE("Nature", "🌿"),
    FOOD("Street Food", "🍜"),
    PHOTOGRAPHY("Photography", "📷"),
    BUDGET("Budget Travel", "💰"),
    ADVENTURE("Adventure", "🌋"),
    SHOPPING("Shopping", "🛍️"),
    NIGHTLIFE("Nightlife", "💃"),
    HISTORY("History", "📜"),
    RELAXATION("Relaxation", "🛀")
}
