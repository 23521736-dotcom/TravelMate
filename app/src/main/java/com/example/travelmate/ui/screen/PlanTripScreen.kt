package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.PlanTripViewModel
import com.example.travelmate.ui.screen.components.showDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PlanTripScreen(
    onBackClick: () -> Unit,
    onComplete: (String) -> Unit,
    destinationId: String? = null,
    viewModel: PlanTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.createdTripId != null) {
            onComplete(uiState.createdTripId!!)
        }
    }

    LaunchedEffect(destinationId) {
        if (destinationId != null && uiState.destination.isEmpty()) {
            viewModel.getDestinationById(destinationId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentStep > 1) currentStep-- else onBackClick() },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "Lên kế hoạch mới", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Bước $currentStep / 4", fontSize = 12.sp, color = GrayText)
            }
        }

        // Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val stepNum = index + 1
                StepIndicator(
                    number = stepNum,
                    isActive = currentStep == stepNum,
                    isCompleted = currentStep > stepNum,
                    label = when(stepNum) {
                        1 -> "Thông tin"
                        2 -> "Ngày đi"
                        3 -> "Ngân sách"
                        else -> "Hoàn tất"
                    }
                )
                if (index < 3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (currentStep > stepNum) BluePrimary else GrayBorder)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                1 -> StepDetails(
                    uiState.tripName, { viewModel.onTripNameChange(it) },
                    uiState.destination, { viewModel.onDestinationQueryChange(it) },
                    uiState.description, { viewModel.onDescriptionChange(it) },
                    uiState.filteredDestinations, { viewModel.selectDestination(it) },
                    uiState.coverImageUrl
                )
                2 -> {
                    val context = LocalContext.current
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
                    val minEndDate = remember(uiState.startDate) {
                        try {
                            if (uiState.startDate.isNotEmpty()) {
                                // For "strictly greater", we can set minDate to start date + 1 day
                                // However, DatePicker's minDate usually includes that date.
                                // If we want strictly greater, we should add 24 hours.
                                val startTime = sdf.parse(uiState.startDate)?.time ?: System.currentTimeMillis()
                                startTime + (24 * 60 * 60 * 1000)
                            } else System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    }
                    
                    StepDates(
                        uiState.startDate.ifEmpty { "Chọn ngày bắt đầu" }, 
                        { showDatePicker(context, System.currentTimeMillis()) { viewModel.onDatesChange(it, uiState.endDate) } }, 
                        uiState.endDate.ifEmpty { "Chọn ngày kết thúc" }, 
                        { showDatePicker(context, minEndDate) { viewModel.onDatesChange(uiState.startDate, it) } },
                        uiState.durationDays
                    )
                }
                3 -> StepBudget(
                    uiState.currency, { viewModel.onCurrencyChange(it) }, 
                    uiState.budgetAmount, { viewModel.onBudgetChange(it) }, 
                    uiState.budgetLevel, { viewModel.onBudgetLevelChange(it) }
                )
                4 -> StepReview(uiState.tripName, uiState.destination, uiState.startDate, uiState.endDate, uiState.budgetAmount, uiState.currency, uiState.coverImageUrl)
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder)
                ) {
                    Text(text = "Quay lại", color = GrayText)
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(if (currentStep > 1) 2f else 1f).height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else {
                Button(
                    onClick = {
                        if (currentStep < 4) currentStep++
                        else viewModel.createTrip()
                    },
                    modifier = Modifier.weight(if (currentStep > 1) 2f else 1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 4) GreenPrimary else BluePrimary
                    )
                ) {
                    Text(
                        text = if (currentStep == 4) "Tạo chuyến đi" else "Tiếp tục",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (currentStep < 4) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StepIndicator(number: Int, isActive: Boolean, isCompleted: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive || isCompleted) BluePrimary else GrayBackground)
                .border(1.dp, if (isActive || isCompleted) BluePrimary else GrayBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = number.toString(),
                    color = if (isActive) Color.White else GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(text = label, fontSize = 10.sp, color = if (isActive || isCompleted) BluePrimary else GrayText)
    }
}

@Composable
fun StepDetails(
    name: String, onNameChange: (String) -> Unit,
    destQuery: String, onDestQueryChange: (String) -> Unit,
    desc: String, onDescChange: (String) -> Unit,
    suggestions: List<Destination>,
    onSelect: (Destination) -> Unit,
    coverImageUrl: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Ảnh bìa chuyến đi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, GrayBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GrayText, modifier = Modifier.size(40.dp))
                    Text(text = "Sẽ hiển thị ảnh theo địa điểm chọn", color = GrayText, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PlanTextField(label = "Tên chuyến đi *", value = name, onValueChange = onNameChange, placeholder = "vd: Khám phá Hà Nội", icon = Icons.Default.FlightTakeoff)
        
        // Destination Search with Suggestions
        Column {
            PlanTextField(label = "Địa điểm đến *", value = destQuery, onValueChange = onDestQueryChange, placeholder = "Tìm địa danh (vd: Chùa Một Cột)...", icon = Icons.Default.LocationOn)
            
            if (suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn {
                        items(suggestions) { dest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(dest) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = dest.imageUrls.firstOrNull(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = dest.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = dest.location, color = GrayText, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = GrayBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PlanTextField(label = "Mô tả (tùy chọn)", value = desc, onValueChange = onDescChange, placeholder = "Chuyến đi này có gì đặc biệt?", icon = Icons.Default.Description, singleLine = false)
    }
}

@Composable
fun StepDates(start: String, onStartClick: () -> Unit, end: String, onEndClick: () -> Unit, duration: Int) {
    Column(modifier = Modifier.padding(24.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Chọn ngày hành trình", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                DateSelectField(label = "NGÀY BẮT ĐẦU *", value = start, onClick = onStartClick)
                Spacer(modifier = Modifier.height(16.dp))
                DateSelectField(label = "NGÀY KẾT THÚC *", value = end, onClick = onEndClick)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "$duration ngày", fontWeight = FontWeight.Bold, color = BluePrimary)
                            Text(text = "Thời gian hành trình", fontSize = 12.sp, color = BluePrimary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepBudget(currency: String, onCurrencyChange: (String) -> Unit, amount: String, onAmountChange: (String) -> Unit, level: String, onLevelChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Thiết lập ngân sách", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VND", "USD", "EUR").forEach { curr ->
                        val isSel = curr == currency
                        Surface(
                            onClick = { onCurrencyChange(curr) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) OrangePrimary else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) OrangePrimary else GrayBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = curr,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (isSel) Color.White else GrayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = if (currency == "VND") "₫" else "$", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GrayText)
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = amount,
                            onValueChange = onAmountChange,
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Gợi ý mức chi tiêu", fontSize = 14.sp, color = GrayText)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetTemplate(
                        "Tiết kiệm", "< 3M", GreenLight, GreenPrimary, level == "Budget", { onLevelChange("Budget") }, modifier = Modifier.weight(1f)
                    )
                    BudgetTemplate(
                        "Trung bình", "3-10M", Color(0xFFE3F2FD), BluePrimary, level == "Mid-range", { onLevelChange("Mid-range") }, modifier = Modifier.weight(1f)
                    )
                    BudgetTemplate(
                        "Sang chảnh", "> 10M", OrangeLight, OrangePrimary, level == "Luxury", { onLevelChange("Luxury") }, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StepReview(name: String, dest: String, start: String, end: String, budget: String, currency: String, coverImageUrl: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    AsyncImage(
                        model = coverImageUrl.takeIf { it.isNotEmpty() } ?: "https://images.unsplash.com/photo-1583417319070-4a69db38a482",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                }
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = name.ifEmpty { "Tên chuyến đi" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ReviewItem(icon = Icons.Default.LocationOn, label = "Điểm đến", value = dest.ifEmpty { "Chưa chọn" })
                    ReviewItem(icon = Icons.Default.CalendarToday, label = "Thời gian", value = "$start - $end")
                    ReviewItem(icon = Icons.Default.Payments, label = "Ngân sách", value = String.format(Locale.US, "%,.0f %s", budget.toDoubleOrNull() ?: 0.0, currency))
                }
            }
        }
    }
}

@Composable
fun PlanTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, icon: ImageVector, singleLine: Boolean = true) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = GrayText) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = GrayText) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = GrayBorder.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun DateSelectField(label: String, value: String, onClick: () -> Unit = {}) {
    Column {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BudgetTemplate(label: String, range: String, bg: Color, border: Color, isSel: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, border) else null
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = border)
            Text(text = range, fontSize = 10.sp, color = border.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ReviewItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.background, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = GrayText)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = GrayText)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


