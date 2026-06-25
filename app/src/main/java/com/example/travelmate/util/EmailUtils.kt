package com.example.travelmate.util

import android.util.Log
import com.example.travelmate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailUtils {
    // Cấu hình SMTP (Thay đổi thông tin này cho phù hợp với tài khoản gửi của bạn)
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "465"
    private val SENDER_EMAIL = BuildConfig.SENDER_EMAIL
    private val SENDER_PASSWORD = BuildConfig.SENDER_PASSWORD

    suspend fun sendOtpEmail(recipientEmail: String, otp: String): Boolean = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.socketFactory.port", SMTP_PORT)
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.socketFactory.fallback", "false")
            put("mail.smtp.user", SENDER_EMAIL)
            put("mail.debug", "true")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "TravelMate"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                setSubject("Mã xác thực TravelMate", "UTF-8")
                
                val htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;">
                        <h2 style="color: #2196F3; text-align: center;">Chào mừng bạn đến với TravelMate!</h2>
                        <p>Mã xác thực của bạn là:</p>
                        <div style="background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333; margin: 20px 0;">
                            $otp
                        </div>
                        <p>Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 12px; color: #888; text-align: center;">Đây là email tự động, vui lòng không phản hồi.</p>
                    </div>
                """.trimIndent()
                
                setContent(htmlContent, "text/html; charset=utf-8")
            }

            Transport.send(message)
            
            Log.d("EmailUtils", "Email sent successfully to $recipientEmail")
            true
        } catch (e: Exception) {
            Log.e("EmailUtils", "Error sending email to $recipientEmail: ${e.message}", e)
            false
        }
    }
}
