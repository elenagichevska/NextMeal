package com.example.nextmeal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// 1. Помошни дата класи потребни за рангирањето
data class RankedCustomRecipe(
    val title: String,
    val allIngredientsString: String,
    val matchedCount: Int,
    val missingIngredients: List<String>,
    val source: String,
    val imageUrl: String,
    val instructions: String
)

data class RankedApiRecipe(
    val id: String,
    val title: String,
    val imageUrl: String,
    val allIngredientsString: String,
    val instructions: String,
    val missingIngredients: List<String>
)

// 2. Логика за обработка и рангирање на состојките
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

// 3. Главниот екран како независна компонента
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

    val apiResults = remember { mutableStateListOf<RankedApiRecipe>() }
    val matchedCustomResults = remember { mutableStateListOf<RankedCustomRecipe>() }
    var isLoading by remember { mutableStateOf(false) }

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