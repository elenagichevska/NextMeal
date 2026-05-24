package com.example.nextmeal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// 1. УНИВЕРЗАЛЕН МОДЕЛ
data class DetailRecipeView(
    val id: String = "",
    val title: String,
    val imageUrl: String,
    val ingredients: String,
    val instructions: String,
    val source: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: DetailRecipeView,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isLandscape = this.maxWidth > this.maxHeight
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (isLandscape) {
                    // 🧭 LANDSCAPE: Слика од лево, Состојки од десно
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // ЛЕВО: Сликата со Насловот
                        Column(modifier = Modifier.weight(1f)) {
                            if (recipe.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = recipe.imageUrl,
                                    contentDescription = recipe.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🥘 No Image Available", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recipe.title,
                                style = MaterialTheme.typography.titleLarge, // Поправено!
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Source: ${recipe.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // ДЕСНО: Твојата картичка за Состојки
                        Box(modifier = Modifier.weight(1.3f)) {
                            IngredientsCard(recipe = recipe)
                        }
                    }
                } else {
                    // 📱 PORTRAIT: Сè едно под друго
                    if (recipe.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥘 No Image Available", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Source: ${recipe.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    IngredientsCard(recipe = recipe)
                }

                // 3. ПОД НИВ (ЗАЕДНИЧКО): Инструкциите се шират низ цел екран
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Menu, contentDescription = "Instructions", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = recipe.instructions.ifEmpty { "No detailed instructions provided for this recipe. Try experimenting with your favorite cooking techniques!" },
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
                        )
                    }
                }
            }
        }
    }
}

// 4. ПОМОШНА КАМПОНЕНТА ЗА СОСТОЈКИТЕ
@Composable
fun IngredientsCard(recipe: DetailRecipeView) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ingredients", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            val ingredientList = recipe.ingredients.split(",")
            if (ingredientList.all { it.trim().isEmpty() } || recipe.ingredients.isEmpty()) {
                Text("No ingredients list available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            } else {
                ingredientList.forEach { ing ->
                    if (ing.trim().isNotEmpty()) {
                        Text(
                            text = "• ${ing.trim().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}