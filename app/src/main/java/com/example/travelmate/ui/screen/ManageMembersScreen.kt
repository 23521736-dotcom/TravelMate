package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.travelmate.domain.model.TripMember
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.TripDetailViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    tripId: String,
    onBackClick: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(tripId) {
        viewModel.getTrip(tripId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý thành viên", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!viewModel.isOwner() && uiState.members.size > 1) {
                        TextButton(onClick = { viewModel.leaveTrip(onBackClick) }) {
                            Text("Rời nhóm", color = Color.Red)
                        }
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
        ) {
            // Invite Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(GrayBackground, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(text = "Mời bạn bè", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Mã mời: ${uiState.trip?.inviteCode ?: "---"}", fontWeight = FontWeight.Medium)
                        Text(text = "Dùng mã này để tham gia nhóm", fontSize = 12.sp, color = GrayText)
                    }
                    Button(
                        onClick = { 
                            uiState.trip?.inviteCode?.let { 
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "Thành viên (${uiState.members.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.members) { member ->
                    MemberItem(
                        member = member,
                        isCurrentUserOwner = viewModel.isOwner(),
                        onRoleChange = { newRole -> viewModel.updateMemberRole(member.userId, newRole) },
                        onTransferOwnership = { viewModel.transferOwnership(member.userId) },
                        onRemove = { viewModel.removeMember(member.userId) }
                    )
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    member: TripMember,
    isCurrentUserOwner: Boolean,
    onRoleChange: (String) -> Unit,
    onTransferOwnership: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTransferConfirm by remember { mutableStateOf(false) }

    if (showTransferConfirm) {
        AlertDialog(
            onDismissRequest = { showTransferConfirm = false },
            title = { Text("Chuyển quyền trưởng nhóm") },
            text = { Text("Bạn có chắc chắn muốn chuyển quyền trưởng nhóm cho ${member.userName}? Sau khi chuyển, bạn sẽ trở thành Biên tập viên.") },
            confirmButton = {
                Button(
                    onClick = {
                        onTransferOwnership()
                        showTransferConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = member.userPhotoUrl ?: "https://ui-avatars.com/api/?name=${member.userName}",
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = member.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (member.role == TripMember.ROLE_OWNER) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "Owner", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                }
            }
            Text(text = member.role.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, fontSize = 12.sp, color = GrayText)
        }

        if (isCurrentUserOwner && member.role != TripMember.ROLE_OWNER) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Set as Editor") },
                        onClick = { 
                            onRoleChange(TripMember.ROLE_EDITOR)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Set as Viewer") },
                        onClick = { 
                            onRoleChange(TripMember.ROLE_VIEWER)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Make Owner", color = OrangePrimary) },
                        onClick = { 
                            showTransferConfirm = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = OrangePrimary) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi nhóm", color = Color.Red) },
                        onClick = { 
                            onRemove()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
