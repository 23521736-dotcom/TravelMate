package com.example.travelmate.util

data class ParsedCccdData(
    val docNumber: String,
    val cmndNumber: String,
    val fullName: String,
    val dobDdMmYyyy: String,
    val dobYyMmDd: String,
    val gender: String,
    val address: String,
    val issueDateDdMmYyyy: String,
    val expiryYyMmDd: String,
    val rawQrString: String
)

object CccdUtils {

    /**
     * Parses Vietnam CCCD QR Code format:
     * "SốCCCD|CMND|HọTên|NgàySinh(ddMMyyyy)|GiớiTính|ĐịaChỉ|NgàyCấp"
     */
    fun parseCccdQr(qrText: String): ParsedCccdData? {
        val cleanText = qrText.trim().replace("\n", "").replace("\r", "")
        val parts = cleanText.split("|")
        if (parts.size < 6) return null

        val docNumber = parts[0].trim()
        if (docNumber.length != 12) return null // Valid CCCD is 12 digits

        val cmndNumber = parts.getOrNull(1)?.trim() ?: ""
        val fullName = parts.getOrNull(2)?.trim() ?: ""
        val dobDdMmYyyy = parts.getOrNull(3)?.trim() ?: "" // ddMMyyyy
        val gender = parts.getOrNull(4)?.trim() ?: ""
        val address = parts.getOrNull(5)?.trim() ?: ""
        val issueDateDdMmYyyy = parts.getOrNull(6)?.trim() ?: "" // ddMMyyyy

        if (dobDdMmYyyy.length != 8) return null

        val day = dobDdMmYyyy.substring(0, 2).toIntOrNull() ?: 1
        val month = dobDdMmYyyy.substring(2, 4).toIntOrNull() ?: 1
        val year = dobDdMmYyyy.substring(4, 8).toIntOrNull() ?: 1990

        val dobYyMmDd = "${year.toString().takeLast(2)}${month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"

        val issueYear = if (issueDateDdMmYyyy.length == 8) {
            issueDateDdMmYyyy.substring(4, 8).toIntOrNull() ?: 2021
        } else {
            2021
        }

        val expiryYyMmDd = calculateExpiryDate(day, month, year, issueYear)

        return ParsedCccdData(
            docNumber = docNumber,
            cmndNumber = cmndNumber,
            fullName = fullName,
            dobDdMmYyyy = dobDdMmYyyy,
            dobYyMmDd = dobYyMmDd,
            gender = gender,
            address = address,
            issueDateDdMmYyyy = issueDateDdMmYyyy,
            expiryYyMmDd = expiryYyMmDd,
            rawQrString = qrText
        )
    }

    /**
     * Calculates expiry date according to Article 21 Vietnam Citizen Identity Law:
     * Cards must be exchanged at 25, 40, and 60 years old.
     * If age > 60, card is valid indefinitely ("991231").
     */
    fun calculateExpiryDate(dobDay: Int, dobMonth: Int, dobYear: Int, issueYear: Int): String {
        val currentAgeAtIssue = issueYear - dobYear
        val expiryYear = when {
            currentAgeAtIssue < 23 -> dobYear + 25
            currentAgeAtIssue in 23..38 -> dobYear + 40
            currentAgeAtIssue in 39..58 -> dobYear + 60
            else -> 2099
        }

        if (expiryYear >= 2099) {
            return "991231"
        }

        val yy = (expiryYear % 100).toString().padStart(2, '0')
        val mm = dobMonth.toString().padStart(2, '0')
        val dd = dobDay.toString().padStart(2, '0')
        return "$yy$mm$dd"
    }

    fun adjustYearInExpiry(expiryYyMmDd: String, deltaYears: Int): String {
        if (expiryYyMmDd.length != 6 || expiryYyMmDd == "991231") return expiryYyMmDd
        val yyStr = expiryYyMmDd.substring(0, 2)
        val rest = expiryYyMmDd.substring(2)
        val yy = yyStr.toIntOrNull() ?: return expiryYyMmDd
        val newYy = (yy + deltaYears + 100) % 100
        return "${newYy.toString().padStart(2, '0')}$rest"
    }
}
