package com.example.myfinances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.security.identity.PersonalizationData
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myfinances.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUpActivity : AppCompatActivity() {

        private lateinit var binding: ActivitySignupBinding
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var firebaseRef: DatabaseReference

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                binding = ActivitySignupBinding.inflate(layoutInflater)
                setContentView(binding.root)

                firebaseAuth = FirebaseAuth.getInstance()

                binding.redirectToLog.setOnClickListener {
                        val intent2 = Intent(this, LogInActivity::class.java)
                        startActivity(intent2)
                }

                binding.registerclick.setOnClickListener {
                        val email = binding.textEmail.text.toString()
                        val pass = binding.textRegisterpassword.text.toString()

                        // Validate email format
                        if (!isValidEmail(email)) {
                                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                        }
                        firebaseRef = FirebaseDatabase.getInstance().getReference("Users")
                        if (email.isNotEmpty() && pass.isNotEmpty()) {
                                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                                val user = firebaseAuth.currentUser
                                                user?.let {
                                                        val uid = user.uid // Get the UID of the newly registered user
                                                        firebaseRef.child(uid).setValue("a") // Set the data under UID branch
                                                        val intent = Intent(this, MainActivity::class.java)
                                                        startActivity(intent)
                                                }
                                        } else {
                                                val exception = task.exception
                                                if (exception is FirebaseAuthInvalidCredentialsException) {
                                                        // Password doesn't meet the Firebase standards
                                                        Toast.makeText(this, "Password is too weak", Toast.LENGTH_SHORT).show()
                                                } else {
                                                        // Other errors, such as network issues, server error, etc.
                                                        Toast.makeText(this, exception?.message, Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                }
                        } else {
                                Toast.makeText(this, "Empty Fields Are not Allowed", Toast.LENGTH_SHORT).show()
                        }
                }
        }

        private fun isValidEmail(email: String): Boolean {
                val emailRegex = Regex("^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})")
                return email.matches(emailRegex)
        }
}