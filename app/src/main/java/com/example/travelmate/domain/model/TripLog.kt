package com.example.travelmate.domain.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class TripLog(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("tripId") @set:PropertyName("tripId") var tripId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("userName") @set:PropertyName("userName") var userName: String = "",
    @get:PropertyName("action") @set:PropertyName("action") var action: String = "", // CREATE_TRIP, ADD_ACTIVITY, EDIT_ACTIVITY, DELETE_ACTIVITY, ADD_EXPENSE, etc.
    @get:PropertyName("details") @set:PropertyName("details") var details: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_CREATE_TRIP = "Đã tạo chuyến đi"
        const val ACTION_UPDATE_TRIP = "Đã cập nhật thông tin chung"
        const val ACTION_ADD_ACTIVITY = "Đã thêm hoạt động"
        const val ACTION_EDIT_ACTIVITY = "Đã sửa hoạt động"
        const val ACTION_DELETE_ACTIVITY = "Đã xóa hoạt động"
        const val ACTION_ADD_EXPENSE = "Đã thêm chi tiêu"
        const val ACTION_DELETE_EXPENSE = "Đã xóa chi tiêu"
        const val ACTION_JOIN_TRIP = "Đã tham gia nhóm"
        const val ACTION_LEAVE_TRIP = "Đã rời nhóm"
        const val ACTION_CHANGE_ROLE = "Đã thay đổi vai trò thành viên"
    }
}
