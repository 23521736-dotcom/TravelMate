package com.example.travelmate.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.User
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.viewmodel.HomeViewModel
import java.util.Locale

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDestinationClick: (String) -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val user = authUiState.user ?: uiState.user

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header & Search Area
        HomeHeader(user, onProfileClick, onNotificationsClick, onSearchClick)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Promo Card
            PromoCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FlightTakeoff,
                    value = uiState.totalTrips.toString(),
                    label = "Chuyến đi",
                    color = BluePrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Payments,
                    value = String.format(Locale.US, "%.1fM", uiState.totalSpent / 1000000),
                    label = "Đã chi",
                    color = OrangePrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    value = uiState.upcomingTrips.toString(),
                    label = "Sắp tới",
                    color = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ongoing Card
            OngoingTripCard(
                trip = uiState.ongoingTrip,
                onClick = { uiState.ongoingTrip?.let { onTripClick(it.id) } }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Section: Top Destinations
            SectionHeader(title = "Điểm đến hàng đầu", actionText = "")
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(uiState.recommendedDestinations.take(4)) { destination ->
                    TopDestinationItem(destination, onClick = { onDestinationClick(destination.id) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Recommended for You (Based on Interests)
            SectionHeader(title = "Gợi ý cho bạn", actionText = "")
            Spacer(modifier = Modifier.height(8.dp))
            
            val userInterests = user?.interests ?: emptyList()
            if (userInterests.isNotEmpty()) {
                Text(
                    text = "Dựa trên sở thích: ${userInterests.joinToString { it.displayName }}",
                    fontSize = 12.sp,
                    color = GrayText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else {
                uiState.recommendedDestinations.forEach { destination ->
                    RecommendationCard(destination, onClick = { onDestinationClick(destination.id) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HomeHeader(user: User?, onProfileClick: () -> Unit, onNotificationsClick: () -> Unit, onSearchClick: () -> Unit) {
    val userName = user?.fullName ?: "Khách"
    val photoUrl = user?.photoUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Chào buổi chiều,", fontSize = 14.sp, color = GrayText)
                Text(text = "$userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Row {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    color = PurplePrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Ảnh đại diện",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(text = userName.firstOrNull()?.toString() ?: "U", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar (Clickable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() }
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Tìm điểm đến, hoạt động...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = BluePrimary) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false, // Disable typing, make it a button
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = GrayBorder.copy(alpha = 0.5f),
                    disabledPlaceholderColor = GrayText,
                    disabledLeadingIconColor = GrayText,
                    disabledTrailingIconColor = BluePrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun PromoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đặt xe sớm trước 3 ngày để có giá tốt nhất!",
                fontSize = 12.sp,
                color = BluePrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 12.sp, color = GrayText)
        }
    }
}

@Composable
fun OngoingTripCard(trip: com.example.travelmate.domain.model.Trip?, onClick: () -> Unit) {
    if (trip == null) {
        // Placeholder or empty state
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(20.dp),
            color = GrayBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "Bạn không có chuyến đi nào sắp tới", color = GrayText, fontSize = 14.sp)
            }
        }
        return
    }

    val isOngoing = trip.status == com.example.travelmate.domain.model.Trip.STATUS_ONGOING

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isOngoing) listOf(GreenPrimary, GreenHeader) else listOf(BluePrimary, BlueHeader)
                )
            )
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 60.dp.toPx(),
                center = Offset(size.width * 0.9f, size.height * 0.2f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOngoing) "ĐANG DIỄN RA" else "SẮP TỚI",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = trip.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Text(text = " ${trip.destination}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (actionText.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = actionText, fontSize = 13.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun RecommendationCard(destination: Destination, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = destination.coverImage ?: destination.imageUrls.firstOrNull() ?: "https://images.unsplash.com/photo-1583417319070-4a69db38a482",
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayBackground),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = destination.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(14.dp))
                        Text(text = " ${destination.rating}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text(text = "${destination.location}, ${destination.country}", color = GrayText, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (category in destination.categories.take(3)) {
                        Surface(
                            color = BluePrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${category.icon} ${category.displayName}",
                                color = BluePrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopDestinationItem(destination: Destination, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = destination.coverImage ?: destination.imageUrls.firstOrNull() ?: "https://images.unsplash.com/photo-1583417319070-4a69db38a482",
                contentDescription = null,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GrayBackground),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(12.dp))
                    Text(text = " ${destination.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = destination.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = destination.location,
            color = GrayText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DestinationItem(name: String, country: String) {
    Column(modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GrayBackground)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = country, color = GrayText, fontSize = 12.sp)
    }
}
