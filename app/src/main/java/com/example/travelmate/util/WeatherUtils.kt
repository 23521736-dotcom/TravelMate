package com.example.travelmate.util

data class WeatherInfo(
    val icon: String,
    val description: String,
    val travelAdvice: String
)

object WeatherUtils {

    fun parseWmoCode(code: Int): WeatherInfo {
        return when (code) {
            0 -> WeatherInfo(
                icon = "☀️",
                description = "Nắng ráo, trời quang",
                travelAdvice = "Rất thích hợp cho các hoạt động tham quan ngoài trời!"
            )
            1, 2 -> WeatherInfo(
                icon = "⛅",
                description = "Mây rải rác, trong lành",
                travelAdvice = "Thời tiết đẹp, thích hợp chụp ảnh và di chuyển."
            )
            3 -> WeatherInfo(
                icon = "☁️",
                description = "Trời nhiều mây",
                travelAdvice = "Nhiệt độ mát mẻ, dễ chịu cho chuyến tham quan."
            )
            45, 48 -> WeatherInfo(
                icon = "🌫️",
                description = "Có sương mù nhẹ",
                travelAdvice = "Nên chú ý quan sát khi lái xe vào buổi sáng sớm."
            )
            51, 53, 55, 61, 63 -> WeatherInfo(
                icon = "🌧️",
                description = "Mưa phùn / Mưa nhẹ",
                travelAdvice = "Nên mang theo ô/dù nhẹ hoặc áo mưa mỏng."
            )
            65, 80, 81, 82 -> WeatherInfo(
                icon = "🌦️",
                description = "Mưa rào dải rác",
                travelAdvice = "Nên chuẩn bị kế hoạch tham quan các điểm trong nhà."
            )
            95, 96, 99 -> WeatherInfo(
                icon = "⛈️",
                description = "Dông bão, mưa lớn",
                travelAdvice = "Nên hạn chế di chuyển ngoài trời và chú ý an toàn."
            )
            else -> WeatherInfo(
                icon = "🌤️",
                description = "Thời tiết mát mẻ",
                travelAdvice = "Thích hợp cho các hoạt động khám phá văn hóa & ẩm thực."
            )
        }
    }
}
