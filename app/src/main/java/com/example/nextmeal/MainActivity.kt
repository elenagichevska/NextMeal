package com.example.nextmeal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class Recipe(
    val title: String = "",
    val ingredients: String = "",
    val instructions: String = ""
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val recipeDao = database.recipeDao()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val windowSizeClass = calculateWindowSizeClass(this)
                    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

                    MainNavigationContainer(recipeDao = recipeDao, isWideScreen = isWideScreen)
                }
            }
        }
    }
}

// НОВИОТ РЕДОСЛЕД НА ЕКРАНИ
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object SmartSearch : Screen("smart_search", "Фрижидер", Icons.Default.Search)
    object Explorer : Screen("explorer", "Глобално", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", "Профил", Icons.Default.Person)
}

@Composable
fun MainNavigationContainer(recipeDao: RecipeDao, isWideScreen: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(Screen.SmartSearch, Screen.Explorer, Screen.Profile)

    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                Spacer(modifier = Modifier.weight(1f))
                items.forEach { screen ->
                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavigationGraph(navController = navController, recipeDao = recipeDao)
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavigationGraph(navController = navController, recipeDao = recipeDao)
            }
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, recipeDao: RecipeDao) {
    val publicRecipes = remember { mutableStateListOf<Recipe>() }
    val localRecipes by recipeDao.getAllLocalRecipes().collectAsState(initial = emptyList())
    val db = Firebase.firestore

    LaunchedEffect(Unit) {
        try {
            db.collection("public_recipes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        publicRecipes.clear()
                        for (document in snapshot) {
                            val recipe = document.toObject(Recipe::class.java)
                            publicRecipes.add(recipe)
                        }
                    }
                }
        } catch (_: Exception) { }
    }

    NavHost(navController = navController, startDestination = Screen.SmartSearch.route) {
        composable(Screen.SmartSearch.route) {
            SmartSearchScreen(localRecipes = localRecipes, publicRecipes = publicRecipes)
        }
        composable(Screen.Explorer.route) {
            ExplorerScreen(publicRecipes = publicRecipes)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(recipeDao = recipeDao, localRecipes = localRecipes, db = db)
        }
    }
}

// === ТАБ 1: ПАМЕТЕН ПРЕБАРАРУВАЧ (MYFRIDGEFOOD) ===
@Composable
fun SmartSearchScreen(localRecipes: List<LocalRecipe>, publicRecipes: List<Recipe>) {
    val selectedIngredients = remember { mutableStateListOf<String>() }
    var hasSearched by remember { mutableStateOf(false) }

    val groupedIngredients = remember { IngredientsData.list.groupBy { it.category } }

    data class RankedRecipe(
        val title: String,
        val allIngredientsString: String,
        val matchedCount: Int,
        val missingIngredients: List<String>,
        val isLocal: Boolean
    )

    fun processAndRankRecipes(rawIngredients: String, title: String, isLocal: Boolean): RankedRecipe? {
        val recipeIngredientsList = rawIngredients.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (recipeIngredientsList.isEmpty()) return null

        val matched = recipeIngredientsList.filter { recipeIngredient ->
            selectedIngredients.any { selected -> recipeIngredient.contains(selected) || selected.contains(recipeIngredient) }
        }

        if (matched.isEmpty()) return null

        val missing = recipeIngredientsList.filter { recipeIngredient ->
            selectedIngredients.none { selected -> recipeIngredient.contains(selected) || selected.contains(recipeIngredient) }
        }

        return RankedRecipe(title, rawIngredients, matched.size, missing, isLocal)
    }

    val allRankedResults = remember(hasSearched, localRecipes, publicRecipes, selectedIngredients.size) {
        if (!hasSearched || selectedIngredients.isEmpty()) emptyList()
        else {
            val localResults = localRecipes.mapNotNull { processAndRankRecipes(it.ingredients, it.title, isLocal = true) }
            val publicResults = publicRecipes.mapNotNull { processAndRankRecipes(it.ingredients, it.title, isLocal = false) }

            (localResults + publicResults).sortedWith(
                compareByDescending<RankedRecipe> { it.matchedCount }
                    .thenBy { it.missingIngredients.size }
            )
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (!hasSearched) {
            Text("Мојот Фрижидер 🍉", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Штиклирајте сè што имате дома во моментов:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                groupedIngredients.forEach { (category, ingredients) ->
                    item {
                        Text(text = category, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(items = ingredients) { ingredient ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIngredients.contains(ingredient.name),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedIngredients.add(ingredient.name)
                                    else selectedIngredients.remove(ingredient.name)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = ingredient.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { hasSearched = true }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = selectedIngredients.isNotEmpty()) {
                Text("ПРОНАЈДИ РЕЦЕПТИ (${selectedIngredients.size} избрани)")
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Предложени јадења", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { hasSearched = false }) { Text("← Фрижидер") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (allRankedResults.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(text = "Нема пронајдено рецепти со овие намирници.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                items(items = allRankedResults) { ranked ->
                    val containerColor = if (ranked.isLocal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    val tagText = if (ranked.isLocal) "Моја кујна (Room)" else "Заедница (Firebase)"
                    val tagColor = if (ranked.isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ranked.title, style = MaterialTheme.typography.titleLarge)
                                Text(tagText, style = MaterialTheme.typography.labelSmall, color = tagColor)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Сите состојки: ${ranked.allIngredientsString}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (ranked.missingIngredients.isEmpty()) {
                                Text(text = "✓ Ги имате сите потребни состојки!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                val missingText = ranked.missingIngredients.joinToString(", ") { it.replaceFirstChar { char -> char.uppercase() } }
                                Text(text = "⚠ Ви фали: $missingText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// === ТАБ 2: ГЛОБАЛНИ РЕЦЕПТИ СО SEARCH BAR ===
@Composable
fun ExplorerScreen(publicRecipes: List<Recipe>) {
    var searchQuery by remember { mutableStateOf("") }

    // Филтрирање во реално време според внесениот текст во Search Bar-от
    val filteredPublicRecipes = remember(searchQuery, publicRecipes) {
        if (searchQuery.isEmpty()) publicRecipes
        else publicRecipes.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.ingredients.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Глобални Рецепти 🌍", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // SEARCH BAR ИМПЛЕМЕНТАЦИЈА
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Пребарај по наслов или состојка...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Пребарај") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (filteredPublicRecipes.isEmpty()) {
                item {
                    Text("Нема резултати за вашето пребарување.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
            items(items = filteredPublicRecipes) { public ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(public.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Состојки: ${public.ingredients}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// === ТАБ 3: МОЈ ПРОФИЛ (ФОРМА + ЛОКАЛНИ РЕЦЕПТИ) ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(recipeDao: RecipeDao, localRecipes: List<LocalRecipe>, db: com.google.firebase.firestore.FirebaseFirestore) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(0) } // 0 за Мои Рецепти, 1 за Додај Рецепт

    // Држачи за состојба на формата
    var titleInput by remember { mutableStateOf("") }
    var ingredientsInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }
    var isPublicChecked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Заглавие на Профилот
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = "Профил", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Готвач Профил", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Добредојдовте назад!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Мали внатрешни јазичиња (Sub-Tabs) за префрлање
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text("Мои Рецепти (${localRecipes.size})", modifier = Modifier.padding(vertical = 12.0.dp))
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text("Додај Нов", modifier = Modifier.padding(vertical = 12.0.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSubTab == 0) {
            // ПОД-ЕКРАН А: Листа на лични рецепти (Room)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (localRecipes.isEmpty()) {
                    item { Text("Сè уште немате додадено ваши рецепти.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                }
                items(items = localRecipes) { local ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(local.title, style = MaterialTheme.typography.titleMedium)
                            Text("Состојки: ${local.ingredients}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                // ТУКА ПОНАТАМУ МОЖЕ ДА ДОДАДЕМЕ И ТАБ ЗА ЗАЧУВАНИ/ОМИЛЕНИ
            }
        } else {
            // ПОД-ЕКРАН Б: Форма за додавање на рецепт
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Наслов на јадењето") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = ingredientsInput, onValueChange = { ingredientsInput = it }, label = { Text("Состојки (одделени со запирка)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = instructionsInput, onValueChange = { instructionsInput = it }, label = { Text("Начин на подготовка") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPublicChecked, onCheckedChange = { isPublicChecked = it })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isPublicChecked) "Сподели јавно на Firebase" else "Зачувај приватно во Room")
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
                            }
                            recipeDao.insertRecipe(localRecipe)
                            if (isPublicChecked) {
                                val firebaseRecipe = hashMapOf("title" to titleInput, "ingredients" to ingredientsInput.lowercase(), "instructions" to instructionsInput)
                                db.collection("public_recipes").add(firebaseRecipe)
                            }
                            Toast.makeText(context, "Успешно зачувано во профилот!", Toast.LENGTH_SHORT).show()
                            titleInput = ""; ingredientsInput = ""; instructionsInput = ""; isPublicChecked = false
                            selectedSubTab = 0 // Го враќаме корисникот кај неговата листа
                        } else {
                            Toast.makeText(context, "Пополнете ги полињата!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Зачувај во мој профил") }
            }
        }
    }
}