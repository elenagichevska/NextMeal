package com.example.nextmeal

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    recipeDao: RecipeDao,
    localRecipes: List<LocalRecipe>,
    db: FirebaseFirestore
) {
    val contentResolver = LocalContext.current.contentResolver
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 👤 Состојби за Firebase профилот на корисникот
    val currentUser = Firebase.auth.currentUser

    // 🌟 КЛУЧНА ПРОВЕРКА: Дали корисникот е Анонимен (Гост)?
    val isAnonymous = currentUser?.isAnonymous == true

    var currentUserName by remember { mutableStateOf(currentUser?.displayName ?: "User") }
    val currentEmail = currentUser?.email ?: ""
    val currentUid = currentUser?.uid ?: ""

    var profileImageUriString by remember { mutableStateOf(currentUser?.photoUrl?.toString() ?: "") }

    // Контрола на екрани
    var isEditingProfile by rememberSaveable { mutableStateOf(false) }
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    // Состојби за внес на профил инфо
    var nameInput by remember { mutableStateOf(currentUserName) }
    var newProfileImageUri by remember { mutableStateOf(profileImageUriString) }

    // Состојби за внес / уредување на рецепти
    var titleInput by rememberSaveable { mutableStateOf("") }
    var ingredientsInput by rememberSaveable { mutableStateOf("") }
    var instructionsInput by rememberSaveable { mutableStateOf("") }
    var imageUriString by rememberSaveable { mutableStateOf("") }
    var isPublicChecked by rememberSaveable { mutableStateOf(false) }

    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var editingRecipeId by rememberSaveable { mutableStateOf<Int?>(null) }

    var recipeToDelete by remember { mutableStateOf<LocalRecipe?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Лансери за слики
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            imageUriString = uri.toString()
        }
    }

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
            newProfileImageUri = uri.toString()
        }
    }

    // Дијалози за потврда
    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Delete Recipe?") },
            text = { Text("Are you sure you want to permanently delete '${recipeToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val recipe = recipeToDelete
                        if (recipe != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                recipeDao.deleteRecipe(recipe)
                            }
                            if (recipe.isPublic) {
                                db.collection("public_recipes")
                                    .whereEqualTo("title", recipe.title)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        for (doc in documents) {
                                            db.collection("public_recipes").document(doc.id).delete()
                                        }
                                    }
                            }
                            Toast.makeText(context, "Recipe deleted successfully!", Toast.LENGTH_SHORT).show()
                        }
                        recipeToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { recipeToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("Are you absolutely sure you want to delete your profile? All data will be wiped out permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Account deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") } }
        )
    }

    // 🔀 МЕНАЏИРАЊЕ НА ЕКРАНИТЕ И РЕСТРИКЦИИТЕ
    if (isAnonymous) {
        // --- 🧑‍🍳 ЕКРАН ЗА ГОСТ (ANONYMOUS) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Guest",
                    modifier = Modifier.size(64.dp).padding(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You are browsing as a Guest 🍽️",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create a profile or log in to unleash full features like adding your own recipes and custom profiles!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    Firebase.auth.signOut() // Го одјавуваме анонимниот гост, со што автоматски се отвора AuthScreen
                    Toast.makeText(context, "Redirecting to Login...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Sign In / Create Account 🍳")
            }
        }
    } else if (isEditingProfile) {
        // --- ✏️ ЕКРАН ЗА ЕДИТ НА ПРОФИЛ (СТАНДАРДЕН КОРИСНИК) ---
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isEditingProfile = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { profileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (newProfileImageUri.isNotEmpty()) {
                    AsyncImage(
                        model = newProfileImageUri,
                        contentDescription = "New Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Person, contentDescription = "Avatar", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(30.dp),
                    color = MaterialTheme.colorScheme.blackWithAlpha
                ) {
                    Text("CHANGE", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (nameInput.isNotEmpty()) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(nameInput)
                            .setPhotoUri(newProfileImageUri.toUri())
                            .build()

                        currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                currentUserName = nameInput
                                profileImageUriString = newProfileImageUri
                                isEditingProfile = false
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account Permanently", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // --- 🏠 ГЛАВЕН ПРОФИЛ ЕКРАН ЗА РЕЦЕПТИ (СТАНДАРДЕН КОРИСНИК) ---
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape)
                            .clickable {
                                nameInput = currentUserName
                                newProfileImageUri = profileImageUriString
                                isEditingProfile = true
                            }
                    ) {
                        if (profileImageUriString.isNotEmpty()) {
                            AsyncImage(
                                model = profileImageUriString,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(currentUserName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (currentEmail.isNotEmpty()) {
                            Text(currentEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Button(
                    onClick = {
                        Firebase.auth.signOut()
                        Toast.makeText(context, "Successfully logged out!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Logout") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(selected = selectedSubTab == 0, onClick = {
                    selectedSubTab = 0
                    isEditMode = false
                }) {
                    Text("My Recipes (${localRecipes.size})", modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                    Text(if (isEditMode) "Edit Recipe ✏️" else "Add New", modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedSubTab == 0) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (localRecipes.isEmpty()) {
                            item { Text("You haven't added any recipes yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                        }
                        items(items = localRecipes) { local ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (local.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = local.imageUrl,
                                            contentDescription = local.title,
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(local.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Ingredients: ${local.ingredients}", style = MaterialTheme.typography.bodyMedium)
                                        if (local.isPublic) {
                                            Text("Shared Globally 🌍", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    IconButton(onClick = {
                                        isEditMode = true
                                        editingRecipeId = local.id
                                        titleInput = local.title
                                        ingredientsInput = local.ingredients
                                        instructionsInput = local.instructions
                                        imageUriString = local.imageUrl
                                        isPublicChecked = local.isPublic
                                        selectedSubTab = 1
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                                    }

                                    IconButton(onClick = { recipeToDelete = local }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Meal Title") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ingredientsInput,
                                onValueChange = { ingredientsInput = it },
                                label = { Text("Ingredients (comma separated)") },
                                placeholder = { Text("tomato, cheese, onion") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = instructionsInput,
                                onValueChange = { instructionsInput = it },
                                label = { Text("Instructions") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                    Text("Pick Image from Gallery 📸")
                                }
                                if (imageUriString.isNotEmpty()) {
                                    AsyncImage(model = imageUriString, contentDescription = "Preview", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Text("No image selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = isPublicChecked, onCheckedChange = { isPublicChecked = it })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isPublicChecked) "Share publicly on Firebase" else "Save privately in Room")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (titleInput.isNotEmpty() && ingredientsInput.isNotEmpty()) {
                                        val finalTitle = titleInput
                                        val finalInstructions = instructionsInput
                                        val finalImage = imageUriString
                                        val finalIsPublic = isPublicChecked
                                        val finalCleanedIngredients = ingredientsInput.split(",")
                                            .map { it.trim().lowercase() }
                                            .filter { it.isNotEmpty() }
                                            .joinToString(", ")

                                        val localRecipe = LocalRecipe().apply {
                                            if (isEditMode && editingRecipeId != null) {
                                                id = editingRecipeId!!
                                            }
                                            title = finalTitle
                                            ingredients = finalCleanedIngredients
                                            instructions = finalInstructions
                                            isPublic = finalIsPublic
                                            imageUrl = finalImage
                                            userId = currentUid
                                        }

                                        coroutineScope.launch(Dispatchers.IO) {
                                            if (isEditMode) {
                                                recipeDao.updateRecipe(localRecipe)
                                            } else {
                                                recipeDao.insertRecipe(localRecipe)
                                            }
                                        }

                                        if (isEditMode) {
                                            db.collection("public_recipes")
                                                .whereEqualTo("title", finalTitle)
                                                .get()
                                                .addOnSuccessListener { documents ->
                                                    if (documents.isEmpty && finalIsPublic) {
                                                        val firebaseRecipe = hashMapOf(
                                                            "title" to finalTitle,
                                                            "ingredients" to finalCleanedIngredients,
                                                            "instructions" to finalInstructions,
                                                            "imageUrl" to finalImage
                                                        )
                                                        db.collection("public_recipes").add(firebaseRecipe)
                                                    } else {
                                                        for (doc in documents) {
                                                            if (finalIsPublic) {
                                                                db.collection("public_recipes").document(doc.id).update(
                                                                    mapOf(
                                                                        "ingredients" to finalCleanedIngredients,
                                                                        "instructions" to finalInstructions,
                                                                        "imageUrl" to finalImage
                                                                    )
                                                                )
                                                            } else {
                                                                db.collection("public_recipes").document(doc.id).delete()
                                                            }
                                                        }
                                                    }
                                                }
                                        } else {
                                            if (finalIsPublic) {
                                                val firebaseRecipe = hashMapOf(
                                                    "title" to finalTitle,
                                                    "ingredients" to finalCleanedIngredients,
                                                    "instructions" to finalInstructions,
                                                    "imageUrl" to finalImage
                                                )
                                                db.collection("public_recipes").add(firebaseRecipe)
                                            }
                                        }

                                        Toast.makeText(context, if (isEditMode) "Recipe updated!" else "Recipe saved successfully!", Toast.LENGTH_SHORT).show()

                                        titleInput = ""
                                        ingredientsInput = ""
                                        instructionsInput = ""
                                        imageUriString = ""
                                        isPublicChecked = false
                                        isEditMode = false
                                        editingRecipeId = null
                                        selectedSubTab = 0
                                    } else {
                                        Toast.makeText(context, "Please fill in Title and Ingredients!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isEditMode) "Update Recipe" else "Save to My Profile")
                            }
                        }
                    }
                }
            }
        }
    }
}

val androidx.compose.material3.ColorScheme.blackWithAlpha: androidx.compose.ui.graphics.Color
    get() = androidx.compose.ui.graphics.Color(0x66000000)