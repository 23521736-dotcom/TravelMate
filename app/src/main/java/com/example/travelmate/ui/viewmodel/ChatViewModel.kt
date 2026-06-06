package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.GroqMessage
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.ChatRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class ChatUiState(
    val messages: List<GroqMessage> = listOf(
        GroqMessage("system", "Bạn là TravelMate AI, một trợ lý du lịch nhiệt tình. Hãy giúp người dùng quản lý chuyến đi và chi tiêu."),
        GroqMessage("assistant", "Xin chào! Tôi là TravelMate AI. Tôi có thể giúp gì cho chuyến đi của bạn hôm nay?")
    ),
    val quickPrompts: List<String> = listOf(
        "Lịch trình 3 ngày ở Đà Lạt?",
        "Tôi đã tiêu bao nhiêu tiền?",
        "Mẹo tiết kiệm khi du lịch?",
        "Chuyến đi sắp tới của tôi?"
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val destinationRepository: com.example.travelmate.domain.repository.DestinationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var userDataContext: String = ""

    init {
        fetchUserDataContext()
    }

    private fun fetchUserDataContext() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val userId = user?.uid ?: return@launch

                // Fetch Trips
                val tripsResult = withTimeoutOrNull(5000) {
                    tripRepository.getUserTrips(userId).first { it !is NetworkResult.Loading }
                }
                val trips = (tripsResult as? NetworkResult.Success<List<Trip>>)?.data ?: emptyList()

                // Fetch Expenses
                val expensesResult = withTimeoutOrNull(5000) {
                    tripRepository.getAllExpenses(userId).first { it !is NetworkResult.Loading }
                }
                val expenses = (expensesResult as? NetworkResult.Success<List<Expense>>)?.data ?: emptyList()

                // Fetch Wishlist Destinations
                val destinationsResult = withTimeoutOrNull(5000) {
                    destinationRepository.getDestinations().first { it !is NetworkResult.Loading }
                }
                val wishlistDestinations = (destinationsResult as? NetworkResult.Success<List<Destination>>)?.data?.filter { it.id in (user.wishlistIds) } ?: emptyList()

                userDataContext = buildDataContextPrompt(user, trips, expenses, wishlistDestinations)
            } catch (e: Exception) {
                // Fallback to simpler context if failed
                val user = authRepository.getCurrentUser()
                userDataContext = "Dữ liệu người dùng hiện tại: ${user?.fullName ?: "Guest"}. Sở thích: ${user?.interests?.joinToString { it.displayName } ?: "Chưa có"}."
            }
        }
    }

    private fun buildDataContextPrompt(user: User, trips: List<Trip>, expenses: List<Expense>, wishlist: List<Destination>): String {
        val sb = StringBuilder()
        val userName = user.fullName ?: "User"
        
        sb.append("\n\nTHÔNG TIN DỮ LIỆU CỦA NGƯỜI DÙNG ($userName):\n")
        sb.append("- Sở thích: ${user.interests.joinToString { it.displayName }}\n")
        sb.append("- Tổng số chuyến đi: ${trips.size}\n")
        
        if (wishlist.isNotEmpty()) {
            sb.append("- Danh sách yêu thích (Wishlist): ${wishlist.joinToString { it.name }}\n")
        }
        
        if (trips.isNotEmpty()) {
            sb.append("- Danh sách chuyến đi:\n")
            trips.forEachIndexed { index, trip ->
                sb.append("  ${index + 1}. Chuyến đi: ${trip.name}\n")
                sb.append("     + Điểm đến: ${trip.destination}\n")
                sb.append("     + Trạng thái: ${trip.status}\n")
                sb.append("     + Thời gian: ${trip.durationDays} ngày\n")
                sb.append("     + Ngân sách: ${trip.budgetAmount} ${trip.currency}\n")
                if (trip.itinerary.isNotEmpty()) {
                    sb.append("     + Lịch trình: Đã lên kế hoạch ${trip.itinerary.size} ngày.\n")
                    trip.itinerary.take(3).forEach { day ->
                        val activities = day.activities.joinToString(", ") { it.title }
                        sb.append("       * Ngày ${day.dayNumber}: $activities\n")
                    }
                }
            }
        }

        val totalSpent = expenses.sumOf { it.amount }
        sb.append("\n- Tổng chi tiêu tất cả chuyến đi: ${String.format("%,.0f", totalSpent)} VND\n")
        if (expenses.isNotEmpty()) {
            sb.append("- 5 giao dịch gần nhất:\n")
            expenses.take(5).forEach { exp ->
                sb.append("  + ${exp.category}: ${String.format("%,.0f", exp.amount)} ${exp.currency} (${exp.description})\n")
            }
        }

        return sb.toString()
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val userMessage = GroqMessage("user", content)
        
        val messagesWithContext = mutableListOf<GroqMessage>()
        
        val baseSystemPrompt = """
            Bạn là TravelMate AI, một trợ lý du lịch thông minh và thân thiện. 
            Nhiệm vụ của bạn là giúp người dùng lên kế hoạch chuyến đi, tìm kiếm điểm đến, quản lý chi tiêu và cung cấp các mẹo du lịch.
            Hãy trả lời ngắn gọn, hữu ích và sử dụng ngôn ngữ phù hợp (ưu tiên tiếng Việt).
            Sử dụng dữ liệu context được cung cấp dưới đây để trả lời các câu hỏi cụ thể của người dùng về chuyến đi và chi tiêu của họ.
            Nếu người dùng hỏi về gợi ý hoặc cải thiện lịch trình, hãy dựa trên những gì họ đã có.
        """.trimIndent()

        messagesWithContext.add(GroqMessage("system", baseSystemPrompt + userDataContext))
        
        _uiState.value.messages.drop(1).forEach { messagesWithContext.add(it) }
        
        messagesWithContext.add(userMessage)

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = chatRepository.getChatResponse(messagesWithContext)
            result.onSuccess { assistantMessage ->
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to get AI response"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
