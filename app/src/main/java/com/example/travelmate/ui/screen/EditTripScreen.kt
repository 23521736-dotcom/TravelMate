package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.TripDetailViewModel
import com.example.travelmate.ui.screen.components.DetailCard
import com.example.travelmate.ui.screen.components.StatusChip
import com.example.travelmate.ui.screen.components.showDatePicker

import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTripScreen(
    tripId: String,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trip = uiState.trip
    val context = LocalContext.current

    // Permission check
    val isOwner = remember(uiState.currentUserMember) {
        uiState.currentUserMember?.role == com.example.travelmate.domain.model.TripMember.ROLE_OWNER
    }
    val canEdit = remember(uiState.currentUserMember) {
        val role = uiState.currentUserMember?.role
        role == com.example.travelmate.domain.model.TripMember.ROLE_OWNER || 
        role == com.example.travelmate.domain.model.TripMember.ROLE_EDITOR
    }

    LaunchedEffect(tripId) {
        viewModel.getTrip(tripId)
    }

    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("upcoming") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showWarningDialog by remember { mutableStateOf(false) }
    var lostDaysCount by remember { mutableIntStateOf(0) }
    var lostActivitiesCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(trip) {
        trip?.let {
            tripName = it.name
            destination = it.destination
            budget = it.budgetAmount.toInt().toString()
            status = it.status
            description = it.description
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            startDate = if (it.startDate > 0) sdf.format(Date(it.startDate)) else ""
            endDate = if (it.endDate > 0) sdf.format(Date(it.endDate)) else ""
        }
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Cảnh báo mất dữ liệu") },
            text = { Text("Việc thay đổi ngày sẽ làm mất $lostDaysCount ngày đã có $lostActivitiesCount hoạt động. Bạn có chắc chắn muốn tiếp tục?") },
            confirmButton = {
                Button(
                    onClick = {
                        performUpdate(trip, tripName, destination, budget, status, description, startDate, endDate, viewModel, onSaveClick)
                        showWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Chỉnh sửa chuyến đi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.clip(CircleShape).background(GrayBackground)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GrayBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            EditField(label = "TÊN CHUYẾN ĐI", value = tripName, onValueChange = { tripName = it })
            EditField(label = "ĐIỂM ĐẾN", value = destination, onValueChange = { destination = it })
            EditField(label = "NGÂN SÁCH (VND)", value = budget, onValueChange = { budget = it })

            Text(text = "CHẾ ĐỘ HIỂN THỊ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val visOptions = listOf(
                    com.example.travelmate.domain.model.Trip.VISIBILITY_PRIVATE to "Riêng tư",
                    com.example.travelmate.domain.model.Trip.VISIBILITY_LINK to "Qua link",
                    com.example.travelmate.domain.model.Trip.VISIBILITY_PUBLIC to "Công khai"
                )
                visOptions.forEach { (value, label) ->
                    StatusChip(label, trip?.visibility == value) {
                        if (isOwner) {
                            trip?.let { viewModel.updateTrip(it.copy(visibility = value)) {} }
                        }
                    }
                }
            }

            if (trip?.visibility != com.example.travelmate.domain.model.Trip.VISIBILITY_PRIVATE) {
                Text(text = "CHIA SẺ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                DetailCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Mã mời: ", fontWeight = FontWeight.Bold)
                            Text(text = trip?.inviteCode ?: "---", color = BluePrimary)
                        }
                        Text(text = "Link chia sẻ: travelmate://join?token=${trip?.shareToken}", fontSize = 11.sp, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
                val minEndDate = remember(startDate) {
                    try {
                        if (startDate.isNotEmpty()) {
                            val startTime = sdfDate.parse(startDate)?.time ?: 0L
                            startTime + (24 * 60 * 60 * 1000)
                        } else 0L
                    } catch (_: Exception) { 0L }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("NGÀY BẮT ĐẦU", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = { showDatePicker(context, null) { startDate = it } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(startDate.ifEmpty { "DD/MM/YYYY" }, color = if (startDate.isEmpty()) GrayText else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("NGÀY KẾT THÚC", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = { showDatePicker(context, if (minEndDate > 0) minEndDate else null) { endDate = it } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(endDate.ifEmpty { "DD/MM/YYYY" }, color = if (endDate.isEmpty()) GrayText else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Text(text = "TRẠNG THÁI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                StatusChip("Sắp tới", status == "upcoming") { status = "upcoming" }
                StatusChip("Đang đi", status == "ongoing") { status = "ongoing" }
                StatusChip("Hoàn thành", status == "done") { status = "done" }
                StatusChip("Đã hủy", status == "cancelled") { status = "cancelled" }
            }

            Text(text = "MÔ TẢ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = GrayBorder,
                    focusedBorderColor = BluePrimary
                )
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Save Button
        if (canEdit) {
            Button(
                onClick = {
                    if (trip != null) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        val newStart = try { sdf.parse(startDate)?.time ?: 0L } catch (e: Exception) { 0L }
                        val newEnd = try { sdf.parse(endDate)?.time ?: 0L } catch (e: Exception) { 0L }
                        
                        if (newStart > 0 && newEnd > 0) {
                            if (newEnd <= newStart) {
                                errorMessage = "Ngày kết thúc phải sau ngày bắt đầu"
                                return@Button
                            }
                            errorMessage = null

                            val diff = newEnd - newStart
                            val newDuration = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                            
                            if (newDuration < trip.durationDays) {
                                var lostDays = 0
                                var lostActs = 0
                                for (i in newDuration until trip.itinerary.size) {
                                    val day = trip.itinerary[i]
                                    if (day.activities.isNotEmpty()) {
                                        lostDays++
                                        lostActs += day.activities.size
                                    }
                                }
                                
                                if (lostActs > 0) {
                                    lostDaysCount = lostDays
                                    lostActivitiesCount = lostActs
                                    showWarningDialog = true
                                    return@Button
                                }
                            }
                        }
                        
                        performUpdate(trip, tripName, destination, budget, status, description, startDate, endDate, viewModel, onSaveClick)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Lưu thay đổi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Text(
                text = "Bạn không có quyền chỉnh sửa thông tin chuyến đi này.",
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
    }
}

private fun performUpdate(
    trip: com.example.travelmate.domain.model.Trip?,
    name: String,
    dest: String,
    budget: String,
    status: String,
    desc: String,
    start: String,
    end: String,
    viewModel: TripDetailViewModel,
    onSuccess: () -> Unit
) {
    if (trip == null) return
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val newStart = try { sdf.parse(start)?.time ?: 0L } catch (e: Exception) { 0L }
    val newEnd = try { sdf.parse(end)?.time ?: 0L } catch (e: Exception) { 0L }
    val diff = newEnd - newStart
    val newDuration = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    
    val newItinerary = mutableListOf<com.example.travelmate.domain.model.ItineraryDay>()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = newStart
    
    for (i in 0 until newDuration) {
        val existingDay = trip.itinerary.getOrNull(i)
        newItinerary.add(com.example.travelmate.domain.model.ItineraryDay(
            dayNumber = i + 1,
            date = calendar.timeInMillis,
            activities = existingDay?.activities ?: emptyList()
        ))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    val updatedTrip = trip.copy(
        name = name,
        destination = dest,
        budgetAmount = budget.toDoubleOrNull() ?: 0.0,
        status = status,
        description = desc,
        startDate = newStart,
        endDate = newEnd,
        durationDays = newDuration,
        itinerary = newItinerary
    )
    
    viewModel.updateTrip(updatedTrip) {
        onSuccess()
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = GrayBorder,
                focusedBorderColor = BluePrimary
            )
        )
    }
}
