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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource

// 1. Помошни дата класи за рангирање
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

// 2. Логика за обработка и рангирање (работи со англиските стрингови во позадина)
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

// 3. Главен екран
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
    val context = LocalContext.current

    // ДИНАМИЧКО ГРУПИРАЊЕ: Ги зема преведените имиња на категориите од strings.xml
    val groupedIngredients = remember(IngredientsData.list) {
        IngredientsData.list.groupBy { context.getString(it.categoryRes) }
    }

    val apiResults = remember { mutableStateListOf<RankedApiRecipe>() }
    val matchedCustomResults = remember { mutableStateListOf<RankedCustomRecipe>() }
    var isLoading by remember { mutableStateOf(false) }

    val sourceLocal = stringResource(id = R.string.source_label_local)
    val sourceShared = stringResource(id = R.string.source_label_shared)
    val sourceGlobalApi = stringResource(id = R.string.source_label_global_api)

    LaunchedEffect(hasSearched, localRecipes, publicRecipes, selectedIngredients) {
        if (hasSearched && selectedIngredients.isNotEmpty()) {

            val localMapped = localRecipes.mapNotNull {
                processAndRankRecipes(it.ingredients, it.title, sourceLocal, it.imageUrl, it.instructions, selectedIngredients)
            }

            val filteredPublicRecipes = publicRecipes.filter { pub ->
                localRecipes.none { loc -> loc.title.equals(pub.title, ignoreCase = true) }
            }

            val publicMapped = filteredPublicRecipes.mapNotNull {
                processAndRankRecipes(it.ingredients, it.title, sourceShared, it.imageUrl, it.instructions, selectedIngredients)
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
                    // И понатаму го земаме англиското име ("chicken") за пребарување во API
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
                                            ingredientsString, fullMeal.strMeal, sourceGlobalApi,
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
            Text(stringResource(id = R.string.fridge_screen_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(stringResource(id = R.string.fridge_screen_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                groupedIngredients.forEach { (categoryName, ingredients) ->
                    item {
                        // Приказ на преведената категорија
                        Text(text = categoryName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    this.items(items = ingredients) { ingredient ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                // Се проверува и зачувува преку чистиот англиски стринг ("chicken") во позадина
                                checked = selectedIngredients.contains(ingredient.name),
                                onCheckedChange = { isChecked ->
                                    val currentSet = selectedIngredients.toMutableSet()
                                    if (isChecked) currentSet.add(ingredient.name)
                                    else currentSet.remove(ingredient.name)
                                    onIngredientsChange(currentSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // ДИНАМИЧКИ ПРЕВОД: Се прикажува локализираното име од ресурсите
                            Text(
                                text = "${stringResource(id = ingredient.nameRes)}  ${ingredient.emoji}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
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
                    Text(stringResource(id = R.string.btn_clear_all))
                }

                Button(
                    onClick = { onHasSearchedChange(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = selectedIngredients.isNotEmpty()
                ) {
                    Text(stringResource(id = R.string.btn_find_recipes, selectedIngredients.size))
                }
            }
        } else {
            // Резултати од пребарувањето (Останува идентично и оптимизирано)
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
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back_button),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(id = R.string.search_results_main_title),
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
                            Text(stringResource(id = R.string.section_matched_custom_recipes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        this.items(items = matchedCustomResults) { ranked ->
                            val containerColor = if (ranked.source == sourceLocal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
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
                                        Text(stringResource(id = R.string.label_ingredients_list, ranked.allIngredientsString), style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (ranked.missingIngredients.isEmpty()) {
                                            Text(stringResource(id = R.string.label_all_ingredients_owned), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(stringResource(id = R.string.label_missing_ingredients_list, ranked.missingIngredients.joinToString(", ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (apiResults.isNotEmpty()) {
                        item {
                            Text(stringResource(id = R.string.section_global_api_inspiration), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 8.dp))
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
                                                source = sourceGlobalApi
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
                                            Text(sourceGlobalApi, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(stringResource(id = R.string.label_ingredients_list, rankedApi.allIngredientsString), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (rankedApi.missingIngredients.isEmpty()) {
                                            Text(stringResource(id = R.string.label_all_ingredients_owned), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(stringResource(id = R.string.label_missing_ingredients_list, rankedApi.missingIngredients.joinToString(", ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (matchedCustomResults.isEmpty() && apiResults.isEmpty()) {
                        item {
                            Text(stringResource(id = R.string.error_no_recipes_found), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}