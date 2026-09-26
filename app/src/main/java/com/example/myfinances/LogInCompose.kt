package com.example.myfinances

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfinances.ui.theme.MyFinancesTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LogInCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyFinancesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            val intent = Intent(this, ExpensesChartActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onNavigateToRegister = {
                            val intent = Intent(this, SignUpCompose::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val preferences = remember { context.getSharedPreferences("checkbox", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(preferences.getString("remember", "") == "true") }

    // Automatyczne logowanie, jeśli wcześniej zaznaczono "Remember me"
    LaunchedEffect(Unit) {
        if (rememberMe) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp), // android:layout_margin="36dp"
            shape = RoundedCornerShape(36.dp), // app:cardCornerRadius="36dp"
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp), // app:cardElevation="20dp"
            colors = CardDefaults.cardColors(containerColor = Color.White) // Wymuszenie czystej bieli klasycznego CardView
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {

                // Ukryty przycisk devlog (lewy górny róg)
                Button(
                    onClick = {
                        val devEmail = "email@gmail.com"
                        val devPassword = "password"
                        if (isInternetAvailable(context)) {
                            firebaseAuth.signInWithEmailAndPassword(devEmail, devPassword).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "DevLogin", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, "Failed to log in with developer credentials", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "No internet connection!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .size(50.dp)
                ) { }

                // Główny układ centralny formularza
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp), // Zapas miejsca na dole karty
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Log in",
                        fontSize = 36.sp, // textSize="36dp" w XML
                        fontWeight = FontWeight.Bold, // textStyle="bold"
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(40.dp)) // layout_marginTop="40dp"

                    // Klasyczny wygląd EditText (tylko dolna linia)
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email") },
                        modifier = Modifier
                            .width(312.dp)
                            .height(56.dp), // Zbliżone do XML height="50dp" z uwzględnieniem minimalnego rozmiaru Compose
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp)) // layout_marginTop="10dp"

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .width(312.dp)
                            .height(56.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp)) // layout_marginTop="10dp"

                    // Checkbox z przesunięciem wzorowanym na XML
                    Row(
                        modifier = Modifier
                            .width(280.dp) // Zbliżona przestrzeń dla layout_marginRight="90dp"
                            .padding(start = 20.dp) // layout_marginLeft="20dp"
                            .height(45.dp), // layout_height="45dp"
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { isChecked ->
                                rememberMe = isChecked
                                preferences.edit().putString("remember", isChecked.toString()).apply()
                            },
                            modifier = Modifier.padding(start = 6.dp) // paddingLeft="6dp"
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 14.sp, // textSize="14dp"
                            fontWeight = FontWeight.Bold // textStyle="bold"
                        )
                    }

                    Spacer(modifier = Modifier.height(50.dp)) // layout_marginTop="50dp"

                    // Przycisk logowania
                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                if (isInternetAvailable(context)) {
                                    firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            onLoginSuccess()
                                        } else {
                                            when (val exception = task.exception) {
                                                is FirebaseAuthInvalidUserException -> {
                                                    Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                                                }
                                                else -> {
                                                    Toast.makeText(context, exception?.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No internet connection!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .width(312.dp)
                            .height(60.dp), // layout_width="312dp", layout_height="60dp"
                        shape = RoundedCornerShape(20.dp), // app:cornerRadius="20dp"
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74C69D)) // backgroundTint="#74c69d"
                    ) {
                        Text(
                            text = "Log in!",
                            color = Color.White,
                            fontSize = 18.sp // textSize="18sp"
                        )
                    }

                    // Tekst przekierowujący (odstępy wymuszone za pomocą padding)
                    Text(
                        text = "Not a user? Register here.",
                        fontSize = 18.sp, // textSize="18sp"
                        modifier = Modifier
                            .padding(top = 20.dp) // layout_margin="20dp" z XML
                            .clickable { onNavigateToRegister() }
                            .padding(8.dp) // padding="8dp" z XML - ten padding działa jak powiększenie klikalnego obszaru
                    )
                }
            }
        }
    }
}

// Zaktualizowana funkcja pomocnicza
fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}