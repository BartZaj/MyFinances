package com.example.myfinances
import android.content.Context
import android.content.Intent

import android.net.ConnectivityManager
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myfinances.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
        val checkBox = preferences.getString("remember","")
        if(checkBox.equals("true"))
        {
            val intent = Intent(this, ExpensesChartXmlActivity::class.java)
            startActivity(intent)
        }

        binding.rememberMe.setOnCheckedChangeListener{ buttonView, isChecked->
            if(isChecked){
                val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString("remember","true")
                editor.apply();

            }
            else if(!isChecked)
            {
                val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString("remember","false")
                editor.apply();

            }

        }


        binding.redirectToReg.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.loginclick.setOnClickListener {
            val email = binding.textLogin.text.toString()
            val pass = binding.textLoginpassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if(isInternetAvailable()){
                    firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, ExpensesChartXmlActivity::class.java)
                            startActivity(intent)
                        } else {
                            when (val exception = task.exception) {
                                is FirebaseAuthInvalidUserException -> {
                                    // Invalid email format
                                    Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(this, exception?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()
                }
            } else{

                Toast.makeText(this,"Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
            }
        }

        // Add click listener for "devlog" button
        binding.devlog.setOnClickListener {
            val devEmail = "email@gmail.com"
            val devPassword = "password"
            if(isInternetAvailable()) {
                firebaseAuth.signInWithEmailAndPassword(devEmail, devPassword).addOnCompleteListener { task ->
                    if (task.isSuccessful) {


                        Toast.makeText(this, "DevLogin", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ExpensesChartXmlActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Handle login failure
                        Toast.makeText(this, "Failed to log in with developer credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()

            }
        }
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


}