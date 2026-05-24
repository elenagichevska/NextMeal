package com.example.nextmeal

// ПРАВИЛНИ ИМПОРТИ ЗА КОРУТИНИ И FIREBASE
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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


// 1. КЛАСАТА НАДВОР ОД ЕКРАНОТ
data class RankedCustomRecipe(
    val title: String,
    val allIngredientsString: String,
    val matchedCount: Int,
    val missingIngredients: List<String>,
    val source: String,
    val imageUrl: String,
    val instructions: String
)

// 2. ФУНКЦИЈАТА ЗА РАНГИРАЊЕ НАДВОР ОД ЕКРАНОТ
fun processAndRankRecipes(
    rawIngredients: String,
    title: String,
    source: String,
    imageUrl: String,
    instructions: String,
    selectedIngredients: Set<String>
): RankedCustomRecipe? {
    val recipeIngredientsList = rawIngredients.split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }

    if (recipeIngredientsList.isEmpty()) return null

    val matched = recipeIngredientsList.filter { recipeIngredient ->
        selectedIngredients.any { selected ->
            recipeIngredient.contains(selected.lowercase()) || selected.lowercase().contains(recipeIngredient)
        }
    }

    if (matched.isEmpty()) return null

    val missing = recipeIngredientsList.filter { recipeIngredient ->
        selectedIngredients.none { selected ->
            recipeIngredient.contains(selected.lowercase()) || selected.lowercase().contains(recipeIngredient)
        }
    }

    return RankedCustomRecipe(title, rawIngredients, matched.size, missing, source, imageUrl, instructions)
}

data class RankedApiRecipe(
    val id: String,
    val title: String,
    val imageUrl: String,
    val allIngredientsString: String,
    val instructions: String,
    val missingIngredients: List<String>
)

@Composable
fun SmartSearchScreen(
    localRecipes: List<LocalRecipe>,
    publicRecipes: List<Recipe>,
    selectedIngredients: Set<String>,
    onIngredientsChange: (Set<String>) -> Unit,
    hasSearched: Boolean,
    onHasSearchedChange: (Boolean) -> Unit,
    onRecipeClick: (DetailRecipeView) -> Unit
) {
    val groupedIngredients = remember { IngredientsData.list.groupBy { it.category } }

    // 🚀 СЕМЕНЕТО: Наместо обично ApiMeal, сега чуваме комплетно обработени и рангирани API рецепти
    val apiResults = remember { mutableStateListOf<RankedApiRecipe>() }
    val matchedCustomResults = remember { mutableStateListOf<RankedCustomRecipe>() }
    var isLoading by remember { mutableStateOf(false) }

    // Ажурирање на резултатите при влез или навигација назад
    LaunchedEffect(hasSearched, localRecipes, publicRecipes, selectedIngredients) {
        if (hasSearched && selectedIngredients.isNotEmpty()) {

            val localMapped = localRecipes.mapNotNull {
                processAndRankRecipes(it.ingredients, it.title, "My Kitchen (Local)", it.imageUrl, it.instructions, selectedIngredients)
            }

            val filteredPublicRecipes = publicRecipes.filter { pub ->
                localRecipes.none { loc -> loc.title.equals(pub.title, ignoreCase = true) }
            }

            val publicMapped = filteredPublicRecipes.mapNotNull {
                processAndRankRecipes(it.ingredients, it.title, "Community (Shared)", it.imageUrl, it.instructions, selectedIngredients)
            }

            matchedCustomResults.clear()
            matchedCustomResults.addAll(
                (localMapped + publicMapped).sortedWith(
                    compareByDescending<RankedCustomRecipe> { it.matchedCount }
                        .thenBy { it.missingIngredients.size }
                )
            )

            if (apiResults.isEmpty()) {
                isLoading = true
                try {
                    val mainIngredient = selectedIngredients.firstOrNull() ?: ""
                    if (mainIngredient.isNotEmpty()) {
                        val response = MealApiService.instance.getMealsByIngredient(mainIngredient.lowercase())
                        apiResults.clear()

                        if (response.meals != null) {

                            val processedApiList = response.meals.take(5).mapNotNull { simpleMeal ->
                                try {
                                    val detailsResponse = MealApiService.instance.getMealDetailsById(simpleMeal.idMeal)
                                    val fullMeal = detailsResponse.meals?.firstOrNull()

                                    if (fullMeal != null) {
                                        val ingredientsString = fullMeal.getFormattedIngredients()

                                        // Ја користиме твојата логика за да видиме што фали од селектираните состојки
                                        val rankedHelper = processAndRankRecipes(
                                            ingredientsString, fullMeal.strMeal, "Global API",
                                            fullMeal.strMealThumb, fullMeal.strInstructions ?: "", selectedIngredients
                                        )

                                        if (rankedHelper != null) {
                                            RankedApiRecipe(
                                                id = fullMeal.idMeal,
                                                title = fullMeal.strMeal,
                                                imageUrl = fullMeal.strMealThumb,
                                                allIngredientsString = rankedHelper.allIngredientsString,
                                                instructions = fullMeal.strInstructions ?: "",
                                                missingIngredients = rankedHelper.missingIngredients
                                            )
                                        } else null
                                    } else null
                                } catch (_: Exception) { null }
                            }

                            apiResults.addAll(processedApiList.sortedBy { it.missingIngredients.size })
                        }
                    }
                } catch (_: Exception) { }
                finally { isLoading = false }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (!hasSearched) {
            Text("My Fridge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Check what ingredients you have at home right now:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                groupedIngredients.forEach { (category, ingredients) ->
                    item {
                        Text(text = category, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    this.items(items = ingredients) { ingredient ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIngredients.contains(ingredient.name),
                                onCheckedChange = { isChecked ->
                                    val currentSet = selectedIngredients.toMutableSet()
                                    if (isChecked) currentSet.add(ingredient.name)
                                    else currentSet.remove(ingredient.name)
                                    onIngredientsChange(currentSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${ingredient.name.replaceFirstChar { it.uppercase() }}  ${ingredient.emoji}",
                                style = MaterialTheme.typography.bodyLarge
                            )}
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onIngredientsChange(emptySet()) },
                    modifier = Modifier.height(50.dp),
                    enabled = selectedIngredients.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CLEAR ALL")
                }

                Button(
                    onClick = { onHasSearchedChange(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = selectedIngredients.isNotEmpty()
                ) {
                    Text("FIND RECIPES (${selectedIngredients.size})")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        apiResults.clear()
                        onHasSearchedChange(false)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    if (matchedCustomResults.isNotEmpty()) {
                        item {
                            Text("Your and shared recipes with these ingredients:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        this.items(items = matchedCustomResults) { ranked ->
                            val containerColor = if (ranked.source.contains("Local")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = "",
                                                title = ranked.title,
                                                imageUrl = ranked.imageUrl,
                                                ingredients = ranked.allIngredientsString,
                                                instructions = ranked.instructions,
                                                source = ranked.source
                                            )
                                        )
                                    },
                                colors = CardDefaults.cardColors(containerColor = containerColor)
                            ) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (ranked.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = ranked.imageUrl,
                                            contentDescription = ranked.title,
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(ranked.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(ranked.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Ingredients: ${ranked.allIngredientsString}", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (ranked.missingIngredients.isEmpty()) {
                                            Text("✓ You have all ingredients!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("⚠ Missing: ${ranked.missingIngredients.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (apiResults.isNotEmpty()) {
                        item {
                            Text("Inspiration from Global API:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        this.items(items = apiResults) { rankedApi ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = rankedApi.id,
                                                title = rankedApi.title,
                                                imageUrl = rankedApi.imageUrl,
                                                ingredients = rankedApi.allIngredientsString,
                                                instructions = rankedApi.instructions,
                                                source = "Global API"
                                            )
                                        )
                                    }
                            ) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = rankedApi.imageUrl,
                                        contentDescription = rankedApi.title,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(rankedApi.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Global API", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Ingredients: ${rankedApi.allIngredientsString}", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 🚀 СЕГА И ТУКА СЕ ПРИКАЖУВА ШТО ФАЛИ ИЛИ ДАЛИ СЀ ИМАШ!
                                        if (rankedApi.missingIngredients.isEmpty()) {
                                            Text("✓ You have all ingredients!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("⚠ Missing: ${rankedApi.missingIngredients.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (matchedCustomResults.isEmpty() && apiResults.isEmpty()) {
                        item {
                            Text("No recipes found.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(publicRecipes: List<Recipe>, onRecipeClick: (DetailRecipeView) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val apiResults = remember { mutableStateListOf<ApiMeal>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var isSearchingActive by remember { mutableStateOf(false) }

    val apiSuggestions = remember { mutableStateListOf<ApiMeal>() }
    var isSuggestionsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = MealApiService.instance.searchMealsByName("pasta")
            if (response.meals != null) {
                apiSuggestions.addAll(response.meals.take(5))
            }
        } catch (_: Exception) { }
        finally { isSuggestionsLoading = false }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Explore Recipes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Enter meal name (e.g., pizza, burger, pasta, cake)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isEmpty()) isSearchingActive = false
                },
                placeholder = { Text("Search meals...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        isLoading = true
                        isSearchingActive = true
                        coroutineScope.launch {
                            try {
                                val response = MealApiService.instance.searchMealsByName(searchQuery.trim().lowercase())
                                apiResults.clear()
                                if (response.meals != null) {
                                    apiResults.addAll(response.meals)
                                }
                            } catch (_: Exception) { }
                            finally { isLoading = false }
                        }
                    }
                },
                modifier = Modifier.height(56.dp)
            ) { Text("Search") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                if (isSearchingActive) {
                    if (apiResults.isEmpty()) {
                        item { Text("No meals found with that name.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                    } else {
                        item { Text("Search Results:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp)) }
                        items(items = apiResults) { meal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = meal.idMeal,
                                                title = meal.strMeal,
                                                imageUrl = meal.strMealThumb,
                                                ingredients = "Main items found inside query: $searchQuery",
                                                instructions = meal.strInstructions ?: "Mix and style according to standard custom parameters. Check online databases for further information.",
                                                source = "Global API"
                                            )
                                        )
                                    }
                            ) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = meal.strMealThumb, contentDescription = meal.strMeal, modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = meal.strMeal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Popular from Community 🌍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (publicRecipes.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text("No public recipes yet. Be the first to share one!", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                items(items = publicRecipes.take(6)) { recipe ->
                                    Card(
                                        modifier = Modifier
                                            .width(170.dp)
                                            .height(210.dp)
                                            .clickable {
                                                onRecipeClick(
                                                    DetailRecipeView(
                                                        id = "",
                                                        title = recipe.title,
                                                        imageUrl = recipe.imageUrl,
                                                        ingredients = recipe.ingredients,
                                                        instructions = recipe.instructions,
                                                        source = "Community Shared"
                                                    )
                                                )
                                            },
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Column {
                                            if (recipe.imageUrl.isNotEmpty()) {
                                                AsyncImage(model = recipe.imageUrl, contentDescription = recipe.title, modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp), contentScale = ContentScale.Crop)
                                            } else {
                                                Box(modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp), contentAlignment = Alignment.Center) { Text("🥘", style = MaterialTheme.typography.headlineMedium) }
                                            }
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(text = recipe.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = "Ingredients: ${recipe.ingredients}", style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        Text("Daily Culinary Inspiration 🍝", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isSuggestionsLoading) {
                        item { Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    } else {
                        items(items = apiSuggestions) { meal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = meal.idMeal,
                                                title = meal.strMeal,
                                                imageUrl = meal.strMealThumb,
                                                ingredients = "Pasta, Water, Salt, Herbs",
                                                instructions = meal.strInstructions ?: "Boil the pasta according to package directions.",
                                                source = "Daily Suggestion"
                                            )
                                        )
                                    },
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = meal.strMealThumb, contentDescription = meal.strMeal, modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = meal.strMeal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(text = "Inspiration of the day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
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

    var selectedSubTab by remember { mutableIntStateOf(0) }

    var titleInput by remember { mutableStateOf("") }
    var ingredientsInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }
    var imageUriString by remember { mutableStateOf("") }
    var isPublicChecked by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.padding(16.dp)) {
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