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

    LaunchedEffect(tripId) {
        viewModel.getTrip(tripId)
    }

    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("upcoming") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(trip) {
        trip?.let {
            tripName = it.name
            destination = it.destination
            budget = it.budgetAmount.toInt().toString()
            status = it.status
            description = it.description
        }
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
                val updatedTrip = trip?.copy(
                    name = tripName,
                    destination = destination,
                    budgetAmount = budget.toDoubleOrNull() ?: 0.0,
                    status = status,
                    description = description
                )
                if (updatedTrip != null) {
                    viewModel.updateTrip(updatedTrip) {
                        onSaveClick()
                    }
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
