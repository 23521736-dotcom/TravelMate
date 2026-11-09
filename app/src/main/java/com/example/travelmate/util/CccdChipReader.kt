package com.example.travelmate.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import net.sf.scuba.smartcards.CardService

data class CccdChipResult(
    val docNumber: String,
    val fullName: String,
    val dob: String,
    val gender: String,
    val faceBitmap: Bitmap?
)

object CccdChipReader {

    suspend fun readCccdChip(
        isoDep: IsoDep,
        parsedQr: ParsedCccdData
    ): Result<CccdChipResult> = withContext(Dispatchers.IO) {
        try {
            if (!isoDep.isConnected) {
                isoDep.connect()
            }
            isoDep.timeout = 10000

            val cardService = CardService.getInstance(isoDep)
            val passportService = PassportService(
                cardService,
                256,
                224,
                false,
                false
            )

            passportService.open()

            val primaryBacKey = BACKey(parsedQr.docNumber, parsedQr.dobYyMmDd, parsedQr.expiryYyMmDd)
            val candidateBacKeys = listOf(
                primaryBacKey,
                BACKey(parsedQr.docNumber, parsedQr.dobYyMmDd, "991231"),
                BACKey(parsedQr.docNumber, parsedQr.dobYyMmDd, CccdUtils.adjustYearInExpiry(parsedQr.expiryYyMmDd, 1)),
                BACKey(parsedQr.docNumber, parsedQr.dobYyMmDd, CccdUtils.adjustYearInExpiry(parsedQr.expiryYyMmDd, -1))
            ).distinctBy { it.dateOfExpiry }

            var bacSuccess = false

            for (bacKey in candidateBacKeys) {
                try {
                    passportService.sendSelectApplet(false)
                    passportService.doBAC(bacKey)
                    bacSuccess = true
                    break
                } catch (bacErr: Exception) {
                    android.util.Log.w("CccdChipReader", "BAC attempt failed for expiry ${bacKey.dateOfExpiry}: ${bacErr.message}")
                }
            }

            if (!bacSuccess) {
                return@withContext Result.failure(Exception("Sai khóa xác thực BAC. Vui lòng kiểm tra lại mã QR."))
            }

            // Read EF.DG1 (Data Group 1)
            var readFullName = parsedQr.fullName
            try {
                val dg1InputStream = passportService.getInputStream(PassportService.EF_DG1)
                val dg1File = DG1File(dg1InputStream)
                val mrzInfo = dg1File.mrzInfo
                val primaryName = mrzInfo.primaryIdentifier?.replace("<", " ")?.trim() ?: ""
                val secondaryName = mrzInfo.secondaryIdentifier?.replace("<", " ")?.trim() ?: ""
                val mrzName = "$secondaryName $primaryName".trim()
                if (mrzName.isNotBlank()) {
                    readFullName = mrzName
                }
            } catch (e: Exception) {
                android.util.Log.w("CccdChipReader", "Could not read EF.DG1: ${e.message}")
            }

            // Read EF.DG2 (Data Group 2 - Biometric Face Photo)
            var faceBitmap: Bitmap? = null
            try {
                val dg2InputStream = passportService.getInputStream(PassportService.EF_DG2)
                val dg2File = DG2File(dg2InputStream)
                val faceInfos = dg2File.faceInfos
                for (faceInfo in faceInfos) {
                    for (faceImageInfo in faceInfo.faceImageInfos) {
                        val imageInputStream = faceImageInfo.imageInputStream
                        val imageBytes = imageInputStream.readBytes()
                        faceBitmap = decodeJp2Image(imageBytes)
                        if (faceBitmap != null) break
                    }
                    if (faceBitmap != null) break
                }
            } catch (e: Exception) {
                android.util.Log.e("CccdChipReader", "Could not read EF.DG2 face photo", e)
            }

            val result = CccdChipResult(
                docNumber = parsedQr.docNumber,
                fullName = readFullName,
                dob = parsedQr.dobDdMmYyyy,
                gender = parsedQr.gender,
                faceBitmap = faceBitmap
            )

            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("CccdChipReader", "Fatal error reading CCCD Chip", e)
            Result.failure(e)
        }
    }

    private fun decodeJp2Image(bytes: ByteArray): Bitmap? {
        // Try Gemalto JP2 Decoder first
        try {
            val jp2Class = Class.forName("com.gemalto.jp2.JP2Decoder")
            val constructor = jp2Class.getConstructor(ByteArray::class.java)
            val decoder = constructor.newInstance(bytes)
            val decodeMethod = jp2Class.getMethod("decode")
            val bmp = decodeMethod.invoke(decoder) as? Bitmap
            if (bmp != null) return bmp
        } catch (_: Exception) { }

        // Fallback to BitmapFactory
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
