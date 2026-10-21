package com.example.travelmate.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OpenMeteoResponse(
    @SerialName("current") val current: OpenMeteoCurrent? = null,
    @SerialName("daily") val daily: OpenMeteoDaily? = null
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0
)

@Serializable
data class OpenMeteoDaily(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList()
)

interface WeatherApiService {

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

data class WeatherUiData(
    val currentTemp: Int = 28,
    val weatherText: String = "Trời quang, nắng đẹp",
    val weatherIcon: String = "☀️",
    val travelAdvice: String = "Rất thích hợp cho các hoạt động ngoài trời",
    val forecastDays: List<ForecastDayData> = emptyList()
)

data class ForecastDayData(
    val dayName: String,
    val icon: String,
    val tempRange: String
)

fun parseWmoCode(code: Int): Pair<String, String> {
    return when (code) {
        0 -> Pair("☀️", "Nắng đẹp, trời quang")
        1, 2 -> Pair("⛅", "Trời trong, ít mây")
        3 -> Pair("☁️", "Trời nhiều mây")
        45, 48 -> Pair("🌫️", "Có sương mù nhẹ")
        51, 53, 55, 61, 63 -> Pair("🌧️", "Có mưa rào nhẹ")
        65, 80, 81, 82 -> Pair("🌦️", "Mưa rào dải rác")
        95, 96, 99 -> Pair("⛈️", "Mưa dông, bão")
        else -> Pair("🌤️", "Thời tiết mát mẻ")
    }
}

fun getTravelAdvice(code: Int): String {
    return when (code) {
        0, 1, 2 -> "Rất thích hợp cho các hoạt động tham quan ngoài trời!"
        3 -> "Nhiệt độ dễ chịu, thích hợp đi dạo và chụp ảnh."
        45, 48 -> "Nên mang theo áo khoác nhẹ khi di chuyển sáng sớm."
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Nên mang theo ô/dù và chuẩn bị kế hoạch tham quan trong nhà."
        95, 96, 99 -> "Nên hạn chế di chuyển xa và chú ý an toàn."
        else -> "Thời tiết lý tưởng để khám phá ẩm thực và văn hóa."
    }
}
