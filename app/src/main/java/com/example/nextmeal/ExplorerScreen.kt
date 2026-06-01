package com.example.nextmeal

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    localRecipes: List<LocalRecipe>,
    publicRecipes: List<Recipe>,
    apiSuggestions: List<ApiMeal>,
    randomKeyword: String,
    isSuggestionsLoading: Boolean,
    onRecipeClick: (DetailRecipeView) -> Unit,
    onSearchLogged: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val apiResults = remember { mutableStateListOf<ApiMeal>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isSearchingActive by rememberSaveable { mutableStateOf(false) }

    val isAnonymous = Firebase.auth.currentUser?.isAnonymous == true

    // Детекција на ориентација
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val totalColumns = if (isLandscape) 4 else 2

    val msgRedirectingAuth = stringResource(id = R.string.toast_redirecting_auth)
    val defaultCustomInstructions = stringResource(id = R.string.default_custom_instructions)
    val defaultNoInstructions = stringResource(id = R.string.error_no_instructions)

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(stringResource(id = R.string.explorer_main_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isEmpty()) isSearchingActive = false
                },
                placeholder = { Text(stringResource(id = R.string.search_meals_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.cd_search_icon)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                isSearchingActive = false
                                apiResults.clear()
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchLogged(searchQuery.trim())
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
            ) { Text(stringResource(id = R.string.btn_search_action)) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (isSuggestionsLoading && apiResults.isEmpty() && apiSuggestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(totalColumns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isSearchingActive) {
                        if (apiResults.isEmpty()) {
                            item(span = { GridItemSpan(totalColumns) }) {
                                Text(stringResource(id = R.string.error_no_meals_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            item(span = { GridItemSpan(totalColumns) }) {
                                Text(stringResource(id = R.string.search_results_header), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                            }

                            items(
                                items = apiResults,
                                span = { GridItemSpan(if (isLandscape) 2 else 1) } // По 2 во ред во Landscape, по 1 во Portrait (бидејќи вкупните колони се 2)
                            ) { meal ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                        .clickable {
                                            onRecipeClick(
                                                DetailRecipeView(
                                                    id = meal.idMeal,
                                                    title = meal.strMeal,
                                                    imageUrl = meal.strMealThumb,
                                                    ingredients = meal.getFormattedIngredients(),
                                                    instructions = meal.strInstructions ?: defaultCustomInstructions,
                                                    source = "Global API"
                                                )
                                            )
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = meal.strMealThumb,
                                            contentDescription = meal.strMeal,
                                            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meal.strMeal,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.label_ingredients_list, meal.getFormattedIngredients()),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 1. ДНЕВНА ИНСПИРАЦИЈА
                        item(span = { GridItemSpan(totalColumns) }) {
                            Text(stringResource(id = R.string.section_daily_inspiration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        // Овде секоја картичка зафаќа по 1 колона.
                        // Во Landscape (4 колони) излегуваат 4 во ред. Во Portrait (2 колони) излегуваат 2 во ред.
                        items(items = apiSuggestions, span = { GridItemSpan(1) }) { meal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .clickable {
                                        onRecipeClick(
                                            DetailRecipeView(
                                                id = meal.idMeal,
                                                title = meal.strMeal,
                                                imageUrl = meal.strMealThumb,
                                                ingredients = meal.getFormattedIngredients(),
                                                instructions = meal.strInstructions ?: defaultNoInstructions,
                                                source = "Global API (Daily)"
                                            )
                                        )
                                    },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = meal.strMealThumb,
                                        contentDescription = meal.strMeal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.3f)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = meal.strMeal,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    val capitalKeyword = randomKeyword.replaceFirstChar { it.uppercase() }
                                    Text(
                                        text = stringResource(id = R.string.today_inspiration_tag, capitalKeyword),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(totalColumns) }) { Spacer(modifier = Modifier.height(12.dp)) }

                        // 2. ПОПУЛАРНИ ОД ЗАЕДНИЦАТА
                        item(span = { GridItemSpan(totalColumns) }) {
                            Text(stringResource(id = R.string.section_community_popular), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (publicRecipes.isEmpty()) {
                            item(span = { GridItemSpan(totalColumns) }) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Text(stringResource(id = R.string.empty_community_recipes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            item(span = { GridItemSpan(totalColumns) }) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(items = publicRecipes.take(6)) { recipe ->
                                        Card(
                                            modifier = Modifier
                                                .width(170.dp)
                                                .height(210.dp)
                                                .clickable {
                                                    onRecipeClick(DetailRecipeView("", recipe.title, recipe.imageUrl, recipe.ingredients, recipe.instructions, "Community Shared"))
                                                },
                                            shape = RoundedCornerShape(20.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                                    Text(text = stringResource(id = R.string.label_ingredients_list, recipe.ingredients), style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(totalColumns) }) { Spacer(modifier = Modifier.height(12.dp)) }

                        // 3. МОИ ЛИЧНИ РЕЦЕПТИ
                        item(span = { GridItemSpan(totalColumns) }) {
                            Text(stringResource(id = R.string.section_my_masterpieces), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (isAnonymous) {
                            item(span = { GridItemSpan(totalColumns) }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.guest_lock_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(id = R.string.guest_lock_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                Firebase.auth.signOut()
                                                Toast.makeText(context, msgRedirectingAuth, Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(stringResource(id = R.string.btn_login_register_action))
                                        }
                                    }
                                }
                            }
                        } else if (localRecipes.isEmpty()) {
                            item(span = { GridItemSpan(totalColumns) }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stringResource(id = R.string.empty_kitchen_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(stringResource(id = R.string.empty_kitchen_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(
                                items = localRecipes.take(4),
                                span = { GridItemSpan(if (isLandscape) 2 else 1) } // По 2 во ред во Landscape (зафаќаат 2 од 4 колони)
                            ) { local ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                        .clickable {
                                            onRecipeClick(DetailRecipeView("", local.title, local.imageUrl, local.ingredients, local.instructions, "My Kitchen (Local)"))
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (local.imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = local.imageUrl,
                                                contentDescription = local.title,
                                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("📝", style = MaterialTheme.typography.titleLarge)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = local.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.label_ingredients_list, local.ingredients),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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