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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.TripDetailViewModel

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
            Text(text = "Edit Trip", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            EditField(label = "TRIP NAME", value = tripName, onValueChange = { tripName = it })
            EditField(label = "DESTINATION", value = destination, onValueChange = { destination = it })
            EditField(label = "BUDGET (VND)", value = budget, onValueChange = { budget = it })

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("START DATE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = { showDatePicker(context) { startDate = it } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(startDate.ifEmpty { "DD/MM/YYYY" }, color = if (startDate.isEmpty()) GrayText else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("END DATE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = { showDatePicker(context) { endDate = it } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(endDate.ifEmpty { "DD/MM/YYYY" }, color = if (endDate.isEmpty()) GrayText else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Text(text = "STATUS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                StatusChip("Upcoming", status == "upcoming") { status = "upcoming" }
                StatusChip("Ongoing", status == "ongoing") { status = "ongoing" }
                StatusChip("Done", status == "done") { status = "done" }
                StatusChip("Cancelled", status == "cancelled") { status = "cancelled" }
            }

            Text(text = "DESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
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
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Save Button
        Button(
            onClick = {
                if (trip != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val newStart = try { sdf.parse(startDate)?.time ?: 0L } catch (e: Exception) { 0L }
                    val newEnd = try { sdf.parse(endDate)?.time ?: 0L } catch (e: Exception) { 0L }
                    
                    if (newStart > 0 && newEnd > 0) {
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
            Text(text = "Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
fun StatusChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GreenLight else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GreenPrimary else GrayBorder),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GreenPrimary else GrayText)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = if (isSelected) GreenPrimary else GrayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
