package com.example.nextmeal

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.res.stringResource

@Composable
fun AuthScreen(
    onGoogleSignIn: () -> Unit,      // Action за Google најава
    onFacebookSignIn: () -> Unit,    // Action за Facebook најава
    onAuthEventLogged: (String) -> Unit // Додаден callback за Analytics
) {
    val contentResolver = LocalContext.current.contentResolver
    val context = LocalContext.current
    val auth = Firebase.auth

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var profileImageUriString by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    // Локализирани Toast пораки за користење внатре во onClick
    val msgEnterName = stringResource(id = R.string.toast_enter_name)
    val msgPasswordsMatch = stringResource(id = R.string.toast_passwords_dont_match)
    val msgSignUpSuccess = stringResource(id = R.string.toast_signup_success)
    val msgLoginSuccess = stringResource(id = R.string.toast_login_success)
    val msgLoginError = stringResource(id = R.string.toast_login_error)
    val msgFillAllFields = stringResource(id = R.string.toast_fill_all_fields)
    val msgGuestLoginSuccess = stringResource(id = R.string.toast_guest_login_success)

    // Лансер за избор на профилна слика од галерија
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            profileImageUriString = uri.toString()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()), // Спречува кршење на UI кога е отворена тастатурата
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegisterMode) stringResource(id = R.string.auth_title_register) else stringResource(
                    id = R.string.auth_title_login
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRegisterMode) stringResource(id = R.string.auth_subtitle_register) else stringResource(
                    id = R.string.auth_subtitle_login
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Аватар / Слика за избор (Само во Register Mode)
            if (isRegisterMode) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            profileImageLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUriString.isNotEmpty()) {
                        AsyncImage(
                            model = profileImageUriString,
                            contentDescription = stringResource(id = R.string.cd_profile_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = stringResource(id = R.string.btn_add_photo),
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(id = R.string.btn_add_photo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isRegisterMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(stringResource(id = R.string.hint_full_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(id = R.string.hint_confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Копче за класична најава / регистрација со е-маил
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        if (isRegisterMode) {
                            if (fullName.isEmpty()) {
                                Toast.makeText(context, msgEnterName, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, msgPasswordsMatch, Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val profileUpdates = userProfileChangeRequest {
                                            displayName = fullName
                                            if (profileImageUriString.isNotEmpty()) {
                                                photoUri = profileImageUriString.toUri()
                                            }
                                        }
                                        auth.currentUser?.updateProfile(profileUpdates)
                                            ?.addOnCompleteListener {
                                                onAuthEventLogged("email_signup_success") // Analytics
                                                Toast.makeText(
                                                    context,
                                                    msgSignUpSuccess,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error: ${task.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        } else {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onAuthEventLogged("email_login_success") // Analytics
                                        Toast.makeText(context, msgLoginSuccess, Toast.LENGTH_SHORT)
                                            .show()
                                    } else {
                                        Toast.makeText(context, msgLoginError, Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(context, msgFillAllFields, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isRegisterMode) stringResource(id = R.string.btn_register_email) else stringResource(
                        id = R.string.btn_login_email
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode) stringResource(id = R.string.switch_to_login) else stringResource(
                        id = R.string.switch_to_register
                    )
                )
            }

            // --- ЛИНИЈА ЗА ПОДЕЛБА НА СОЦИЈАЛНИ МРЕЖИ ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = stringResource(id = R.string.or_connect_with),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // 🌟 КОПЧИЊАТА ЕДНО ДО ДРУГО (СПАКУВАНИ ВО ROW)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- ГУГЛ НАЈАВА ---
                OutlinedButton(
                    onClick = {
                        onGoogleSignIn()
                        onAuthEventLogged("google_login_attempt") // Analytics
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface, containerColor = MaterialTheme.colorScheme.surface )

                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = stringResource(id = R.string.cd_google_logo),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(id = R.string.google_sign_in),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // --- ФЕЈСБУК НАЈАВА ---
                OutlinedButton(
                    onClick = {
                        onFacebookSignIn()
                        onAuthEventLogged("facebook_login_attempt") // Analytics
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface, containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            contentDescription = stringResource(id = R.string.cd_facebook_logo),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(id = R.string.facebook_sign_in),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(id = R.string.divider_guest_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            ElevatedButton(
                onClick = {
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onAuthEventLogged("guest_login_success") // Analytics
                                Toast.makeText(context, msgGuestLoginSuccess, Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Guest login failed: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_anonymous),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}