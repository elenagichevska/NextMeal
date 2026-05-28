package com.example.nextmeal

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    val currentUserName = Firebase.auth.currentUser?.displayName ?: "User"
    val currentEmail = Firebase.auth.currentUser?.email ?: ""

    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    var titleInput by rememberSaveable { mutableStateOf("") }
    var ingredientsInput by rememberSaveable { mutableStateOf("") }
    var instructionsInput by rememberSaveable { mutableStateOf("") }
    var imageUriString by rememberSaveable { mutableStateOf("") }
    var isPublicChecked by rememberSaveable { mutableStateOf(false) }

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

    // Главна колона со fillMaxSize() за да менаџира правилно со просторот
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
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
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text("My Recipes (${localRecipes.size})", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text("Add New", modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Изнесување на LazyColumn со тежина за да нема заглавување на екранот во Landscape
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedSubTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (localRecipes.isEmpty()) {
                        item { Text("You haven't added any recipes yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                    }
                    items(items = localRecipes) { local ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (local.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = local.imageUrl,
                                        contentDescription = local.title,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp)),
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
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Meal Title") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = ingredientsInput, onValueChange = { ingredientsInput = it }, label = { Text("Ingredients (comma separated)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = instructionsInput, onValueChange = { instructionsInput = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Text("Pick Image from Gallery 📸")
                            }
                            if (imageUriString.isNotEmpty()) {
                                AsyncImage(model = imageUriString, contentDescription = "Preview", modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
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
                                    val localRecipe = LocalRecipe().apply {
                                        title = titleInput
                                        ingredients = ingredientsInput.lowercase()
                                        instructions = instructionsInput
                                        isPublic = isPublicChecked
                                        imageUrl = imageUriString
                                    }

                                    coroutineScope.launch(Dispatchers.IO) {
                                        recipeDao.insertRecipe(localRecipe)
                                    }

                                    if (isPublicChecked) {
                                        val firebaseRecipe = hashMapOf(
                                            "title" to titleInput,
                                            "ingredients" to ingredientsInput.lowercase(),
                                            "instructions" to instructionsInput,
                                            "imageUrl" to imageUriString
                                        )
                                        db.collection("public_recipes").add(firebaseRecipe)
                                    }

                                    Toast.makeText(context, "Recipe saved successfully!", Toast.LENGTH_SHORT).show()
                                    titleInput = ""; ingredientsInput = ""; instructionsInput = ""; imageUriString = ""; isPublicChecked = false; selectedSubTab = 0
                                } else {
                                    Toast.makeText(context, "Please fill in Title and Ingredients!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save to My Profile") }
                    }
                }
            }
        }
    }
}