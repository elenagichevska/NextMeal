package com.example.nextmeal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// Модел за Firebase приказ
data class Recipe(
    val title: String = "",
    val ingredients: String = "",
    val instructions: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Иницијализација на Room базата (Поврзано со RecipeLocal.kt)
        val database = AppDatabase.getDatabase(applicationContext)
        val recipeDao = database.recipeDao()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecipeScreen(recipeDao = recipeDao)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(recipeDao: RecipeDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Состојби за Firestore (јавни рецепти) и Room (локални кориснички рецепти)
    val publicRecipes = remember { mutableStateListOf<Recipe>() }
    val localRecipes by recipeDao.getAllLocalRecipes().collectAsState(initial = emptyList())

    val db = Firebase.firestore

    // Форма состојби
    var titleInput by remember { mutableStateOf("") }
    var ingredientsInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }
    var isPublicChecked by remember { mutableStateOf(false) } // Состојба за Switch

    // Сигурен слушач за Firebase
    LaunchedEffect(Unit) {
        try {
            db.collection("public_recipes")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        android.util.Log.e("FirebaseError", "Грешка со Firebase: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        publicRecipes.clear()
                        for (document in snapshot) {
                            val recipe = document.toObject(Recipe::class.java)
                            publicRecipes.add(recipe)
                        }
                    }
                }
        } catch (obj: Exception) {
            android.util.Log.e("FirebaseError", "Не може да се воспостави врска")
        }
    }

    // Главна колона која ја содржи формата горе, а долу има тежина за листата
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Додај нов рецепт", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Наслов на јадењето") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ingredientsInput,
            onValueChange = { ingredientsInput = it },
            label = { Text("Состојки (одделени со запирка)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = instructionsInput,
            onValueChange = { instructionsInput = it },
            label = { Text("Начин на подготовка") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Копче за избор: Јавно или Приватно (Switch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isPublicChecked,
                onCheckedChange = { isPublicChecked = it }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(if (isPublicChecked) "Сподели јавно на Firebase" else "Зачувај како приватно (само на уредот)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Копче за зачувување со новата комбинирана логика
        Button(
            onClick = {
                if (titleInput.isNotEmpty() && ingredientsInput.isNotEmpty()) {

                    // НОВ НАЧИН НА КРЕИРАЊЕ НА ОБЈЕКТОТ ЗА ДА НЕ КРАШНУВА КSP
                    val localRecipe = LocalRecipe().apply {
                        title = titleInput
                        ingredients = ingredientsInput
                        instructions = instructionsInput
                        isPublic = isPublicChecked
                    }

                    // СЕКОГАШ запишувај локално во Room директно
                    recipeDao.insertRecipe(localRecipe)

                    // Ако е вклучен Switch-от, прати го и во Firebase
                    if (isPublicChecked) {
                        val firebaseRecipe = hashMapOf(
                            "title" to titleInput,
                            "ingredients" to ingredientsInput,
                            "instructions" to instructionsInput
                        )
                        db.collection("public_recipes")
                            .add(firebaseRecipe)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Успешно споделено во облак!", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Зачувано локално во Room базата!", Toast.LENGTH_SHORT).show()
                    }

                    // Ресетирање на формата
                    titleInput = ""
                    ingredientsInput = ""
                    instructionsInput = ""
                    isPublicChecked = false

                } else {
                    Toast.makeText(context, "Внесете наслов и состојки!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зачувај рецепт")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ЕДНА СИГУРНА LAZYCOLUMN КОЈА ГИ СОДРЖИ ДВЕТЕ ЛИСТИ БЕЗ ДА ЗАГЛАВУВА ЕКРАНОТ
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ДЕЛ 1: ПРИВАТНИ РЕЦЕПТИ (ROOM)
            item {
                Text(
                    text = "Мои Приватни Рецепти (Room сесија: ${localRecipes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(items = localRecipes) { local: LocalRecipe ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(local.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Јавен статус: ${if (local.isPublic) "Да" else "Не"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // РАЗДЕЛУВАЧ ПОМЕЃУ ЛИСТИТЕ ВНАТРЕ ВО Скролот
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Глобални Јавни Рецепти (Firebase сесија: ${publicRecipes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ДЕЛ 2: ЈАВНИ РЕЦЕПТИ (FIREBASE)
            items(items = publicRecipes) { public: Recipe ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(public.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}