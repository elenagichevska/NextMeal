package com.example.nextmeal

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val auth = Firebase.auth

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegisterMode) "Create Profile 🍳" else "Welcome to NextMeal 🍉",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isRegisterMode) "Fill in your details below" else "Sign in to view the fridge",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("First Name and Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (isRegisterMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (isRegisterMode) {
                        if (fullName.isEmpty()) {
                            Toast.makeText(context, "Enter your name!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords don't match up!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val profileUpdates = userProfileChangeRequest { displayName = fullName }
                                    auth.currentUser?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener {
                                            Toast.makeText(context, "Sign up successful, welcome!", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Log in successful!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error in log in - please check again.", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(context, "Fill in all the boxes!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isRegisterMode) "Register" else "Log In")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Already have a profile? Log in!" else "Don't have a profile? Register here!")
        }
    }
}