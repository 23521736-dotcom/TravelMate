package com.example.travelmate.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.Category
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.screen.components.TransparentEditField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var displayName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<Category>()) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            photoUri = uri
            // We'll process bytes in the VM or here
        }
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(photoUri) {
        photoUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { 
                photoBytes = it.readBytes()
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.user?.displayName?.isNotEmpty() == true) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF9C27B0))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Photo Picker
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user?.fullName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = 8.dp, y = 8.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    IconButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Fields
            TransparentEditField(
                value = user?.fullName ?: "",
                onValueChange = {},
                placeholder = "Họ và tên",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            TransparentEditField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = "Tên hiển thị",
                modifier = Modifier.fillMaxWidth()
            )

            TransparentEditField(
                value = location,
                onValueChange = { location = it },
                placeholder = "Vị trí (vd: Hà Nội, Việt Nam)",
                modifier = Modifier.fillMaxWidth()
            )

            TransparentEditField(
                value = bio,
                onValueChange = { bio = it },
                placeholder = "Giới thiệu bản thân",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sở thích du lịch",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Category.values().forEach { category ->
                    val isSelected = category in selectedInterests
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedInterests = if (isSelected) selectedInterests - category else selectedInterests + category
                        },
                        label = { Text(category.displayName) },
                        leadingIcon = { 
                            Text(text = category.icon, fontSize = 14.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White,
                            selectedContainerColor = Color.White,
                            selectedLabelColor = BluePrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.White.copy(alpha = 0.3f),
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
                
                Button(
                    onClick = { 
                        if (photoBytes == null) {
                            viewModel.updateFullProfile(
                                name = user?.fullName ?: "",
                                displayName = displayName,
                                location = location,
                                bio = bio,
                                interests = selectedInterests.toList(),
                                photoBytes = null
                            )
                            // Trình điều khiển VM sẽ bắt lỗi nếu photoBytes null và ta muốn nó bắt buộc
                            // Nhưng tốt hơn là kiểm tra ở đây để báo lỗi cụ thể
                        }

                        val errorMsg = when {
                            photoBytes == null -> "Vui lòng tải lên ảnh đại diện"
                            displayName.isBlank() -> "Vui lòng nhập tên hiển thị"
                            location.isBlank() -> "Vui lòng nhập vị trí của bạn"
                            selectedInterests.isEmpty() -> "Vui lòng chọn ít nhất một sở thích"
                            else -> null
                        }

                        if (errorMsg != null) {
                            viewModel.setError(errorMsg)
                        } else {
                            viewModel.updateFullProfile(
                                name = user?.fullName ?: "",
                                displayName = displayName,
                                location = location,
                                bio = bio,
                                interests = selectedInterests.toList(),
                                photoBytes = photoBytes
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(24.dp))
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
                Text(text = uiState.error!!, color = Color.Yellow, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
