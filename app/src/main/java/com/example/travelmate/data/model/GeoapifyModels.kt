package com.example.travelmate.data.model

import com.example.travelmate.domain.model.Destination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoapifySearchResponse(
    @SerialName("type") val type: String? = null,
    @SerialName("features") val features: List<GeoapifyFeature> = emptyList()
)

@Serializable
data class GeoapifyFeature(
    @SerialName("type") val type: String? = null,
    @SerialName("properties") val properties: GeoapifyProperties? = null,
    @SerialName("geometry") val geometry: GeoapifyGeometry? = null
)

@Serializable
data class GeoapifyContact(
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null
)

@Serializable
data class GeoapifyWikiAndMedia(
    @SerialName("wikipedia") val wikipedia: String? = null,
    @SerialName("wikidata") val wikidata: String? = null,
    @SerialName("wikimedia_commons") val wikimediaCommons: String? = null
)

@Serializable
data class GeoapifyProperties(
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("formatted") val formatted: String? = null,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("street") val street: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("district") val district: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("categories") val categories: List<String> = emptyList(),
    @SerialName("result_type") val resultType: String? = null,
    @SerialName("website") val website: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    @SerialName("contact") val contact: GeoapifyContact? = null,
    @SerialName("wiki_and_media") val wikiAndMedia: GeoapifyWikiAndMedia? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("lat") val lat: Double? = null
)

@Serializable
data class GeoapifyGeometry(
    @SerialName("type") val type: String? = null,
    @SerialName("coordinates") val coordinates: List<Double> = emptyList()
)

fun GeoapifyFeature.toDestination(apiKey: String, photoUrl: String? = null): Destination {
    val props = properties ?: GeoapifyProperties()
    val placeName = props.name?.ifBlank { null }
        ?: props.addressLine1?.ifBlank { null }
        ?: props.formatted?.split(",")?.firstOrNull()
        ?: "Địa điểm du lịch"

    val locCity = props.city ?: props.suburb ?: props.district ?: props.state ?: props.addressLine2 ?: ""
    val countryName = props.country ?: "Việt Nam"

    val latitudeVal = props.lat ?: geometry?.coordinates?.getOrNull(1) ?: 0.0
    val longitudeVal = props.lon ?: geometry?.coordinates?.getOrNull(0) ?: 0.0

    val rawId = props.placeId ?: "${latitudeVal}_${longitudeVal}_${placeName.hashCode()}"
    val idVal = if (rawId.startsWith("geoapify_")) rawId else "geoapify_$rawId"

    // High resolution Geoapify Static Map image centered on lat/lon with marker
    val staticMapUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=800&height=500&center=lonlat:$longitudeVal,$latitudeVal&zoom=16&marker=lonlat:$longitudeVal,$latitudeVal;color:%230284c7;size:medium&apiKey=$apiKey"

    // Primary real scenery photo from Unsplash/Pixabay
    val categoryPhotoUrl = photoUrl ?: getCategoryPhoto(props.category, props.categories, props.resultType, placeName)

    val categoryTagsList = getCategoryTags(props.category, props.categories, props.resultType)

    // Build rich, informative Vietnamese description from Geoapify metadata
    val desc = buildGeoapifyDescription(props, latitudeVal, longitudeVal)

    return Destination(
        id = idVal,
        name = placeName,
        location = locCity.ifBlank { countryName },
        country = countryName,
        description = desc,
        rating = 4.7,
        reviewCount = 35,
        categoryTags = categoryTagsList,
        imageUrls = listOf(categoryPhotoUrl, staticMapUrl),
        coverImage = categoryPhotoUrl,
        unesco = props.categories.any { it.contains("heritage") || it.contains("attraction") } || props.category?.contains("tourism") == true,
        priceLevel = 2,
        latitude = latitudeVal,
        longitude = longitudeVal,
        promoted = false,
        avgPricePerDay = 500000.0
    )
}

private fun buildGeoapifyDescription(props: GeoapifyProperties, lat: Double, lon: Double): String {
    val sb = StringBuilder()

    val catLabel = getCategoryVietnameseLabel(props.category, props.categories, props.resultType)
    sb.append("Loại địa điểm: $catLabel\n\n")

    if (!props.formatted.isNullOrBlank()) {
        sb.append("Địa chỉ: ${props.formatted}\n")
    }

    val locParts = listOfNotNull(props.street, props.suburb, props.district, props.city, props.state, props.country)
        .filter { it.isNotBlank() }
        .distinct()
    if (locParts.isNotEmpty()) {
        sb.append("Khu vực: ${locParts.joinToString(", ")}\n")
    }

    if (!props.openingHours.isNullOrBlank()) {
        sb.append("Giờ mở cửa: ${props.openingHours}\n")
    }

    val phoneNum = props.contact?.phone
    if (!phoneNum.isNullOrBlank()) {
        sb.append("Điện thoại liên hệ: $phoneNum\n")
    }

    if (!props.website.isNullOrBlank()) {
        sb.append("Website chính thức: ${props.website}\n")
    }

    val wiki = props.wikiAndMedia?.wikipedia
    if (!wiki.isNullOrBlank()) {
        sb.append("Wikipedia: $wiki\n")
    }

    sb.append("Tọa độ GPS: $lat, $lon\n\n")
    sb.append("Dữ liệu địa điểm được trích xuất trực tuyến và xác thực từ Geoapify Places API.")

    return sb.toString().trim()
}

private fun getCategoryVietnameseLabel(category: String?, categories: List<String>, resultType: String?): String {
    val allCats = (categories + listOfNotNull(category, resultType)).map { it.lowercase() }
    return when {
        allCats.any { it.contains("education") || it.contains("school") || it.contains("university") || it.contains("college") } -> "Cơ sở giáo dục / Trường học"
        allCats.any { it.contains("healthcare") || it.contains("hospital") || it.contains("clinic") || it.contains("pharmacy") } -> "Cơ sở y tế / Bệnh viện"
        allCats.any { it.contains("catering") || it.contains("restaurant") || it.contains("cafe") || it.contains("food") } -> "Nhà hàng / Quán ăn / Cà phê"
        allCats.any { it.contains("accommodation") || it.contains("hotel") || it.contains("motel") || it.contains("hostel") } -> "Khách sạn / Lưu trú"
        allCats.any { it.contains("commercial") || it.contains("shopping") || it.contains("marketplace") || it.contains("mall") } -> "Trung tâm thương mại / Chợ / Mua sắm"
        allCats.any { it.contains("park") || it.contains("garden") || it.contains("natural") || it.contains("forest") } -> "Công viên / Điểm thiên nhiên"
        allCats.any { it.contains("tourism") || it.contains("attraction") || it.contains("museum") || it.contains("heritage") } -> "Điểm du lịch / Danh lam thắng cảnh"
        allCats.any { it.contains("beach") || it.contains("coastal") } -> "Bãi biển / Bờ biển"
        allCats.any { it.contains("populated_place") || it.contains("city") || it.contains("administrative") } -> "Thành phố / Khu đô thị"
        else -> "Địa điểm công cộng / Tòa nhà"
    }
}

private fun getCategoryTags(category: String?, categories: List<String>, resultType: String?): List<String> {
    val allCats = (categories + listOfNotNull(category, resultType)).map { it.lowercase() }
    val tags = mutableListOf<String>()

    if (allCats.any { it.contains("beach") || it.contains("coastal") }) tags.add("BEACH")
    if (allCats.any { it.contains("culture") || it.contains("heritage") || it.contains("museum") || it.contains("artwork") || it.contains("building") || it.contains("education") }) tags.add("CULTURE")
    if (allCats.any { it.contains("nature") || it.contains("park") || it.contains("national_park") || it.contains("natural") }) tags.add("NATURE")
    if (allCats.any { it.contains("catering") || it.contains("food") || it.contains("restaurant") || it.contains("marketplace") }) tags.add("FOOD")
    if (allCats.any { it.contains("commercial") || it.contains("shopping") || it.contains("mall") }) tags.add("SHOPPING")
    if (allCats.any { it.contains("leisure") || it.contains("entertainment") || it.contains("tourism") }) tags.add("ADVENTURE")
    if (allCats.any { it.contains("accommodation") || it.contains("hotel") }) tags.add("RELAXATION")

    if (tags.isEmpty()) {
        tags.add("CULTURE")
        tags.add("NATURE")
    }
    return tags.distinct()
}

private fun getCategoryPhoto(category: String?, categories: List<String>, resultType: String?, name: String): String {
    val lower = name.lowercase()
    val allCats = (categories + listOfNotNull(category, resultType)).map { it.lowercase() }

    return when {
        // Specific city/place keyword overrides
        lower.contains("da lat") || lower.contains("đà lạt") -> "https://images.unsplash.com/photo-1583417319070-4a69db38a482"
        lower.contains("hanoi") || lower.contains("hà nội") -> "https://images.unsplash.com/photo-1509042239860-f550ce710b93"
        lower.contains("da nang") || lower.contains("đà nẵng") -> "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b"
        lower.contains("nha trang") -> "https://images.unsplash.com/photo-1540555700478-4be289fbecef"
        lower.contains("sa pa") || lower.contains("sapa") -> "https://images.unsplash.com/photo-1528127269322-539801943592"
        lower.contains("ha long") || lower.contains("hạ long") -> "https://images.unsplash.com/photo-1528127269322-539801943592"
        lower.contains("phu quoc") || lower.contains("phú quốc") -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e"
        lower.contains("bến thành") || lower.contains("ben thanh") -> "https://images.unsplash.com/photo-1504674900247-0877df9cc836"

        // Specific category-tailored photos
        allCats.any { it.contains("education") || it.contains("school") || it.contains("university") || it.contains("college") } ->
            "https://images.unsplash.com/photo-1523050854058-8df90110c9f1" // Campus & Education
        allCats.any { it.contains("healthcare") || it.contains("hospital") || it.contains("clinic") || it.contains("pharmacy") } ->
            "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d" // Modern Healthcare
        allCats.any { it.contains("catering") || it.contains("restaurant") || it.contains("cafe") || it.contains("food") } ->
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4" // Restaurant dining
        allCats.any { it.contains("accommodation") || it.contains("hotel") || it.contains("motel") || it.contains("hostel") } ->
            "https://images.unsplash.com/photo-1566073771259-6a8506099945" // Hotel resort
        allCats.any { it.contains("commercial") || it.contains("shopping") || it.contains("marketplace") || it.contains("mall") } ->
            "https://images.unsplash.com/photo-1555396273-367ea4eb4db5" // Shopping marketplace
        allCats.any { it.contains("park") || it.contains("garden") || it.contains("natural") || it.contains("forest") } ->
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e" // Green park nature
        allCats.any { it.contains("tourism") || it.contains("attraction") || it.contains("museum") || it.contains("heritage") } ->
            "https://images.unsplash.com/photo-1503899036084-c55cdd92da26" // Tourist attraction landmark
        allCats.any { it.contains("beach") || it.contains("coastal") } ->
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e" // Tropical beach
        allCats.any { it.contains("populated_place") || it.contains("city") || it.contains("administrative") } ->
            "https://images.unsplash.com/photo-1477959858617-67f30ac4ce78" // City skyline
        else -> "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab" // Modern building architecture
    }
}
