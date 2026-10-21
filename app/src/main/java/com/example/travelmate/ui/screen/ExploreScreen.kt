package com.example.travelmate.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.TravelMateApp
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.viewmodel.ExploreViewModel
import com.example.travelmate.ui.viewmodel.SearchMode
import com.example.travelmate.util.VoiceToTextParser

@Composable
fun ExploreScreen(
    onDestinationClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val wishlistIds = authUiState.user?.wishlistIds ?: emptyList()

    val context = LocalContext.current
    val voiceParser = remember { VoiceToTextParser(context.applicationContext as android.app.Application) }
    val voiceState by voiceParser.state.collectAsState()

    var canRecord by remember { mutableStateOf(false) }
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            canRecord = isGranted
            if (isGranted) {
                voiceParser.startListening()
            }
        }
    )

    // Update search query when voice text is received
    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            viewModel.onSearchQueryChange(voiceState.spokenText)
        }
    }

    val categories = uiState.categories
    val recentSearches = listOf("Da Lat flowers", "Hanoi street food", "Sa Pa trekking")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Tìm kiếm điểm đến, địa danh...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (voiceState.isListening) {
                                voiceParser.stopListening()
                            } else {
                                if (canRecord) {
                                    voiceParser.startListening()
                                } else {
                                    recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = if (voiceState.isListening) Color.Red else BluePrimary
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = BluePrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // Voice Status Overlay
        AnimatedVisibility(
            visible = voiceState.isListening || voiceState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                color = if (voiceState.error != null) Color.Red.copy(alpha = 0.1f) else BluePrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (voiceState.isListening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (voiceState.spokenText.isEmpty()) "Đang lắng nghe..." else voiceState.spokenText,
                            fontSize = 14.sp,
                            color = BluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (voiceState.error != null) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = voiceState.error!!, fontSize = 14.sp, color = Color.Red)
                    }
                }
            }
        }

        // Search Mode Selector (when search query is present)
        AnimatedVisibility(visible = uiState.searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.searchMode == SearchMode.ALL,
                    onClick = { viewModel.onSearchModeSelect(SearchMode.ALL) },
                    label = { Text("Tất cả", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = uiState.searchMode == SearchMode.GEOAPIFY_ONLINE,
                    onClick = { viewModel.onSearchModeSelect(SearchMode.GEOAPIFY_ONLINE) },
                    label = { Text("🌐 Geoapify API", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = uiState.searchMode == SearchMode.FIRESTORE_ONLY,
                    onClick = { viewModel.onSearchModeSelect(SearchMode.FIRESTORE_ONLY) },
                    label = { Text("💾 Firestore", fontSize = 12.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Show Categories and Recent Searches only when no search query
            if (uiState.searchQuery.isEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = category == uiState.selectedCategory,
                                onClick = { viewModel.onCategorySelect(category) },
                                label = { Text(category) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = category == uiState.selectedCategory,
                                    borderColor = GrayBorder,
                                    selectedBorderColor = BluePrimary
                                )
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(text = "Tìm kiếm gần đây", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        recentSearches.forEach { search ->
                            RecentSearchItem(text = search)
                        }
                    }
                }
            }

            // GEOAPIFY ONLINE RESULTS SECTION
            if (uiState.searchQuery.isNotEmpty() && (uiState.searchMode == SearchMode.ALL || uiState.searchMode == SearchMode.GEOAPIFY_ONLINE)) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐 Kết quả Online (Geoapify API)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.isGeoapifyLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
                        }
                    }
                }

                if (uiState.geoapifyDestinations.isEmpty() && !uiState.isGeoapifyLoading) {
                    item {
                        Text(
                            text = "Không tìm thấy địa điểm online cho '${uiState.searchQuery}'",
                            fontSize = 13.sp,
                            color = GrayText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(uiState.geoapifyDestinations) { destination ->
                        PopularDestinationCard(
                            destination = destination,
                            isWishlisted = wishlistIds.contains(destination.id),
                            onWishlistToggle = { authViewModel.toggleWishlist(destination.id) },
                            onClick = { onDestinationClick(destination.id) }
                        )
                    }
                }
            }

            // FIRESTORE RESULTS / POPULAR DESTINATIONS SECTION
            if (uiState.searchMode == SearchMode.ALL || uiState.searchMode == SearchMode.FIRESTORE_ONLY) {
                item {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) "Địa điểm nổi bật" else "💾 Kết quả CSDL Firestore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BluePrimary)
                        }
                    }
                } else {
                    items(uiState.filteredDestinations) { destination ->
                        PopularDestinationCard(
                            destination = destination,
                            isWishlisted = wishlistIds.contains(destination.id),
                            onWishlistToggle = { authViewModel.toggleWishlist(destination.id) },
                            onClick = { onDestinationClick(destination.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = GrayText, fontSize = 14.sp)
    }
}

@Composable
fun PopularDestinationCard(
    destination: Destination, 
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
                    .size(100.dp)
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
                    Text(text = destination.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onWishlistToggle) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                                contentDescription = "Wishlist", 
                                tint = if (isWishlisted) Color.Red else BluePrimary, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Text(text = "${destination.location}, ${destination.country}", color = GrayText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(14.dp))
                    Text(text = " ${destination.rating} (${destination.reviewCount})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (destination.isUnesco) {
                        Surface(color = BluePrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "UNESCO",
                                color = BluePrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (destination.id.startsWith("geoapify_")) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = BluePrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "🌐 Geoapify",
                                color = BluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
