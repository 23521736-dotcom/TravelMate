package com.example.travelmate.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Singleton
class EmailService @Inject constructor() {

    private val TAG = "EmailService"
    private val username = "buitung6705@gmail.com" // Updated sender email
    private val password = "bvcm bgxp axds bhav" // Updated App Password from user

    private val props = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.gmail.com")
        put("mail.smtp.port", "587")
        put("mail.smtp.connectiontimeout", "10000")
        put("mail.smtp.timeout", "10000")
    }

    private val session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    })

    suspend fun sendVerificationEmail(toEmail: String, fullName: String, verificationLink: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to send verification email to $toEmail")
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username, "TravelMate"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Xác nhận tài khoản TravelMate"
                val htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #2196F3;">Chào mừng ${fullName} đến với TravelMate!</h2>
                        <p>Cảm ơn bạn đã đăng ký tham gia cộng đồng du lịch của chúng tôi.</p>
                        <p>Để bắt đầu khám phá những chuyến đi tuyệt vời, vui lòng xác nhận địa chỉ email của bạn bằng cách nhấn vào nút bên dưới:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="${verificationLink}" style="background-color: #2196F3; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">Xác nhận Email</a>
                        </div>
                        <p style="font-size: 0.8em; color: #777;">Lưu ý: Liên kết này sẽ giúp bạn xác thực tài khoản trực tiếp trên hệ thống của chúng tôi.</p>
                        <hr style="border: 0; border-top: 1px solid #eee;">
                        <p style="text-align: center; color: #999;">© 2024 TravelMate Team</p>
                    </div>
                """.trimIndent()
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            Transport.send(message)
            Log.d(TAG, "Verification email sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending verification email: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun sendOTPEmail(toEmail: String, otp: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to send OTP email to $toEmail")
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username, "TravelMate"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Mã OTP xác thực TravelMate"
                val htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #2196F3;">Mã xác thực OTP</h2>
                        <p>Bạn đang thực hiện thao tác trên ứng dụng TravelMate.</p>
                        <p>Vui lòng sử dụng mã OTP dưới đây để hoàn tất quá trình:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2196F3; border: 2px dashed #2196F3; padding: 10px 20px; border-radius: 5px;">${otp}</span>
                        </div>
                        <p style="color: #f44336;">Mã OTP này sẽ hết hạn sau 5 phút.</p>
                        <p style="font-size: 0.8em; color: #777;">Tuyệt đối không chia sẻ mã này với bất kỳ ai để bảo mật tài khoản. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>
                        <hr style="border: 0; border-top: 1px solid #eee;">
                        <p style="text-align: center; color: #999;">© 2024 TravelMate Team</p>
                    </div>
                """.trimIndent()
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            Transport.send(message)
            Log.d(TAG, "OTP email sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending OTP email: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
