package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.CarDiagApp
import com.example.myapplication.ui.viewmodels.LineType
import com.example.myapplication.ui.viewmodels.TerminalViewModel
import com.example.myapplication.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CarDiagApp
    val viewModel: TerminalViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TerminalViewModel(app.obdBluetoothManager) as T
            }
        }
    )

    val history by viewModel.history.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickCommands = listOf("ATZ", "ATSP0", "ATRV", "0100", "0105", "010C", "010D", "03", "04")

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.open_terminal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.clear))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            // History Console
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                items(history) { line ->
                    val color = when (line.type) {
                        LineType.COMMAND -> Color(0xFF00E5FF)
                        LineType.RESPONSE -> Color(0xFF00E676)
                        LineType.ERROR -> Color(0xFFFF5252)
                    }
                    Text(
                        text = line.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Quick Commands Row
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Surface(
                        onClick = { viewModel.sendCommand(cmd) },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(cmd, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.enter_command), fontSize = 14.sp) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    enabled = !isSending,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                    } else {
                        FloatingActionButton(
                            onClick = {
                                viewModel.sendCommand(commandText)
                                commandText = ""
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }
}
