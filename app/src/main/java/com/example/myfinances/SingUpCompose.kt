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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.FirebaseDatabase

class SignUpCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyFinancesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignUpScreen(
                        onRegisterSuccess = {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onNavigateToLogin = {
                            val intent = Intent(this, LogInCompose::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignUpScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            colors = CardDefaults.cardColors(containerColor = Color.White) // Wymuszenie bieli
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Sign Up",
                    fontSize = 36.sp, // textSize="36dp"
                    fontWeight = FontWeight.Bold, // textStyle="bold"
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp)) // layout_marginTop="10dp" dla emaila (względne)

                // Klasyczny wygląd EditText (tylko dolna linia)
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
                    modifier = Modifier
                        .width(312.dp)
                        .height(56.dp), // Zbliżone do height="50dp"
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

                Spacer(modifier = Modifier.height(50.dp)) // layout_marginTop="50dp"

                // Przycisk rejestracji
                Button(
                    onClick = {
                        if (!isValidEmail(email)) {
                            Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            if (isNetworkAvailable(context)) {
                                val firebaseRef = FirebaseDatabase.getInstance().getReference("Users")
                                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = firebaseAuth.currentUser
                                        user?.let {
                                            val uid = it.uid
                                            firebaseRef.child(uid).setValue("a")
                                            onRegisterSuccess()
                                        }
                                    } else {
                                        when (val exception = task.exception) {
                                            is FirebaseAuthInvalidCredentialsException -> {
                                                Toast.makeText(context, "Password is too weak", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Empty Fields Are not Allowed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .width(312.dp)
                        .height(60.dp), // layout_width="312dp", layout_height="60dp"
                    shape = RoundedCornerShape(20.dp), // app:cornerRadius="20dp"
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74C69D)) // backgroundTint="#74c69d"
                ) {
                    Text(
                        text = "Sign Up!",
                        color = Color.White,
                        fontSize = 18.sp // textSize="18sp"
                    )
                }

                // Tekst przekierowujący (odstępy wzorowane na XML)
                Text(
                    text = "Alredy a user? Log in!",
                    fontSize = 18.sp, // textSize="18sp"
                    modifier = Modifier
                        .padding(top = 10.dp) // layout_margin="10dp"
                        .clickable { onNavigateToLogin() }
                        .padding(8.dp) // padding="8dp" - powiększa klikalny obszar
                )
            }
        }
    }
}

// Funkcje pomocnicze
private fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})")
    return email.matches(emailRegex)
}

// Zmiana nazwy na isNetworkAvailable, aby uniknąć konfliktu z top-level funkcją z LogInActivity
private fun isNetworkAvailable(context: Context): Boolean {
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