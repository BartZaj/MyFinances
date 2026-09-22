package com.example.myfinances

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myfinances.ui.theme.MyFinancesTheme
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFinancesTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("products")

            // Dodanie przykładowego wpisu
            val product = mapOf("name" to "Pomarańcza", "price" to 4.2)
            ref.child("1").setValue(product).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "Dane zapisane pomyślnie!")
                } else {
                    Log.e("Firebase", "Błąd podczas zapisywania: ${task.exception}")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyFinancesTheme {
        Greeting("Android")
    }
}