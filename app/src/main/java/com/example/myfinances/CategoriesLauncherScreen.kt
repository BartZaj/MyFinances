package com.example.myfinances

import android.content.Intent
import android.os.Trace
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoriesLauncherScreen() {
    // Śledzenie dla CPU Profilera - osobny znacznik dla tego ekranu
    DisposableEffect(Unit) {
        Trace.beginSection("ComposeScreen_CategoriesLauncher")
        onDispose {
            Trace.endSection()
        }
    }

    val context = LocalContext.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Zarządzanie Kategoriami",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    val intent = Intent(context, ExpandableCategoriesActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Przejdź do Kategorii",
                    fontSize = 18.sp
                )
            }
        }
    }
}