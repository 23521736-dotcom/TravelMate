package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.domain.model.Category
import com.example.travelmate.ui.screen.components.TransparentEditField
import com.example.travelmate.ui.theme.BlueHeader
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestSelectionScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var name by remember { mutableStateOf("") }
     var displayName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    val selectedInterests = remember { mutableStateListOf<Category>() }

    // Initialize fields from current user data if available
    LaunchedEffect(user) {
        if (user != null && name.isEmpty()) {
            name = user.fullName ?: ""
            displayName = user.displayName ?: ""
            location = user.location ?: ""
            bio = user.bio ?: ""
            if (selectedInterests.isEmpty()) {
                selectedInterests.addAll(user.interests)
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.completeOnboarding()
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BlueHeader, Color(0xFF6A1B9A))
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Thiết lập hồ sơ",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Hãy cho chúng tôi biết thêm về bạn để có trải nghiệm cá nhân hóa.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Profile Fields
        TransparentEditField(value = name, onValueChange = { name = it }, placeholder = "Họ và tên")
        TransparentEditField(value = displayName, onValueChange = { displayName = it }, placeholder = "Tên hiển thị")
        TransparentEditField(value = location, onValueChange = { location = it }, placeholder = "Vị trí (vd: Hà Nội, Việt Nam)")
        TransparentEditField(value = bio, onValueChange = { bio = it }, placeholder = "Giới thiệu bản thân", singleLine = false)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Sở thích du lịch (chọn ít nhất 3)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Category.entries.forEach { category ->
                val isSelected = selectedInterests.contains(category)
                Surface(
                    onClick = {
                        if (isSelected) selectedInterests.remove(category)
                        else selectedInterests.add(category)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check, 
                                contentDescription = null, 
                                modifier = Modifier.size(14.dp), 
                                tint = BluePrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = category.displayName,
                            fontSize = 12.sp,
                            color = if (isSelected) BluePrimary else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.updateFullProfile(
                    name = name,
                    displayName = displayName,
                    location = location,
                    bio = bio,
                    interests = selectedInterests.toList()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedInterests.size >= 3 && name.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary)
            } else {
                Text(
                    text = "Hoàn tất",
                    color = BluePrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
