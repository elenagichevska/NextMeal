package com.example.nextmeal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

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

                    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

                    LaunchedEffect(Unit) {
                        Firebase.auth.addAuthStateListener { auth ->
                            currentUser = auth.currentUser
                        }
                    }

                    if (currentUser == null) {
                        AuthScreen()
                    } else {
                        MainNavigationContainer(recipeDao = recipeDao, isWideScreen = isWideScreen)
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object SmartSearch : Screen("smart_search", "Fridge", Icons.Default.Search)
    object Explorer : Screen("explorer", "Explore", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
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

    var selectedRecipeForDetail by remember { mutableStateOf<DetailRecipeView?>(null) }
    val navigationScope = rememberCoroutineScope()

    // НАЈБЕЗБЕДЕН НАЧИН: rememberSaveable кој ја чува состојбата во меморијата на Android системски!
    var globalSelectedIngredients by rememberSaveable { mutableStateOf(setOf<String>()) }
    var globalHasSearched by rememberSaveable { mutableStateOf(false) }

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

        // 1. Smart Search (Fridge)
        composable(Screen.SmartSearch.route) {
            SmartSearchScreen(
                localRecipes = localRecipes,
                publicRecipes = publicRecipes,
                // Ги праќаме новите стабилни променливи и нивните функции за промена
                selectedIngredients = globalSelectedIngredients,
                onIngredientsChange = { globalSelectedIngredients = it },
                hasSearched = globalHasSearched,
                onHasSearchedChange = { globalHasSearched = it },
                onRecipeClick = { detailView ->
                    if (detailView.source == "Global API" && detailView.id.isNotEmpty()) {
                        navigationScope.launch {
                            try {
                                val response = MealApiService.instance.getMealDetailsById(detailView.id)
                                val fullMeal = response.meals?.firstOrNull()
                                if (fullMeal != null) {
                                    val ingredientsList = listOfNotNull(
                                        fullMeal.strIngredient1, fullMeal.strIngredient2,
                                        fullMeal.strIngredient3, fullMeal.strIngredient4,
                                        fullMeal.strIngredient5
                                    ).filter { it.isNotEmpty() && it.isNotBlank() }

                                    val compiledIngredients = if (ingredientsList.isNotEmpty()) ingredientsList.joinToString(", ") else detailView.ingredients

                                    selectedRecipeForDetail = detailView.copy(
                                        instructions = fullMeal.strInstructions ?: "No detailed instructions found.",
                                        ingredients = compiledIngredients
                                    )
                                } else {
                                    selectedRecipeForDetail = detailView
                                }
                            } catch (_: Exception) {
                                selectedRecipeForDetail = detailView
                            }
                            navController.navigate("recipe_detail")
                        }
                    } else {
                        selectedRecipeForDetail = detailView
                        navController.navigate("recipe_detail")
                    }
                }
            )
        }

        // 2. Explorer (Explore)
        composable(Screen.Explorer.route) {
            ExplorerScreen(
                publicRecipes = publicRecipes,
                onRecipeClick = { detailView ->
                    if (detailView.source == "Global API" && detailView.id.isNotEmpty()) {
                        navigationScope.launch {
                            try {
                                val response = MealApiService.instance.getMealDetailsById(detailView.id)
                                val fullMeal = response.meals?.firstOrNull()
                                if (fullMeal != null) {
                                    val ingredientsList = listOfNotNull(
                                        fullMeal.strIngredient1, fullMeal.strIngredient2,
                                        fullMeal.strIngredient3, fullMeal.strIngredient4,
                                        fullMeal.strIngredient5
                                    ).filter { it.isNotEmpty() && it.isNotBlank() }

                                    val compiledIngredients = if (ingredientsList.isNotEmpty()) ingredientsList.joinToString(", ") else detailView.ingredients

                                    selectedRecipeForDetail = detailView.copy(
                                        instructions = fullMeal.strInstructions ?: "No detailed instructions found.",
                                        ingredients = compiledIngredients
                                    )
                                } else {
                                    selectedRecipeForDetail = detailView
                                }
                            } catch (_: Exception) {
                                selectedRecipeForDetail = detailView
                            }
                            navController.navigate("recipe_detail")
                        }
                    } else {
                        selectedRecipeForDetail = detailView
                        navController.navigate("recipe_detail")
                    }
                }
            )
        }

        // 3. Profile
        composable(Screen.Profile.route) {
            ProfileScreen(recipeDao, localRecipes, db)
        }

        // 4. Detail Screen
        composable("recipe_detail") {
            selectedRecipeForDetail?.let { recipe ->
                RecipeDetailScreen(
                    recipe = recipe,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}