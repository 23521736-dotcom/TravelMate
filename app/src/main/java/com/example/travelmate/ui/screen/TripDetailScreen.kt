package com.example.travelmate.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.ItineraryDay
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.TripDetailViewModel
import com.example.travelmate.ui.screen.components.DetailCard
import com.example.travelmate.ui.screen.components.ImageViewerDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripDetailScreen(
    tripId: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onManageMembersClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeleteTripSuccess: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trip = uiState.trip
    val context = LocalContext.current

    // Permission check (Observing state reactively)
    val canEdit = remember(uiState.currentUserMember) {
        val role = uiState.currentUserMember?.role
        role == com.example.travelmate.domain.model.TripMember.ROLE_OWNER || 
        role == com.example.travelmate.domain.model.TripMember.ROLE_EDITOR
    }
    val isOwner = remember(uiState.currentUserMember) {
        uiState.currentUserMember?.role == com.example.travelmate.domain.model.TripMember.ROLE_OWNER
    }
    
    // Check if trip was deleted or user was removed
    LaunchedEffect(uiState.error) {
        if (uiState.error?.contains("không thuộc nhóm", ignoreCase = true) == true || 
            uiState.error?.contains("Không tìm thấy", ignoreCase = true) == true) {
            onBackClick()
        }
    }

    LaunchedEffect(tripId) {
        viewModel.getTrip(tripId)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Itinerary", "Expenses", "Photos")

    // State to trigger Add/Edit Activity dialog
    var showActivityDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<TripActivity?>(null) }
    var selectedDayIndexForActivity by remember { mutableIntStateOf(0) }
    
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        DeleteTripConfirmDialog(
            tripName = trip?.name ?: "chuyến đi này",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteTrip(tripId, onDeleteTripSuccess)
                showDeleteDialog = false
            }
        )
    }

    if (showActivityDialog) {
        val currentDayActivities = trip?.itinerary?.getOrNull(selectedDayIndexForActivity)?.activities ?: emptyList()
        ActivityDialog(
            dayInfo = trip?.itinerary?.getOrNull(selectedDayIndexForActivity)?.let { "Day ${it.dayNumber}" } ?: "",
            activity = editingActivity,
            existingActivities = currentDayActivities,
            canEdit = canEdit,
            onDismiss = { 
                showActivityDialog = false
                editingActivity = null
            },
            onConfirm = { act, imageBytes ->
                viewModel.saveActivity(selectedDayIndexForActivity, act, imageBytes)
                showActivityDialog = false
                editingActivity = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Image
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = trip?.coverImageUrl.takeIf { it?.isNotEmpty() == true } ?: "https://images.unsplash.com/photo-1583417319070-4a69db38a482",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.2f))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val context = LocalContext.current
                        val coverPicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia(),
                            onResult = { uri ->
                                uri?.let {
                                    context.contentResolver.openInputStream(it)?.use { stream ->
                                        viewModel.updateTripCover(stream.readBytes())
                                    }
                                }
                            }
                        )

                        if (canEdit) {
                            HeaderIcon(icon = Icons.Default.AddPhotoAlternate, onClick = {
                                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            })
                        }

                        HeaderIcon(icon = Icons.Default.History, onClick = onHistoryClick)
                        HeaderIcon(icon = Icons.Default.Share, onClick = {
                            val shareText = "Hãy tham gia cùng tôi trong chuyến đi: ${trip?.name}\n" +
                                    "Mã mời: ${trip?.inviteCode}\n" +
                                    "Hoặc dùng link: travelmate://join?token=${trip?.shareToken}\n\n" +
                                    "Chia sẻ từ TravelMate"
                            
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        })
                        HeaderIcon(icon = Icons.Default.Group, onClick = onManageMembersClick)
                        if (canEdit) {
                            HeaderIcon(icon = Icons.Default.Edit, onClick = onEditClick)
                        }
                        if (isOwner) {
                            HeaderIcon(icon = Icons.Default.Delete, onClick = { 
                                showDeleteDialog = true
                            })
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = GreenPrimary, shape = RoundedCornerShape(8.dp)) {
                            Text(text = trip?.status?.uppercase() ?: "UPCOMING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        uiState.currentUserMember?.let { member ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = when(member.role) {
                                    com.example.travelmate.domain.model.TripMember.ROLE_OWNER -> Color(0xFFFFD700) // Gold
                                    com.example.travelmate.domain.model.TripMember.ROLE_EDITOR -> BluePrimary
                                    else -> GrayText
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = member.role,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(text = trip?.name ?: "Loading...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                        Text(text = " ${trip?.destination ?: ""} • ${trip?.durationDays ?: 0} ngày", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BluePrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && trip == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BluePrimary)
                } else {
                    when (selectedTab) {
                        0 -> OverviewTab(
                            trip = trip,
                            totalSpent = uiState.totalSpent,
                            expenses = uiState.expenses,
                            members = uiState.members,
                            currentUserMember = uiState.currentUserMember,
                            canEdit = canEdit,
                            onAddExpense = { selectedTab = 2 },
                            onAddActivity = { 
                                selectedDayIndexForActivity = 0
                                showActivityDialog = true
                            }
                        )
                        1 -> {
                            ItineraryTab(
                                itinerary = trip?.itinerary ?: emptyList(),
                                canEdit = canEdit,
                                onAddActivityClick = { dayIndex ->
                                    selectedDayIndexForActivity = dayIndex
                                    editingActivity = null
                                    showActivityDialog = true
                                },
                                onEditActivityClick = { dayIndex, activity ->
                                    selectedDayIndexForActivity = dayIndex
                                    editingActivity = activity
                                    showActivityDialog = true
                                },
                                onToggleDone = { day, id -> viewModel.toggleActivityDone(day, id) },
                                onDeleteActivity = { day, id -> viewModel.deleteActivity(day, id) }
                            )
                        }
                        2 -> ExpensesTabContentHighFidelity(
                            trip = trip,
                            totalSpent = uiState.totalSpent,
                            expenses = uiState.expenses,
                            canEdit = canEdit,
                            onAddClick = { onAddExpenseClick() },
                            onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                        )
                        3 -> PhotosTab(
                            urls = trip?.galleryUrls ?: emptyList(),
                            canEdit = canEdit,
                            onAddPhotos = { uriStrings ->
                                val byteArrayList = mutableListOf<ByteArray>()
                                uriStrings.forEach { uriString ->
                                    val uri = android.net.Uri.parse(uriString)
                                    try {
                                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                            byteArrayList.add(inputStream.readBytes())
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                if (byteArrayList.isNotEmpty()) {
                                    viewModel.addPhotos(byteArrayList)
                                }
                            },
                            onDeletePhoto = { url -> viewModel.deletePhoto(url) },
                            onPhotoClick = { url -> selectedImageUrl = url }
                        )
                    }
                }
            }
        }
    }

    selectedImageUrl?.let { url ->
        ImageViewerDialog(imageUrl = url, onDismiss = { selectedImageUrl = null })
    }
}

@Composable
fun OverviewTab(trip: com.example.travelmate.domain.model.Trip?, totalSpent: Double, expenses: List<Expense>, members: List<com.example.travelmate.domain.model.TripMember>, currentUserMember: com.example.travelmate.domain.model.TripMember?, canEdit: Boolean, onAddExpense: () -> Unit, onAddActivity: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        // Budget Overview Card
        DetailCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Budget Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.Payments, contentDescription = null, tint = OrangePrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(text = String.format(Locale.US, "%,.1fM đ", totalSpent / 1000000), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "spent", fontSize = 12.sp, color = GrayText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val remaining = (trip?.budgetAmount ?: 0.0) - totalSpent
                    Text(text = String.format(Locale.US, "%,.1fM đ", if (remaining > 0) remaining / 1000000 else 0.0), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Text(text = "remaining", fontSize = 12.sp, color = GrayText)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val progress = if ((trip?.budgetAmount ?: 0.0) > 0) (totalSpent / trip!!.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = BluePrimary,
                trackColor = GrayBackground
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${(progress * 100).toInt()}% used", fontSize = 10.sp, color = GrayText)
                Text(text = "Budget: ${String.format(Locale.US, "%,.1fM đ", (trip?.budgetAmount ?: 0.0) / 1000000)}", fontSize = 10.sp, color = GrayText)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Spending Breakdown
        DetailCard {
            Text(text = "Spending Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpendingDonutChart(modifier = Modifier.size(100.dp), expenses = expenses)
                Spacer(modifier = Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> convertToVnd(exp.amount, exp.currency) } }
                    val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }
                    val colors = listOf(OrangePrimary, BluePrimary, PurplePrimary, GreenPrimary, Color.Magenta)
                    
                    if (sortedCategories.isEmpty()) {
                        Text(text = "No expenses yet", fontSize = 12.sp, color = GrayText)
                    } else {
                        sortedCategories.take(4).forEachIndexed { index, entry ->
                            BreakdownItem(
                                color = colors[index % colors.size], 
                                label = entry.key,
                                amount = String.format(Locale.US, "%,.0f đ", entry.value)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weather Card
        DetailCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Weather in ${trip?.destination?.split(",")?.firstOrNull() ?: ""}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Dự báo cho chuyến đi", fontSize = 12.sp, color = GrayText)
                }
                Icon(Icons.Default.Cloud, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeatherDayItem("Mon", "24°C", Icons.Default.WbSunny, Color(0xFFFBC02D))
                WeatherDayItem("Tue", "22°C", Icons.Default.Cloud, BluePrimary)
                WeatherDayItem("Wed", "25°C", Icons.Default.WbSunny, Color(0xFFFBC02D))
                WeatherDayItem("Thu", "21°C", Icons.Default.Thunderstorm, GrayText)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions
        if (canEdit) {
            Text(text = "Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionBtn(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, label = "Add Expense", color = OrangeLight, tint = OrangePrimary, onClick = onAddExpense)
                ActionBtn(modifier = Modifier.weight(1f), icon = Icons.Default.AddLocationAlt, label = "Add Activity", color = GreenLight, tint = GreenPrimary, onClick = onAddActivity)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Permission Info Hint
        currentUserMember?.let { member ->
            val permissionText = when(member.role) {
                com.example.travelmate.domain.model.TripMember.ROLE_OWNER -> "Bạn là trưởng nhóm. Bạn có toàn quyền quản lý thành viên và chuyến đi."
                com.example.travelmate.domain.model.TripMember.ROLE_EDITOR -> "Bạn là biên tập viên. Bạn có thể chỉnh sửa lịch trình và chi tiêu."
                else -> "Bạn là người xem. Bạn chỉ có thể xem thông tin chuyến đi."
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = permissionText, fontSize = 12.sp, color = BluePrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(text = "About This Trip", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = trip?.description ?: "No description provided.", fontSize = 14.sp, color = GrayText, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ItineraryTab(
    itinerary: List<ItineraryDay>, 
    canEdit: Boolean,
    onAddActivityClick: (Int) -> Unit,
    onEditActivityClick: (Int, TripActivity) -> Unit,
    onToggleDone: (Int, String) -> Unit,
    onDeleteActivity: (Int, String) -> Unit
) {
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (itinerary.isEmpty()) {
            EmptyStateView(
                icon = Icons.AutoMirrored.Filled.EventNote,
                message = "Chưa có lịch trình cho chuyến đi này.",
                actionText = "Tạo lịch trình",
                onAction = { onAddActivityClick(0) }
            )
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(itinerary) { index, day ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = day.date }
                    val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(calendar.time).uppercase()
                    val dayOfMonth = SimpleDateFormat("dd", Locale.US).format(calendar.time)
                    
                    Surface(
                        modifier = Modifier.width(75.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedDayIndex == index) BluePrimary else MaterialTheme.colorScheme.surface,
                        border = if (selectedDayIndex != index) androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)) else null,
                        onClick = { selectedDayIndex = index }
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = dayOfWeek, fontSize = 10.sp, color = if (selectedDayIndex == index) Color.White else GrayText, fontWeight = FontWeight.Bold)
                            Text(text = dayOfMonth, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (selectedDayIndex == index) Color.White else MaterialTheme.colorScheme.onSurface)
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (selectedDayIndex == index) Color.White else BluePrimary))
                            Text(text = "Day ${day.dayNumber}", fontSize = 10.sp, color = if (selectedDayIndex == index) Color.White else GrayText, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            itinerary.getOrNull(selectedDayIndex)?.let { currentDay ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(currentDay.date))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = dateStr, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Day ${currentDay.dayNumber} • ${currentDay.activities.size} activities", fontSize = 12.sp, color = GrayText)
                    }
                    if (canEdit) {
                        Button(onClick = { onAddActivityClick(selectedDayIndex) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Text(" Add", color = Color.White)
                        }
                    }
                }

                if (currentDay.activities.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                        Text(text = "Chưa có hoạt động nào.", color = GrayText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
                        items(currentDay.activities) { activity ->
                            TimelineActivityItem(
                                activity = activity,
                                canEdit = canEdit,
                                onToggleDone = { onToggleDone(selectedDayIndex, activity.id) },
                                onDelete = { onDeleteActivity(selectedDayIndex, activity.id) },
                                onEdit = { onEditActivityClick(selectedDayIndex, activity) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineActivityItem(activity: TripActivity, canEdit: Boolean, onToggleDone: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = activity.time, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayText)
            Spacer(modifier = Modifier.height(4.dp))
            IconButton(
                onClick = { if (canEdit) onToggleDone() }, 
                modifier = Modifier.size(24.dp),
                enabled = canEdit
            ) {
                Icon(
                    imageVector = if (activity.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, 
                    contentDescription = null, 
                    tint = if (activity.isDone) GreenPrimary else GrayBorder, 
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(modifier = Modifier.width(2.dp).height(50.dp).background(GrayBorder))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Surface(
            modifier = Modifier.weight(1f).clickable { onEdit() }, 
            shape = RoundedCornerShape(16.dp), 
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)), 
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (activity.imageUrl != null) {
                            AsyncImage(model = activity.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Icon(
                                imageVector = when(activity.type) {
                                    "Transport" -> Icons.Default.DirectionsCar
                                    "Food" -> Icons.Default.Restaurant
                                    "Stay" -> Icons.Default.Hotel
                                    "Shopping" -> Icons.Default.ShoppingBag
                                    "Leisure" -> Icons.Default.BeachAccess
                                    "Sightseeing" -> Icons.Default.Museum
                                    else -> Icons.Default.Map
                                },
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = activity.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = activity.location, fontSize = 12.sp, color = GrayText)
                }
                if (canEdit) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesTabContentHighFidelity(
    trip: com.example.travelmate.domain.model.Trip?, 
    totalSpent: Double, 
    expenses: List<Expense>, 
    canEdit: Boolean,
    onAddClick: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        DetailCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = String.format(Locale.US, "%,.1fM đ", totalSpent / 1000000), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "total spent • ${expenses.size} transactions", fontSize = 11.sp, color = GrayText)
                }
                val remaining = (trip?.budgetAmount ?: 0.0) - totalSpent
                Text(text = String.format(Locale.US, "%,.1fM đ left", if (remaining > 0) remaining / 1000000 else 0.0), color = GreenPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if ((trip?.budgetAmount ?: 0.0) > 0) (totalSpent / trip!!.budgetAmount).toFloat() else 0f }, 
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = GreenPrimary,
                trackColor = GrayBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "By Category", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (canEdit) {
                Button(onClick = onAddClick, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text(" Thêm", color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (expenses.isEmpty()) {
            EmptyStateView(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                message = "Chưa có chi tiêu nào.",
                actionText = "Thêm chi tiêu",
                onAction = onAddClick
            )
        } else {
            val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> convertToVnd(exp.amount, exp.currency) } }
            val total = categoryTotals.values.sum()
            val categories = categoryTotals.keys.toList().take(4)
            val colors = listOf(OrangePrimary, BluePrimary, PurplePrimary, GreenPrimary)

            Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                categories.forEachIndexed { index, cat ->
                    val catAmount = categoryTotals[cat] ?: 0.0
                    val heightFactor = if (total > 0) (catAmount / total).toFloat() else 0.1f
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "${(heightFactor * 100).toInt()}%", fontSize = 10.sp, color = GrayText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(30.dp).height((heightFactor * 80).coerceAtLeast(10f).dp).clip(RoundedCornerShape(4.dp)).background(colors[index % colors.size]))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = cat, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            expenses.forEach { expense ->
                ExpenseListItem(
                    category = expense.category, 
                    title = expense.description, 
                    amount = String.format(Locale.US, "%,.0f %s", expense.amount, expense.currency), 
                    icon = Icons.Default.Payments,
                    canEdit = canEdit,
                    onDelete = { onDeleteExpense(expense.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PhotosTab(urls: List<String>, canEdit: Boolean, onAddPhotos: (List<String>) -> Unit, onDeletePhoto: (String) -> Unit, onPhotoClick: (String) -> Unit) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                onAddPhotos(uris.map { it.toString() })
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Gallery", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (canEdit) {
                Button(
                    onClick = { 
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }, 
                    shape = RoundedCornerShape(12.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Upload")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (urls.isEmpty()) {
            EmptyStateView(icon = Icons.Default.PhotoLibrary, message = "Chưa có ảnh nào trong bộ sưu tập.")
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(urls) { url ->
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        AsyncImage(
                            model = url, 
                            contentDescription = null, 
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GrayBackground)
                                .clickable { onPhotoClick(url) }, 
                            contentScale = ContentScale.Crop
                        )
                        if (canEdit) {
                            IconButton(
                                onClick = { onDeletePhoto(url) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String, actionText: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = GrayBorder)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = GrayText, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionText != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
fun SpendingDonutChart(modifier: Modifier, expenses: List<Expense>) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> convertToVnd(exp.amount, exp.currency) } }
    val total = categoryTotals.values.sum()
    val sortedEntries = categoryTotals.entries.sortedByDescending { it.value }
    
    Canvas(modifier = modifier) {
        drawArc(color = GrayBackground, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 25f, cap = StrokeCap.Round))
        
        if (total > 0) {
            val colors = listOf(OrangePrimary, BluePrimary, PurplePrimary, GreenPrimary, Color.Magenta)
            var startAngle = -90f

            sortedEntries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total * 360).toFloat()
                if (sweepAngle > 0) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 25f, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}

// Helper to match ViewModel's logic for UI charts
private fun convertToVnd(amount: Double, currency: String): Double {
    val rate = when (currency.uppercase()) {
        "USD" -> 25400.0
        "EUR" -> 27500.0
        "SGD" -> 18800.0
        "THB" -> 700.0
        else -> 1.0
    }
    return amount * rate
}

@Composable
fun BreakdownItem(color: Color, label: String, amount: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = GrayText, modifier = Modifier.weight(1f))
        Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ActionBtn(modifier: Modifier, icon: ImageVector, label: String, color: Color, tint: Color, onClick: () -> Unit) {
    Surface(modifier = modifier.height(60.dp), color = color, shape = RoundedCornerShape(12.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HeaderIcon(icon: ImageVector, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun WeatherDayItem(day: String, temp: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = day, fontSize = 12.sp, color = GrayText)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp).padding(vertical = 4.dp))
        Text(text = temp, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpenseListItem(category: String, title: String, amount: String, icon: ImageVector, canEdit: Boolean, onDelete: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)), shape = CircleShape, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = category, fontSize = 11.sp, color = GrayText)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (onDelete != null && canEdit) {
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityDialog(
    dayInfo: String,
    activity: TripActivity? = null,
    existingActivities: List<TripActivity> = emptyList(),
    canEdit: Boolean = true,
    onDismiss: () -> Unit, 
    onConfirm: (TripActivity, ByteArray?) -> Unit
) {
    var title by remember { mutableStateOf(activity?.title ?: "") }
    var time by remember { mutableStateOf(activity?.time ?: "09:00 SA") }
    var location by remember { mutableStateOf(activity?.location ?: "") }
    var note by remember { mutableStateOf(activity?.note ?: "") }
    var selectedType by remember { mutableStateOf(activity?.type ?: "Sightseeing") }
    var currentImageUrl by remember { mutableStateOf(activity?.imageUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    val types = listOf(
        "Sightseeing" to Icons.Default.Museum,
        "Food" to Icons.Default.Restaurant,
        "Activity" to Icons.Default.LocalActivity,
        "Transport" to Icons.Default.DirectionsCar,
        "Shopping" to Icons.Default.ShoppingBag,
        "Leisure" to Icons.Default.BeachAccess,
        "Stay" to Icons.Default.Hotel
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (activity == null) "Add Activity" else "Edit Activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(dayInfo, fontSize = 12.sp, color = GrayText)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.background)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Image picker for activity
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayBackground)
                        .then(if (canEdit) Modifier.clickable { 
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (currentImageUrl != null) {
                        AsyncImage(model = currentImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GrayText, modifier = Modifier.size(40.dp))
                            Text("Add Activity Image", color = GrayText, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("ACTIVITY TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText)
                Spacer(modifier = Modifier.height(12.dp))
                
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    types.forEach { pair ->
                        val type = pair.first
                        val icon = pair.second
                        val isSel = selectedType == type
                        Surface(
                            onClick = { if (canEdit) selectedType = type },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BluePrimary else GrayBorder.copy(alpha = 0.5f)),
                            color = if (isSel) BluePrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSel) BluePrimary else GrayText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(type, fontSize = 12.sp, color = if (isSel) BluePrimary else GrayText, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                DialogTextField(label = "TIME (HH:mm SA/CH)", value = time, onValueChange = { if (canEdit) { time = it; errorMessage = null } }, icon = Icons.Default.AccessTime)
                DialogTextField(label = "ACTIVITY NAME *", value = title, onValueChange = { if (canEdit) title = it }, placeholder = "e.g. Visit Ha Long Cave")
                DialogTextField(label = "LOCATION", value = location, onValueChange = { if (canEdit) location = it }, placeholder = "e.g. Ha Long Bay")
                DialogTextField(label = "NOTES", value = note, onValueChange = { if (canEdit) note = it }, placeholder = "Optional notes...")

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                if (canEdit) {
                    Button(
                        onClick = { 
                            if (title.isEmpty()) {
                                errorMessage = "Vui lòng nhập tên hoạt động"
                                return@Button
                            }

                            val newTimeMinutes = parseTime(time)
                            if (newTimeMinutes == -1) {
                                errorMessage = "Định dạng giờ không hợp lệ (vd: 09:00 SA)"
                                return@Button
                            }

                            val imageBytes = selectedImageUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }
                            val newActivity = (activity ?: TripActivity()).copy(
                                title = title,
                                time = time,
                                location = location,
                                type = selectedType,
                                note = note,
                                imageUrl = currentImageUrl
                            )
                            onConfirm(newActivity, imageBytes)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (activity == null) Icons.Default.Add else Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activity == null) "Add Activity" else "Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Text(text = "Chế độ xem chi tiết", color = GrayText, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

private fun parseTime(timeStr: String): Int {
    return try {
        val parts = timeStr.uppercase().split(" ")
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val isPm = parts[1] == "CH" || parts[1] == "PM"
        
        if (isPm && hour < 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        
        hour * 60 + minute
    } catch (e: Exception) {
        -1
    }
}

@Composable
fun DialogTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", icon: ImageVector? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = GrayText, fontSize = 14.sp) },
            trailingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = GrayText) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = GrayBorder.copy(alpha = 0.5f),
                focusedBorderColor = BluePrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
fun DeleteTripConfirmDialog(tripName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
        title = { Text(text = "Xóa chuyến đi?", fontWeight = FontWeight.Bold) },
        text = { Text(text = "Hành động này sẽ xóa vĩnh viễn \"$tripName\" và toàn bộ dữ liệu liên quan.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(12.dp)) {
                Text("Xóa")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Hủy")
            }
        }
    )
}
