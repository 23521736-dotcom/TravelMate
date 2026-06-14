package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.TripViewModel
import java.util.Locale

@Composable
fun MyTripsScreen(
    onAddTripClick: () -> Unit,
    onJoinTripClick: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: TripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters = listOf("All", "Upcoming", "Ongoing", "Completed")
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshTrips()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "My Trips", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${uiState.trips.size} chuyến đi", fontSize = 14.sp, color = GrayText)
            }
            Row {
                IconButton(onClick = { viewModel.refreshTrips() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = GrayText)
                }
                IconButton(onClick = onJoinTripClick) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Join Trip", tint = BluePrimary)
                }
                Surface(
                    onClick = onAddTripClick,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = BluePrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm chuyến đi...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = GrayBorder.copy(alpha = 0.5f),
                focusedBorderColor = BluePrimary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            items(filters) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    onClick = { selectedFilter = filter },
                    color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else GrayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BluePrimary)
            } else if (uiState.error != null) {
                Text(text = uiState.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (uiState.trips.isEmpty()) {
                EmptyTripsPlaceholder(onAddTripClick)
            } else {
                val filteredTrips = uiState.trips.filter { trip ->
                    val matchesFilter = when (selectedFilter) {
                        "Upcoming" -> trip.status == com.example.travelmate.domain.model.Trip.STATUS_UPCOMING
                        "Ongoing" -> trip.status == com.example.travelmate.domain.model.Trip.STATUS_ONGOING
                        "Completed" -> trip.status == com.example.travelmate.domain.model.Trip.STATUS_COMPLETED
                        else -> true
                    }
                    val matchesSearch = trip.name.contains(searchQuery, ignoreCase = true) || 
                                       trip.destination.contains(searchQuery, ignoreCase = true)
                    matchesFilter && matchesSearch
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredTrips) { trip ->
                        // Get role for the current user
                        val currentUserMember = uiState.tripsWithRoles.find { it.first.id == trip.id }?.second
                        
                        DetailedTripCard(
                            title = trip.name,
                            location = trip.destination,
                            status = trip.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            statusColor = when(trip.status) {
                                com.example.travelmate.domain.model.Trip.STATUS_ONGOING -> GreenHeader
                                com.example.travelmate.domain.model.Trip.STATUS_UPCOMING -> OrangePrimary
                                else -> GrayText
                            },
                            date = formatDateRange(trip.startDate, trip.endDate),
                            duration = "${trip.durationDays} ngày",
                            budget = String.format(Locale.US, "%,.0f %s", trip.budgetAmount, trip.currency),
                            coverImageUrl = trip.coverImageUrl,
                            badge = currentUserMember?.role,
                            onClick = { onTripClick(trip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTripsPlaceholder(onAddTripClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = GrayBorder)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Bạn chưa có chuyến đi nào", color = GrayText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddTripClick, shape = RoundedCornerShape(12.dp)) {
            Text("Bắt đầu lên kế hoạch")
        }
    }
}

@Composable
fun DetailedTripCard(
    title: String,
    location: String,
    status: String,
    statusColor: Color,
    date: String,
    duration: String,
    budget: String,
    coverImageUrl: String = "",
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(GrayBackground)
            ) {
                if (coverImageUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = coverImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                if (badge != null) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        color = OrangePrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(text = title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        Text(text = location, color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = date, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = duration, fontSize = 10.sp, color = GrayText)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Budget", fontSize = 10.sp, color = GrayText)
                        Text(text = budget, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

fun formatDateRange(start: Long, end: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
    return "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
}
