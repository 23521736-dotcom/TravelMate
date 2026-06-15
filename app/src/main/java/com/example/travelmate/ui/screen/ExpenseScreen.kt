package com.example.travelmate.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ExpenseScreen(
    onAddExpenseClick: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showBudgetDialog) {
        SetBudgetDialog(
            currentAmount = uiState.monthlyBudget?.amount ?: 0.0,
            currentThreshold = uiState.monthlyBudget?.thresholdPercent ?: 80,
            error = uiState.error,
            onDismiss = { 
                showBudgetDialog = false
                viewModel.clearError() // Need to add this to ViewModel
            },
            onSave = { amount, threshold ->
                viewModel.saveBudget(amount, threshold)
            }
        )
    }

    // Close dialog on success
    LaunchedEffect(uiState.monthlyBudget) {
        if (showBudgetDialog) {
            showBudgetDialog = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val budget = uiState.monthlyBudget?.amount ?: 0.0
            val isOverBudget = budget > 0 && uiState.currentMonthTotalVnd >= budget

            FloatingActionButton(
                onClick = {
                    if (isOverBudget) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Đã vượt quá ngân sách tháng! Vui lòng tăng ngân sách trong mục Cài đặt.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        onAddExpenseClick()
                    }
                },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BluePrimary)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard Chi Tiêu",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%,.0f VND", uiState.totalSpentVnd),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Budget Settings", tint = Color.White)
                }
            }

            if (uiState.isLoading && uiState.expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Monthly Budget Card
                    item {
                        BudgetOverviewCard(
                            spent = uiState.currentMonthTotalVnd,
                            budget = uiState.monthlyBudget?.amount ?: 0.0,
                            threshold = uiState.monthlyBudget?.thresholdPercent ?: 80
                        )
                    }

                    // Chart Section
                    if (uiState.expenses.isNotEmpty()) {
                        item {
                            ExpenseChartCard(expenses = uiState.expenses)
                        }
                    }

                    // Recent Transactions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Giao dịch gần đây",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.expenses.size > 10) {
                                TextButton(onClick = { /* View all */ }) {
                                    Text("Xem tất cả", color = BluePrimary)
                                }
                            }
                        }
                    }
                    
                    if (uiState.expenses.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = GrayBackground.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Payments, contentDescription = null, tint = GrayText, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(text = "Chưa có dữ liệu chi tiêu", color = GrayText)
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.expenses.take(10)) { expense ->
                            GlobalExpenseItem(
                                category = expense.category,
                                description = expense.description,
                                amount = String.format(Locale.US, "%,.0f đ", expense.amount * (if (expense.currency == "VND") 1.0 else 25000.0)),
                                currency = expense.currency,
                                originalAmount = expense.amount,
                                icon = Icons.Default.Payments
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetOverviewCard(spent: Double, budget: Double, threshold: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Ngân sách tháng này", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            val hasBudget = budget > 0
            val progress = if (hasBudget) (spent / budget).toFloat().coerceIn(0f, 2f) else 0f
            val isOverThreshold = hasBudget && progress >= (threshold / 100f)
            val isOverBudget = hasBudget && progress > 1f

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = try { String.format(Locale.US, "%,.0f đ", spent) } catch (e: Exception) { "0 đ" },
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(text = "đã dùng", fontSize = 12.sp, color = GrayText)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasBudget) try { String.format(Locale.US, "%,.0f đ", budget) } catch (e: Exception) { "0 đ" } else "Unset",
                        fontSize = if (hasBudget) 16.sp else 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (hasBudget) MaterialTheme.colorScheme.onSurface else OrangePrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(text = "ngân sách", fontSize = 12.sp, color = GrayText)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = when {
                    isOverBudget -> Color.Red
                    isOverThreshold -> OrangePrimary
                    else -> BluePrimary
                },
                trackColor = GrayBackground.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (hasBudget) "${(progress * 100).toInt()}% đã dùng" else "Chưa thiết lập",
                    fontSize = 12.sp,
                    color = if (isOverBudget) Color.Red else GrayText
                )
                if (isOverThreshold && !isOverBudget) {
                    Text(text = "Sắp chạm ngưỡng!", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExpenseChartCard(expenses: List<Expense>) {
    val categoryTotals = getCategoryTotals(expenses)
    val total = categoryTotals.values.sum()
    val colors = listOf(BluePrimary, OrangePrimary, PurplePrimary, GreenPrimary, Color.Magenta, Color.Cyan)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Phân bổ chi tiêu",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DonutChart(modifier = Modifier.size(160.dp), expenses = expenses)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Tổng cộng", fontSize = 12.sp, color = GrayText)
                        Text(
                            text = String.format(Locale.US, "%,.0f đ", total),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }
                    sortedCategories.take(5).forEachIndexed { index, entry ->
                        val percent = (entry.value / total * 100).toInt()
                        ChartLegendItem(
                            color = colors[index % colors.size],
                            label = entry.key,
                            percent = "$percent%"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Detailed Breakdown
            Divider(color = GrayBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Chi tiết theo hạng mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GrayText)
            Spacer(modifier = Modifier.height(12.dp))
            
            categoryTotals.entries.sortedByDescending { it.value }.forEach { entry ->
                CategoryBreakdownRow(
                    category = entry.key,
                    amount = entry.value,
                    percent = entry.value / total
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CategoryBreakdownRow(category: String, amount: Double, percent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when(category) {
                            "Food" -> BluePrimary
                            "Accommodation" -> OrangePrimary
                            "Transport" -> GreenPrimary
                            "Activities" -> PurplePrimary
                            else -> GrayText
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = category, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = String.format(Locale.US, "%,.0f đ (%.0f%%)", amount, percent * 100),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DonutChart(modifier: Modifier, expenses: List<Expense>) {
    val categoryTotals = getCategoryTotals(expenses)
    val total = categoryTotals.values.sum()
    val colors = listOf(BluePrimary, OrangePrimary, PurplePrimary, GreenPrimary, Color.Magenta, Color.Cyan)

    Canvas(modifier = modifier) {
        drawArc(
            color = GrayBackground,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 35f, cap = StrokeCap.Round)
        )
        
        if (total > 0) {
            var startAngle = -90f
            categoryTotals.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total * 360).toFloat()
                if (sweepAngle > 0) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 35f, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String, percent: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text(text = percent, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
    }
}

@Composable
fun SetBudgetDialog(
    currentAmount: Double,
    currentThreshold: Int,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var amount by remember { mutableStateOf(currentAmount.toString()) }
    var threshold by remember { mutableStateOf(currentThreshold.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Cài đặt ngân sách tháng", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Ngân sách (VND)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Ngưỡng cảnh báo (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onSave(amount.toDoubleOrNull() ?: 0.0, threshold.toIntOrNull() ?: 80)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalExpenseItem(category: String, description: String, amount: String, currency: String, originalAmount: Double, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = when(category) {
                    "Accommodation" -> OrangeLight
                    "Food" -> BluePrimary.copy(alpha = 0.1f)
                    "Transport" -> GreenLight
                    else -> PurplePrimary.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(category) {
                            "Accommodation" -> Icons.Default.Hotel
                            "Food" -> Icons.Default.Restaurant
                            "Transport" -> Icons.Default.DirectionsCar
                            "Activities" -> Icons.Default.LocalActivity
                            else -> icon
                        },
                        contentDescription = null, 
                        tint = when(category) {
                            "Accommodation" -> OrangePrimary
                            "Food" -> BluePrimary
                            "Transport" -> GreenPrimary
                            else -> PurplePrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (description.isNotBlank()) description else category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = category,
                    fontSize = 12.sp,
                    color = GrayText
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currency != "VND") {
                    Text(
                        text = String.format(Locale.US, "%,.0f %s", originalAmount, currency),
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }
        }
    }
}

private fun getCategoryTotals(expenses: List<Expense>): Map<String, Double> {
    return expenses.groupBy { it.category }
        .mapValues { entry -> 
            entry.value.sumOf { exp ->
                val rate = when (exp.currency.uppercase()) {
                    "USD" -> 25400.0
                    "EUR" -> 27500.0
                    "SGD" -> 18800.0
                    "THB" -> 700.0
                    else -> 1.0
                }
                exp.amount * rate
            }
        }
}

private fun getCategoryBreakdown(expenses: List<Expense>): List<Pair<String, Color>> {
    val totals = getCategoryTotals(expenses)
    val sorted = totals.entries.sortedByDescending { it.value }
    val colors = listOf(BluePrimary, OrangePrimary, PurplePrimary, GreenPrimary, Color.Magenta, Color.Cyan)
    return sorted.mapIndexed { index, entry -> entry.key to colors[index % colors.size] }
}
