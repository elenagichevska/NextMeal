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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onRecipeClick: (DetailRecipeView) -> Unit,
    recipeDao: RecipeDao,
    localRecipes: List<LocalRecipe>,
    db: FirebaseFirestore,
    onRecipeCreatedLogged: (String, Int) -> Unit // 👈 Овој callback безбедно го пренесува настанот до MainActivity
) {
    val contentResolver = LocalContext.current.contentResolver
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 👤 Состојби за Firebase профилот на корисникот
    val currentUser = Firebase.auth.currentUser

    // 🌟 КЛУЧНА ПРОВЕРКА: Дали корисникот е Анонимен (Гост)?
    val isAnonymous = currentUser?.isAnonymous == true

    val defaultUserLabel = stringResource(id = R.string.default_username)
    var currentUserName by remember { mutableStateOf(currentUser?.displayName ?: defaultUserLabel) }
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

    // Локализирани пораки за логика во onClick
    val msgRecipeDeleted = stringResource(id = R.string.toast_recipe_deleted)
    val msgAccountDeleted = stringResource(id = R.string.toast_account_deleted)
    val msgProfileUpdated = stringResource(id = R.string.toast_profile_updated)
    val msgProfileUpdateFailed = stringResource(id = R.string.toast_profile_update_failed)
    val msgLoggedOut = stringResource(id = R.string.toast_logged_out)
    val msgRecipeUpdated = stringResource(id = R.string.toast_recipe_updated)
    val msgRecipeSaved = stringResource(id = R.string.toast_recipe_saved_success)
    val msgFillRequiredRecipe = stringResource(id = R.string.toast_fill_title_ingredients)
    val msgRedirectingLogin = stringResource(id = R.string.toast_redirecting_login)

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
            title = { Text(stringResource(id = R.string.dialog_delete_recipe_title)) },
            text = { Text(stringResource(id = R.string.dialog_delete_recipe_text, recipeToDelete?.title ?: "")) },
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
                            Toast.makeText(context, msgRecipeDeleted, Toast.LENGTH_SHORT).show()
                        }
                        recipeToDelete = null
                    }
                ) { Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { recipeToDelete = null }) { Text(stringResource(id = R.string.btn_cancel)) } }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(id = R.string.dialog_delete_account_title)) },
            text = { Text(stringResource(id = R.string.dialog_delete_account_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, msgAccountDeleted, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text(stringResource(id = R.string.btn_delete_permanently), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false }) { Text(stringResource(id = R.string.btn_cancel)) } }
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
                color = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.cd_guest_avatar),
                    modifier = Modifier.size(64.dp).padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.guest_profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.guest_profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    Firebase.auth.signOut()
                    Toast.makeText(context, msgRedirectingLogin, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(id = R.string.btn_signin_create_account))
            }
        }
    } else if (isEditingProfile) {
        // --- ✏️ ЕКРАН ЗА ЕДИТ НА ПРОФИЛ (СТАНДАРДЕН КОРИСНИК) ---
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isEditingProfile = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back_button))
                }
                Text(stringResource(id = R.string.edit_profile_header), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        contentDescription = stringResource(id = R.string.cd_new_profile_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(id = R.string.cd_avatar_placeholder), modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(30.dp),
                    color = Color(0x66000000)
                ) {
                    Text(stringResource(id = R.string.label_change_photo), style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(stringResource(id = R.string.hint_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
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
                                Toast.makeText(context, msgProfileUpdated, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, msgProfileUpdateFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.btn_save_changes))
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.btn_delete))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.btn_delete_account_permanently), fontWeight = FontWeight.Bold)
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
                                contentDescription = stringResource(id = R.string.cd_profile_picture),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Default.Person, contentDescription = stringResource(id = R.string.cd_profile_picture), modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                        Toast.makeText(context, msgLoggedOut, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.btn_logout)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(selected = selectedSubTab == 0, onClick = {
                    selectedSubTab = 0
                    isEditMode = false
                }) {
                    Text(stringResource(id = R.string.tab_my_recipes, localRecipes.size), modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                    Text(if (isEditMode) stringResource(id = R.string.tab_edit_recipe_indicator) else stringResource(id = R.string.tab_add_new), modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedSubTab == 0) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (localRecipes.isEmpty()) {
                            item { Text(stringResource(id = R.string.empty_my_recipes_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                        }
                        items(items = localRecipes) { local ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = local.id.toString(),
                                                title = local.title,
                                                imageUrl = local.imageUrl,
                                                ingredients = local.ingredients,
                                                instructions = local.instructions,
                                                source = "local"
                                            )
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                        Text(local.title, fontWeight = FontWeight.Bold)
                                        Text(local.ingredients)
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
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }

                                    IconButton(onClick = { recipeToDelete = local }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text(stringResource(id = R.string.hint_meal_title)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ingredientsInput,
                                onValueChange = { ingredientsInput = it },
                                label = { Text(stringResource(id = R.string.hint_ingredients_comma)) },
                                placeholder = { Text(stringResource(id = R.string.placeholder_ingredients_example)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = instructionsInput,
                                onValueChange = { instructionsInput = it },
                                label = { Text(stringResource(id = R.string.hint_instructions_title)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                    Text(stringResource(id = R.string.btn_pick_gallery))

                                }
                                if (imageUriString.isNotEmpty()) {
                                    AsyncImage(model = imageUriString, contentDescription = stringResource(id = R.string.cd_preview_image), modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Text(stringResource(id = R.string.label_no_image_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = isPublicChecked, onCheckedChange = { isPublicChecked = it })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isPublicChecked) stringResource(id = R.string.switch_share_firebase) else stringResource(id = R.string.switch_save_room))
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

                                        // 👈 ОВДЕ: Изброј ги состојките што корисникот реално ги внел
                                        val ingredientsCount = finalCleanedIngredients.split(",").filter { it.isNotBlank() }.size

                                        // 👈 ОВДЕ: Го активираме аналитичкиот настан само кога се КРЕИРА нов рецепт (не кога едитираме)
                                        if (!isEditMode) {
                                            onRecipeCreatedLogged(finalTitle, ingredientsCount)
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

                                        Toast.makeText(context, if (isEditMode) msgRecipeUpdated else msgRecipeSaved, Toast.LENGTH_SHORT).show()

                                        titleInput = ""
                                        ingredientsInput = ""
                                        instructionsInput = ""
                                        imageUriString = ""
                                        isPublicChecked = false
                                        isEditMode = false
                                        editingRecipeId = null
                                        selectedSubTab = 0
                                    } else {
                                        Toast.makeText(context, msgFillRequiredRecipe, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when {
                                        isEditMode -> msgRecipeUpdated
                                        isPublicChecked -> stringResource(id = R.string.btn_share_to_community)
                                        else -> stringResource(id = R.string.btn_save_profile_action)
                                    }
                                )
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