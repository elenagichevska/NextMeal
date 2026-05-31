package com.example.nextmeal

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.auth // 🚀 Точниот импорт за auth кој ја решава грешката
import com.google.firebase.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    localRecipes: List<LocalRecipe>,
    publicRecipes: List<Recipe>,
    apiSuggestions: List<ApiMeal>,
    randomKeyword: String,
    isSuggestionsLoading: Boolean,
    onRecipeClick: (DetailRecipeView) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val apiResults = remember { mutableStateListOf<ApiMeal>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isSearchingActive by rememberSaveable { mutableStateOf(false) }

    // Проверка за Анонимен Гост
    val isAnonymous = Firebase.auth.currentUser?.isAnonymous == true

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Explore Recipes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isEmpty()) isSearchingActive = false
                },
                placeholder = { Text("Search meals online...") },
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 🔄 Искористување на isLoading: Прикажуваме анимација додека се пребарува на интернет
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (isSuggestionsLoading && apiResults.isEmpty() && apiSuggestions.isEmpty()) {
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
                                                    ingredients = meal.getFormattedIngredients(),
                                                    instructions = meal.strInstructions ?: "Mix and style according to standard custom parameters.",
                                                    source = "Global API"
                                                )
                                            )
                                        }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        // 1. Дневна инспирација
                        item {
                            Text("Daily Culinary Inspiration 💫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

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
                                                ingredients = meal.getFormattedIngredients(),
                                                instructions = meal.strInstructions ?: "No detailed instructions found.",
                                                source = "Global API (Daily)"
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
                                        Text(text = "Today's Inspiration: ${randomKeyword.replaceFirstChar { it.uppercase() }} 👨‍🍳", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }

                        // 2. Популарни од заедницата
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
                                                    onRecipeClick(DetailRecipeView("", recipe.title, recipe.imageUrl, recipe.ingredients, recipe.instructions, "Community Shared"))
                                                },
                                            elevation = CardDefaults.cardElevation(2.dp)
                                        ) {
                                            Column {
                                                if (recipe.imageUrl.isNotEmpty()) {
                                                    AsyncImage(model = recipe.imageUrl, contentDescription = recipe.title, modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp), contentScale = ContentScale.Crop)
                                                } else {
                                                    Box(modifier = Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) { Text("🥘", style = MaterialTheme.typography.headlineMedium) }
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

                        item { Spacer(modifier = Modifier.height(20.dp)) }

                        // 3. Мои лични рецепти (Со проверка за анонимен гост)
                        item {
                            Text("My Kitchen Masterpieces 👩‍🍳", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (isAnonymous) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Your digital kitchen is locked! 🔒",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "To create and view your personal recipes, please log in or register a full account.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                Firebase.auth.signOut()
                                                Toast.makeText(context, "Redirecting to Authentication...", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Log In / Register 🍳")
                                        }
                                    }
                                }
                            }
                        } else if (localRecipes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Your kitchen is quiet! 🧑‍🍳", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("No personal recipes created yet. Go ahead and add your first culinary masterpiece!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        } else {
                            items(items = localRecipes.take(4)) { local ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onRecipeClick(DetailRecipeView("", local.title, local.imageUrl, local.ingredients, local.instructions, "My Kitchen (Local)"))
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (local.imageUrl.isNotEmpty()) {
                                            AsyncImage(model = local.imageUrl, contentDescription = local.title, modifier = Modifier
                                                .size(55.dp)
                                                .clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(modifier = Modifier.size(55.dp), contentAlignment = Alignment.Center) { Text("📝") }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = local.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(text = "Ingredients: ${local.ingredients}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
}