package com.example.nextmeal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource

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
                title = { Text(stringResource(id = R.string.detail_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back_button))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                                        .heightIn(min = 200.dp, max = 350.dp) // 🌟 Зголемен лимит за таблет за цела да ја собера
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit // 🌟 Спречува сечење (Crop) на сликата
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
                                    Text(stringResource(id = R.string.label_no_image_placeholder), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = recipe.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.recipe_source_label, recipe.source),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // ДЕСНО: Картичка за Состојки
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
                                .heightIn(min = 250.dp, max = 450.dp) // 🌟 На таблет вертикално да има простор за цела слика
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit // 🌟 Спречува сечење (Crop) на сликата
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
                            Text(stringResource(id = R.string.label_no_image_placeholder), style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.recipe_source_label, recipe.source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    IngredientsCard(recipe = recipe)
                }

                // 3. ПОД НИВ (ЗАЕДНИЧКО): Инструкциите се шират низ цел екран
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            text = stringResource(id = R.string.label_instructions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        HorizontalDivider(
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = recipe.instructions.ifEmpty {
                                stringResource(id = R.string.fallback_no_instructions_provided)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(id = R.string.label_ingredients),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            val ingredientList = recipe.ingredients.split(",")

            if (ingredientList.all { it.trim().isEmpty() } || recipe.ingredients.isEmpty()) {
                Text(
                    stringResource(id = R.string.error_no_ingredients_list),
                    color = MaterialTheme.colorScheme.outline
                )
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