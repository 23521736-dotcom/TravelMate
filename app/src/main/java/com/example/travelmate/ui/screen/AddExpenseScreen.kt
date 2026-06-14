package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AddExpenseViewModel
import com.example.travelmate.ui.viewmodel.TripDetailViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    tripId: String?,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
    tripDetailViewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tripDetailState by tripDetailViewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Fetch trip detail to check permissions if tripId is provided
    LaunchedEffect(tripId) {
        if (tripId != null) {
            tripDetailViewModel.getTrip(tripId)
        }
    }

    // Permission check
    val canEdit = remember(tripId, tripDetailState.currentUserMember) {
        if (tripId == null) true // Personal expense
        else {
            val role = tripDetailState.currentUserMember?.role
            role == com.example.travelmate.domain.model.TripMember.ROLE_OWNER || 
            role == com.example.travelmate.domain.model.TripMember.ROLE_EDITOR
        }
    }
    var amount by remember { mutableStateOf("0") }
    var selectedCurrency by remember { mutableStateOf("VND") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var description by remember { mutableStateOf("") }
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    var dateInMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val suggestionsMap = mapOf(
        "Food" to listOf("Ăn trưa", "Ăn tối", "Cà phê", "Trà sữa", "Bữa phụ"),
        "Transport" to listOf("Grab/Taxi", "Vé máy bay", "Xăng xe", "Thuê xe", "Vé xe khách"),
        "Stay" to listOf("Khách sạn", "Homestay", "Resort", "AirBnB", "Ký túc xá"),
        "Activities" to listOf("Vé tham quan", "Tour du lịch", "Trò chơi", "Lặn biển", "Leo núi"),
        "Shopping" to listOf("Quà lưu niệm", "Quần áo", "Đặc sản", "Mỹ phẩm", "Đồ điện tử")
    )

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSaveClick()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateInMillis = datePickerState.selectedDateMillis ?: dateInMillis
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick, 
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = "Thêm chi tiêu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Button(
                onClick = { 
                    keyboardController?.hide()
                    viewModel.addExpense(tripId, amount, selectedCurrency, selectedCategory, description, dateFormatter.format(Date(dateInMillis)))
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading && canEdit
            ) {
                Text(text = "Lưu", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Amount Section
        // ... (keep amount section as is, maybe tweak quick buttons)
        Surface(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "SỐ TIỀN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (selectedCurrency == "VND") "₫" else "$", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GrayText)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 42.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.width(220.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Currency Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VND", "USD", "EUR", "THB", "SGD").forEach { curr ->
                        val isSel = curr == selectedCurrency
                        Surface(
                            onClick = { selectedCurrency = curr },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) OrangePrimary else MaterialTheme.colorScheme.surface,
                            border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)) else null
                        ) {
                            Text(
                                text = curr,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = if (isSel) Color.White else GrayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Amount Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("50K", "100K", "200K", "500K", "1M", "2M").forEach { label ->
                        val value = when(label) {
                            "50K" -> "50000"
                            "100K" -> "100000"
                            "200K" -> "200000"
                            "500K" -> "500000"
                            "1M" -> "1000000"
                            "2M" -> "2000000"
                            else -> "0"
                        }
                        Surface(
                            onClick = { amount = value },
                            shape = RoundedCornerShape(16.dp),
                            color = if (amount == value) OrangePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (amount == value) OrangePrimary else GrayBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (amount == value) OrangePrimary else GrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Category Section
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = "Hạng mục *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            val categories = listOf(
                "Food" to Icons.Default.Restaurant,
                "Transport" to Icons.Default.DirectionsCar,
                "Stay" to Icons.Default.Hotel,
                "Activities" to Icons.Default.ConfirmationNumber,
                "Shopping" to Icons.Default.ShoppingBag,
                "Health" to Icons.Default.MedicalServices,
                "SIM/Data" to Icons.Default.SimCard,
                "Other" to Icons.Default.Help
            )

            for (i in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (j in 0 until 4) {
                        val index = i * 4 + j
                        if (index < categories.size) {
                            val cat = categories[index]
                            CategoryIconItem(
                                name = cat.first,
                                icon = cat.second,
                                isSelected = selectedCategory == cat.first,
                                onClick = { selectedCategory = cat.first },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Bạn đã chi tiêu vào việc gì?") },
                label = { Text("MÔ TẢ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = GrayBorder.copy(alpha = 0.5f), 
                    focusedBorderColor = OrangePrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Suggestions
            val currentSuggestions = suggestionsMap[selectedCategory] ?: emptyList()
            if (currentSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentSuggestions) { suggest ->
                        SuggestionChip(
                            onClick = { description = suggest },
                            label = { Text(suggest) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = OrangePrimary.copy(alpha = 0.05f),
                                labelColor = OrangePrimary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                borderColor = OrangePrimary.copy(alpha = 0.2f),
                                enabled = true
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dateFormatter.format(Date(dateInMillis)),
                onValueChange = { },
                label = { Text("NGÀY CHI TIÊU", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                trailingIcon = { 
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangePrimary) 
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = GrayBorder.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (uiState.error != null) {
                Text(text = uiState.error!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }

            if (!canEdit) {
                Text(
                    text = "Bạn không có quyền thêm chi tiêu cho chuyến đi này.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    keyboardController?.hide()
                    viewModel.addExpense(tripId, amount, selectedCurrency, selectedCategory, description, dateFormatter.format(Date(dateInMillis)))
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = !uiState.isLoading && canEdit
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Lưu chi tiêu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CategoryIconItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) OrangePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) else androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isSelected) OrangePrimary else GrayText, modifier = Modifier.size(28.dp))
            }
        }
        Text(text = name, fontSize = 11.sp, color = if (isSelected) OrangePrimary else GrayText, modifier = Modifier.padding(top = 6.dp), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
