package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.JoinTripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripScreen(
    onBackClick: () -> Unit,
    onJoinSuccess: (String) -> Unit,
    viewModel: JoinTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.joinedTripId) {
        uiState.joinedTripId?.let {
            onJoinSuccess(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tham gia chuyến đi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BluePrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Nhập mã mời",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            
            Text(
                text = "Nhập mã mời được chủ chuyến đi chia sẻ để tham gia cùng nhóm.",
                fontSize = 14.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = uiState.inviteCode,
                onValueChange = { viewModel.onInviteCodeChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mã mời (VD: HL7YYB)") },
                placeholder = { Text("Nhập mã tại đây...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = uiState.error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = GrayBorder
                )
            )
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.joinByCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Tham gia ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
