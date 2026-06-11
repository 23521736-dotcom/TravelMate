package com.example.travelmate.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.DestinationDetailViewModel
import com.example.travelmate.domain.model.Review
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
    viewModel: DestinationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val destination = uiState.destination
    val context = LocalContext.current

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
                            .padding(20.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Giá trung bình/ngày", fontSize = 12.sp, color = GrayText)
                            Text(
                                text = String.format(Locale.US, "%,.0f đ", destination.avgPricePerDay),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                        Button(
                            onClick = { onPlanTripClick() },
                            modifier = Modifier.height(56.dp).weight(1.5f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text(text = "Lên kế hoạch ngay", fontWeight = FontWeight.Bold, color = Color.White)
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
                Text(text = uiState.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (destination != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Image Header
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        AsyncImage(
                            model = destination.coverImage ?: destination.imageUrls.firstOrNull(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Transparent Back Button Overlay
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        // Title and Rating
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = destination.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = BluePrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                                    Text(text = " ${destination.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Location
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                            Text(text = " ${destination.location}, ${destination.country}", color = GrayText, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tags Row
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (destination.isUnesco) {
                                TagItem(text = "UNESCO Heritage", color = PurplePrimary)
                            }
                            destination.categories.forEach { category ->
                                TagItem(text = category.displayName, color = BluePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Description Section
                        Text(text = "Giới thiệu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = destination.description,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Quick Info Cards
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val priceLevel = if (destination.priceLevel > 0) "$".repeat(minOf(destination.priceLevel, 5)) else "N/A"
                            InfoBox(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, title = "Mức giá", value = priceLevel)
                            InfoBox(modifier = Modifier.weight(1f), icon = Icons.Default.Groups, title = "Đánh giá", value = "${destination.reviewCount}+")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Google Maps Button
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:${destination.latitude},${destination.longitude}?q=${Uri.encode(destination.name)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    // Thêm flag để tránh crash khi quay lại từ một task khác
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    // Fallback to web browser if Maps app not installed
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination.name)}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {
                                        // Handle case where no browser is available
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GrayBackground, contentColor = BluePrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Xem trên bản đồ", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Reviews Summary
                        if (uiState.reviews.isNotEmpty()) {
                            ReviewSummary(reviews = uiState.reviews)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Reviews List Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Đánh giá (${uiState.reviews.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                                Text(text = "Chưa có đánh giá nào. Hãy là người đầu tiên!", color = GrayText, fontSize = 14.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                uiState.reviews.forEach { review ->
                                    ReviewItem(review = review)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Extra space for bottom bar
                    }
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
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", if (avgRating.isNaN()) 0f else avgRating),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BluePrimary
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index <= avgRating) Color(0xFFFBC02D) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$count VERIFIED REVIEWS", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bars
            val distribution = (1..5).map { star -> 
                reviews.count { it.rating.toInt() == star }.toFloat() / count 
            }.reversed()
            
            distribution.forEachIndexed { index, percent ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "${5 - index}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
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
fun ReviewItem(review: Review) {
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
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            if (review.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(review.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
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
fun InfoBox(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 12.sp, color = GrayText)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


