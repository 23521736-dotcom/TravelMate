package com.example.travelmate.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.data.remote.WeatherUiData
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.Review
import com.example.travelmate.ui.screen.components.ImageViewerDialog
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.viewmodel.DestinationDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DestinationDetailScreen(
    destinationId: String,
    onBackClick: () -> Unit,
    onPlanTripClick: () -> Unit,
    onWriteReviewClick: (String) -> Unit,
    viewModel: DestinationDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val destination = uiState.destination
    val wishlistIds = authUiState.user?.wishlistIds ?: emptyList()
    val isWishlisted = destination != null && wishlistIds.contains(destination.id)

    val context = LocalContext.current
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(destinationId) {
        viewModel.getDestination(destinationId)
    }

    Scaffold(
        bottomBar = {
            if (destination != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlanTripClick() },
                            modifier = Modifier
                                .height(52.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088FF))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lên kế hoạch ngay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BluePrimary)
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (destination != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    // [1] Hero Image Header with Pager & Glassmorphism Overlay
                    val allImages = remember(destination) {
                        val images = mutableListOf<String>()
                        destination.coverImage?.let { images.add(it) }
                        images.addAll(destination.imageUrls)
                        images.distinct()
                    }
                    val pagerState = rememberPagerState(pageCount = { allImages.size })

                    Box(modifier = Modifier.fillMaxWidth().height(270.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = allImages[page],
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradient Overlays for contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )

                        // Glassmorphism Back Button
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        // Glassmorphism Favorite Button
                        IconButton(
                            onClick = { authViewModel.toggleWishlist(destination.id) },
                            modifier = Modifier
                                .statusBarsPadding()
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isWishlisted) Color.Red else Color.White
                            )
                        }

                        // Pager Indicator
                        if (allImages.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(allImages.size) { index ->
                                    val color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {

                        // [2] Title, Rating Badge & Category Tags (Reverted to original Category Tags)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = destination.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD54F))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", destination.rating)} (${destination.reviewCount})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Tags (Original Style)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (destination.isUnesco) {
                                TagItem(text = "UNESCO Heritage", color = PurplePrimary)
                            }
                            destination.categories.forEach { category ->
                                TagItem(text = "${category.icon} ${category.displayName}", color = BluePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // [3] Travel Weather Card & 3-Day Forecast
                        TravelWeatherCard(weatherData = uiState.weatherData)

                        Spacer(modifier = Modifier.height(24.dp))

                        // [4] Giới thiệu Section (Wikipedia REST API) & Traveloka Chrome Custom Tabs Banner
                        Text(
                            text = "Giới thiệu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Wikipedia summary text if available, fallback to cleaned destination description
                        val introText = uiState.wikiSummary ?: cleanTechnicalText(destination.description)
                        ExpandableText(text = introText)

                        if (!uiState.wikiPageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📖 Tham khảo bách khoa toàn thư Wikipedia ↗",
                                color = BluePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    launchChromeCustomTab(context, uiState.wikiPageUrl!!)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Traveloka Chrome Custom Tabs Banner
                        TravelokaBannerCard(destinationName = destination.name)

                        Spacer(modifier = Modifier.height(28.dp))

                        // [5] Nearby Attractions (Geoapify Places - tourism.sights, tourism.attraction)
                        NearbyAttractionsSection(
                            centerLat = destination.latitude,
                            centerLon = destination.longitude,
                            places = uiState.nearbyPlaces,
                            isLoading = uiState.isNearbyLoading,
                            onPlaceClick = { viewModel.getDestination(it) }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // [6] Mini-Map Snapshot & Google Maps Button
                        MiniMapCard(
                            destination = destination,
                            onDirectionsClick = {
                                val gmmIntentUri = Uri.parse("geo:${destination.latitude},${destination.longitude}?q=${Uri.encode(destination.name)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination.name)}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    try { context.startActivity(browserIntent) } catch (_: Exception) {}
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // [7] Reviews Section
                        if (uiState.reviews.isNotEmpty()) {
                            ReviewSummary(reviews = uiState.reviews)
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Đánh giá (${uiState.reviews.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { onWriteReviewClick(destinationId) }) {
                                Text(text = "Viết đánh giá", color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (uiState.reviews.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có đánh giá nào. Hãy là người đầu tiên!",
                                    color = GrayText,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                uiState.reviews.forEach { review ->
                                    ReviewItem(review = review, onImageClick = { selectedImageUrl = it })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Extra space for sticky bottom bar
                    }
                }
            }
        }
    }

    selectedImageUrl?.let { url ->
        ImageViewerDialog(imageUrl = url, onDismiss = { selectedImageUrl = null })
    }
}

fun launchChromeCustomTab(context: android.content.Context, url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setToolbarColor(android.graphics.Color.parseColor("#0088FF"))
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}

private fun cleanTechnicalText(raw: String): String {
    return raw.lines()
        .filterNot { line ->
            line.startsWith("Loại địa điểm:") ||
            line.startsWith("Khu vực:") ||
            line.startsWith("Tọa độ GPS:") ||
            line.contains("Dữ liệu địa điểm được trích xuất trực tuyến") ||
            line.contains("Geoapify Places API")
        }
        .joinToString("\n")
        .trim()
        .ifEmpty { "Địa điểm du lịch hấp dẫn, thích hợp cho chuyến đi nghỉ dưỡng và khám phá cùng bạn bè, gia đình." }
}

@Composable
fun TagItem(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TravelWeatherCard(weatherData: WeatherUiData?) {
    val weather = weatherData ?: WeatherUiData()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF0F9FF),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = weather.weatherIcon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${weather.currentTemp}°C",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = weather.weatherText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BluePrimary
                        )
                    }
                }
                Surface(
                    color = BluePrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Thời tiết hôm nay",
                        color = BluePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "💡 ${weather.travelAdvice}",
                fontSize = 12.sp,
                color = GrayText,
                fontWeight = FontWeight.Medium
            )

            if (weather.forecastDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BluePrimary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weather.forecastDays.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = day.dayName, fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                            Text(text = day.icon, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(text = day.tempRange, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableText(text: String, minimizedMaxLines: Int = 4) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else minimizedMaxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        if (text.lines().size > 4 || text.length > 200) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isExpanded) "Thu gọn ▲" else "Xem thêm ▼",
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
fun TravelokaBannerCard(destinationName: String) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val travelokaUrl = "https://www.traveloka.com/vi-vn/explore/search?q=${Uri.encode(destinationName)}"
                launchChromeCustomTab(context, travelokaUrl)
            },
        color = Color(0xFFE0F2FE),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = BluePrimary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cẩm nang du lịch $destinationName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Xem mẹo khám phá, ẩm thực và lịch trình chi tiết",
                    fontSize = 11.sp,
                    color = GrayText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = BluePrimary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Mở ngay ↗",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun NearbyAttractionsSection(
    centerLat: Double,
    centerLon: Double,
    places: List<Destination>,
    isLoading: Boolean,
    onPlaceClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Điểm tham quan nổi bật xung quanh",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary)
            }
        } else if (places.isEmpty()) {
            Text(text = "Đang cập nhật danh sách địa điểm xung quanh", color = GrayText, fontSize = 13.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(places) { place ->
                    val distKm = calculateDistanceKm(centerLat, centerLon, place.latitude, place.longitude)
                    val distStr = if (distKm > 0) "${String.format(Locale.US, "%.1f", distKm)} km" else "Lân cận"

                    Surface(
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { onPlaceClick(place.id) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            AsyncImage(
                                model = place.coverImage ?: place.imageUrls.firstOrNull() ?: "https://images.unsplash.com/photo-1503899036084-c55cdd92da26",
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = place.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = place.location,
                                        color = GrayText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = distStr,
                                        color = BluePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat1 == 0.0 || lat2 == 0.0) return 0.0
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
fun MiniMapCard(destination: Destination, onDirectionsClick: () -> Unit) {
    val staticMapUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=600&height=300&center=lonlat:${destination.longitude},${destination.latitude}&zoom=15&marker=lonlat:${destination.longitude},${destination.latitude};color:%230284c7;size:medium&apiKey=fc415480b8b0473790661a20b28ac855"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = staticMapUrl,
                    contentDescription = "Map Snapshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${destination.location}, ${destination.country}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDirectionsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Chỉ đường trên Google Maps", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReviewSummary(reviews: List<Review>) {
    val avgRating = reviews.map { it.rating }.average().toFloat()
    val count = reviews.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", if (avgRating.isNaN()) 0f else avgRating),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BluePrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index <= avgRating) Color(0xFFFBC02D) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "$count ĐÁNH GIÁ ĐÃ XÁC THỰC", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            val distribution = (1..5).map { star ->
                reviews.count { it.rating.toInt() == star }.toFloat() / count
            }.reversed()

            distribution.forEachIndexed { index, percent ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "${5 - index}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                        color = BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${(percent * 100).toInt()}%", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(24.dp))
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review, onImageClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GrayBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.userPhotoUrl ?: "https://ui-avatars.com/api/?name=${review.userName}",
                    contentDescription = null,
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(review.createdAt))
                    Text(text = dateStr, color = GrayText, fontSize = 11.sp)
                }
                Surface(
                    color = Color(0xFFFBC02D).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(12.dp))
                        Text(text = " ${review.rating.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFBC02D))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (review.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(review.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(75.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(url) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
