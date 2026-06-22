package com.example.travelmate.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.Category
import com.example.travelmate.ui.screen.components.TransparentEditField
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }
    
    val selectedInterests = remember { mutableStateListOf<Category>() }

    // Bắt đầu sạch sẽ
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Chỉ nạp dữ liệu một lần duy nhất khi user data đã tải xong từ Database
    LaunchedEffect(user) {
        if (user != null && name.isEmpty()) {
            name = user.fullName ?: ""
            displayName = user.displayName ?: ""
            bio = user.bio ?: ""
            location = user.location ?: ""
            selectedInterests.clear()
            selectedInterests.addAll(user.interests)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            android.util.Log.d("EditProfileScreen", "Save success, navigating back")
            onSaveSuccess()
            viewModel.clearSuccessState()
        }
    }

    Scaffold(
        snackbarHost = { 
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Hiển thị lỗi bằng log để debug nếu cần
                    android.util.Log.e("EditProfileScreen", "UI Error: $error")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BlueHeader, Color(0xFF6A1B9A))
                    )
                )
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image with Edit Icon
                Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { 
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White, RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (user?.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = name.firstOrNull()?.toString() ?: "U",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Custom TextFields
                TransparentEditField(value = name, onValueChange = { name = it }, placeholder = "Họ và tên")
                TransparentEditField(value = displayName, onValueChange = { displayName = it }, placeholder = "Tên hiển thị")
                TransparentEditField(value = location, onValueChange = { location = it }, placeholder = "Vị trí (vd: Hà Nội, Việt Nam)")
                TransparentEditField(value = bio, onValueChange = { bio = it }, placeholder = "Giới thiệu bản thân", singleLine = false)

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "Sở thích du lịch", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                // Selectable Category Chips
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = BluePrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "${category.icon} ${category.displayName}",
                                    fontSize = 12.sp, 
                                    color = if (isSelected) BluePrimary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            android.util.Log.d("EditProfileScreen", "Save button clicked")
                            keyboardController?.hide()
                            val photoBytes = selectedImageUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }
                            viewModel.updateFullProfile(name, displayName, location, bio, selectedInterests.toList(), photoBytes)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save", color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lỗi: ${uiState.error}", 
                        color = Color.Yellow, 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}
