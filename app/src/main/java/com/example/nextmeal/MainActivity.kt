package com.example.nextmeal



import android.os.Bundle

import android.widget.Toast

import androidx.activity.ComponentActivity

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts

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

import android.Manifest

import android.annotation.SuppressLint

import android.content.pm.PackageManager

import android.os.Build

import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat

import androidx.compose.ui.res.stringResource

import com.facebook.CallbackManager

import com.facebook.FacebookCallback

import com.facebook.FacebookException

import com.facebook.login.LoginManager

import com.facebook.login.LoginResult

import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.common.api.ApiException

import com.google.firebase.analytics.FirebaseAnalytics

import com.google.firebase.auth.FacebookAuthProvider

import com.google.firebase.auth.GoogleAuthProvider

import com.example.nextmeal.BuildConfig

import com.example.nextmeal.ui.theme.*

import androidx.compose.foundation.Image

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource



class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: com.google.firebase.analytics.FirebaseAnalytics

    private val callbackManager = CallbackManager.Factory.create()



    @SuppressLint("StringFormatInvalid")

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        if (BuildConfig.DEBUG) {

            try {

                Runtime.getRuntime().exec("setprop debug.firebase.analytics.app com.example.nextmeal")

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }



        firebaseAnalytics = FirebaseAnalytics.getInstance(this)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)

            }

        }



        val database = AppDatabase.getDatabase(applicationContext)

        val recipeDao = database.recipeDao()



        setContent {
            NextMealTheme {
                AppBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                    ) {
                        val windowSizeClass = calculateWindowSizeClass(this)
                        val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

                        val auth = Firebase.auth
                        var currentUser by remember { mutableStateOf(auth.currentUser) }

                        val googleClientId = getString(R.string.my_google_web_client_id)
// ==========================================
// 1. СИГУРЕН И ЧИСТ GOOGLE ЛАНСЕР
// ==========================================
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(googleClientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(this, gso)

                        val googleSignInLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                                try {
                                    val account = task.getResult(ApiException::class.java)!!
                                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                                    auth.signInWithCredential(credential)
                                        .addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) { // Analytics: Google Login Успешен
                                                firebaseAnalytics.logEvent("google_login_success", null)

                                                val welcomeMsg = getString(R.string.toast_welcome_user, account.displayName ?: "")
                                                Toast.makeText(this, welcomeMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } catch (e: ApiException) {
                                    val failMsg = getString(R.string.toast_google_failed, e.message ?: "")
                                    Toast.makeText(this, failMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }

// ==========================================
// 2. СИГУРЕН И ЧИСТ FACEBOOK ЛАНСЕР
// ==========================================
                        val facebookLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            callbackManager.onActivityResult(0xface, result.resultCode, result.data)
                        }

// Регистрирање на Facebook Callback
                        LaunchedEffect(Unit) {
                            LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                                override fun onSuccess(result: LoginResult) {
                                    val token = result.accessToken.token
                                    val credential = FacebookAuthProvider.getCredential(token)
                                    auth.signInWithCredential(credential)
                                        .addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) { // Analytics: Facebook Login Успешен
                                                firebaseAnalytics.logEvent("facebook_login_success", null)
                                                Toast.makeText(this@MainActivity, getString(R.string.toast_facebook_success), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                                override fun onCancel() {
                                    Toast.makeText(this@MainActivity, getString(R.string.toast_facebook_cancelled), Toast.LENGTH_SHORT).show()
                                }
                                override fun onError(error: FacebookException) {
                                    val errMsg = getString(R.string.toast_facebook_error, error.message ?: "")
                                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                                }
                            })
                        }

// Следење на состојбата на корисникот
                        LaunchedEffect(Unit) {
                            auth.addAuthStateListener { firebaseAuth ->
                                currentUser = firebaseAuth.currentUser
                            }
                        }

// ПРИКАЗ НА ЕКРАНИ
                        if (currentUser == null) {
                            AuthScreen(
                                onGoogleSignIn = {
                                    val signInIntent = googleSignInClient.signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                },
                                onFacebookSignIn = {
                                    LoginManager.getInstance().logInWithReadPermissions(
                                        this,
                                        listOf("email", "public_profile")
                                    )
                                    facebookLauncher.launch(android.content.Intent())
                                },
                                onAuthEventLogged = { eventName ->
                                    val bundle = android.os.Bundle().apply { putString("auth_method", eventName) }
                                    firebaseAnalytics.logEvent("user_auth_action", bundle)
                                }
                            )
                        } else {
                            val navController = rememberNavController()
                            NavigationGraph(
                                navController = navController,
                                recipeDao = recipeDao,
                                isWideScreen = isWideScreen,
                                firebaseAnalytics = firebaseAnalytics // Праќање на Analytics објектот
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val titleResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object SmartSearch : Screen("smart_search", R.string.nav_smart_search, Icons.Default.Search)
    object Explorer : Screen("explorer", R.string.nav_explorer, Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
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
            Surface(
                color = GreenMain,
                shadowElevation = 12.dp
            ) {
                NavigationRail(
                    containerColor = GreenMain
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    items.forEach { screen ->
                        val localizedTitle = stringResource(id = screen.titleResId)
                        NavigationRailItem(
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AccentTeal,
                                selectedTextColor = AccentTeal,
                                indicatorColor = GreenDark,

                                unselectedIconColor = TextLight.copy(alpha = 0.7f),
                                unselectedTextColor = TextLight.copy(alpha = 0.7f)
                            ),
                            icon = { Icon(screen.icon, contentDescription = localizedTitle) },
                            label = { Text(localizedTitle) },
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
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                content(PaddingValues(0.dp))
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                Surface(
                    shadowElevation = 12.dp
                ) {
                    NavigationBar(
                        containerColor = GreenMain,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { screen ->
                            val localizedTitle = stringResource(id = screen.titleResId)

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = localizedTitle
                                    )
                                },
                                label = {
                                    Text(localizedTitle)
                                },
                                selected = currentRoute == screen.route,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentTeal,
                                    selectedTextColor = AccentTeal,
                                    indicatorColor = GreenDark,

                                    unselectedIconColor = TextLight.copy(alpha = 0.7f),
                                    unselectedTextColor = TextLight.copy(alpha = 0.7f)
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ){ innerPadding ->
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
fun NavigationGraph(
    navController: NavHostController,
    recipeDao: RecipeDao,
    isWideScreen: Boolean,
    firebaseAnalytics: com.google.firebase.analytics.FirebaseAnalytics // Додаден параметар за Analytics
) {
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

    var globalSelectedIngredients by rememberSaveable { mutableStateOf(setOf<String>()) }
    var globalHasSearched by rememberSaveable { mutableStateOf(false) }

    val searchKeywords =
        remember { listOf("pizza", "burger", "chicken", "pasta", "salad", "cake", "seafood") }

    val globalRandomKeyword = remember {
        val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val deterministicIndex = currentDayOfYear % searchKeywords.size
        searchKeywords[deterministicIndex]
    }

    val globalApiSuggestions = remember { mutableStateListOf<ApiMeal>() }
    var globalIsExplorerLoading by remember { mutableStateOf(true) }

    val defaultInstructions = stringResource(id = R.string.error_no_instructions)

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

                                                ?: defaultInstructions,

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

                    },

                    onSmartSearchLogged = { count ->

// Analytics: Паметно пребарување

                        val bundle = android.os.Bundle().apply { putInt("ingredients_count", count) }

                        firebaseAnalytics.logEvent("smart_search_used", bundle)

                    }

                )

            }

        }



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

                    },

                    onSearchLogged = { term ->

// Analytics: Истражување

                        val bundle = android.os.Bundle().apply { putString(com.google.firebase.analytics.FirebaseAnalytics.Param.SEARCH_TERM, term) }

                        firebaseAnalytics.logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.SEARCH, bundle)

                    }

                )

            }

        }



        composable(Screen.Profile.route) {

            MainAppLayout(navController = navController, isWideScreen = isWideScreen) {

                ProfileScreen(

                    recipeDao = recipeDao,

                    localRecipes = localRecipes,

                    db = db,

                    onRecipeCreatedLogged = { title, count ->

                        firebaseAnalytics.logEvent(

                            "create_custom_recipe",

                            Bundle().apply {

                                putString("recipe_title", title)

                                putInt("ingredients_count", count)

                            }

                        )

                    },

                    onRecipeClick = { detailView ->

                        selectedRecipeForDetail = detailView

                        navController.navigate("recipe_detail")

                    }

                )

            }

        }



        composable("recipe_detail") {

            val currentRecipe = remember { selectedRecipeForDetail }



            if (currentRecipe.title.isNotEmpty()) {

                RecipeDetailScreen(

                    recipe = currentRecipe,

                    onBackClick = {

                        navController.popBackStack()

                    }

                )

            } else {

                LaunchedEffect(Unit) {

                    navController.popBackStack()

                }

            }



            DisposableEffect(Unit) {

                onDispose {

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