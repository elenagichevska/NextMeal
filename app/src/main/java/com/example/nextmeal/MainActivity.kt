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
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.unit.dp
import java.util.Calendar

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
                        val navController = rememberNavController()
                        NavigationGraph(navController = navController, recipeDao = recipeDao, isWideScreen = isWideScreen)
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
fun MainAppLayout(
    navController: NavHostController,
    isWideScreen: Boolean,
    content: @Composable (PaddingValues) -> Unit
) {
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
                content(PaddingValues(0.dp))
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
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                content(innerPadding)
            }
        }
    }
}

val DetailRecipeViewSaver = mapSaver(
    save = { mapOf("id" to it.id, "title" to it.title, "imageUrl" to it.imageUrl, "ingredients" to it.ingredients, "instructions" to it.instructions, "source" to it.source) },
    restore = {
        DetailRecipeView(
            id = it["id"] as? String ?: "",
            title = it["title"] as? String ?: "",
            imageUrl = it["imageUrl"] as? String ?: "",
            ingredients = it["ingredients"] as? String ?: "",
            instructions = it["instructions"] as? String ?: "",
            source = it["source"] as? String ?: ""
        )
    }
)

@Composable
fun NavigationGraph(navController: NavHostController, recipeDao: RecipeDao, isWideScreen: Boolean) {
    val publicRecipes = remember { mutableStateListOf<Recipe>() }
    val userId = Firebase.auth.currentUser?.uid ?: ""
    val localRecipes by recipeDao.getLocalRecipesForUser(userId).collectAsState(initial = emptyList())
    val db = Firebase.firestore

    var selectedRecipeForDetail by rememberSaveable(stateSaver = DetailRecipeViewSaver) {
        mutableStateOf(
            DetailRecipeView(
                title = "",
                imageUrl = "",
                ingredients = "",
                instructions = "",
                source = ""
            )
        )
    }
    val navigationScope = rememberCoroutineScope()

    // СОСТОЈБИ ЗА SMART SEARCH
    var globalSelectedIngredients by rememberSaveable { mutableStateOf(setOf<String>()) }
    var globalHasSearched by rememberSaveable { mutableStateOf(false) }

    // ГЛОБАЛНИ СОСТОЈБИ ЗА EXPLORER SCREEN (Паметен 24-часовен тајминг базиран на датум)
    val searchKeywords =
        remember { listOf("pizza", "burger", "chicken", "pasta", "salad", "cake", "seafood") }

    val globalRandomKeyword = remember {
        val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val deterministicIndex = currentDayOfYear % searchKeywords.size
        searchKeywords[deterministicIndex]
    }

    val globalApiSuggestions = remember { mutableStateListOf<ApiMeal>() }
    var globalIsExplorerLoading by remember { mutableStateOf(true) }

    LaunchedEffect(globalRandomKeyword) {
        if (globalApiSuggestions.isEmpty()) {
            globalIsExplorerLoading = true
            try {
                val response = MealApiService.instance.searchMealsByName(globalRandomKeyword)
                if (response.meals != null) {
                    globalApiSuggestions.clear()
                    globalApiSuggestions.addAll(response.meals.take(4))
                }
            } catch (_: Exception) {
            } finally {
                globalIsExplorerLoading = false
            }
        } else {
            globalIsExplorerLoading = false
        }
    }

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
        } catch (_: Exception) {
        }
    }

    NavHost(navController = navController, startDestination = Screen.SmartSearch.route) {

        // 1. Smart Search (Fridge)
        composable(Screen.SmartSearch.route) {
            MainAppLayout(navController = navController, isWideScreen = isWideScreen) {
                SmartSearchScreen(
                    localRecipes = localRecipes,
                    publicRecipes = publicRecipes,
                    selectedIngredients = globalSelectedIngredients,
                    onIngredientsChange = { globalSelectedIngredients = it },
                    hasSearched = globalHasSearched,
                    onHasSearchedChange = { globalHasSearched = it },
                    onRecipeClick = { detailView ->
                        if (detailView.source == "Global API" && detailView.id.isNotEmpty()) {
                            navigationScope.launch {
                                try {
                                    val response =
                                        MealApiService.instance.getMealDetailsById(detailView.id)
                                    val fullMeal = response.meals?.firstOrNull()
                                    if (fullMeal != null) {
                                        val ingredientsList = listOfNotNull(
                                            fullMeal.strIngredient1, fullMeal.strIngredient2,
                                            fullMeal.strIngredient3, fullMeal.strIngredient4,
                                            fullMeal.strIngredient5
                                        ).filter { it.isNotEmpty() && it.isNotBlank() }

                                        val compiledIngredients =
                                            if (ingredientsList.isNotEmpty()) ingredientsList.joinToString(
                                                ", "
                                            ) else detailView.ingredients

                                        selectedRecipeForDetail = detailView.copy(
                                            instructions = fullMeal.strInstructions
                                                ?: "No detailed instructions found.",
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
        }

        // 2. Explorer (Explore)
        composable(Screen.Explorer.route) {
            MainAppLayout(navController = navController, isWideScreen = isWideScreen) {
                ExplorerScreen(
                    localRecipes = localRecipes,
                    publicRecipes = publicRecipes,
                    apiSuggestions = globalApiSuggestions,
                    randomKeyword = globalRandomKeyword,
                    isSuggestionsLoading = globalIsExplorerLoading,
                    onRecipeClick = { detailView ->
                        selectedRecipeForDetail = detailView
                        navController.navigate("recipe_detail")
                    }
                )
            }
        }

        // 3. Profile
        composable(Screen.Profile.route) {
            MainAppLayout(navController = navController, isWideScreen = isWideScreen) {
                ProfileScreen(recipeDao, localRecipes, db)
            }
        }

        // 4. Detail Screen
        composable("recipe_detail") {
            // Го зачувуваме рецептот во локална променлива во рамки на овој екран
            // за да остане стабилен дури и ако глобалната состојба се смени
            val currentRecipe = remember { selectedRecipeForDetail }

            if (currentRecipe.title.isNotEmpty()) {
                RecipeDetailScreen(
                    recipe = currentRecipe,
                    onBackClick = {
                        // 🚀 Само едноставно навигирај назад, без да допираш состојби!
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }

            // 🕒 Паметно чистење дури ОТКАКО екранот целосно ќе исчезне од навигацијата
            DisposableEffect(Unit) {
                onDispose {
                    // Ако глобалната состојба се уште го чува овој рецепт, ја чистиме безбедно во позадина
                    if (selectedRecipeForDetail.title.isNotEmpty()) {
                        selectedRecipeForDetail = DetailRecipeView(
                            title = "",
                            imageUrl = "",
                            ingredients = "",
                            instructions = "",
                            source = ""
                        )
                    }
                }
            }
        }
    }
}