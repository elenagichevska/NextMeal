package com.example.nextmeal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Модел кој одговара на полињата од твојата слика
data class Recipe(
    val title: String = "",
    val ingredients: String = "",
    val instructions: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecipeScreen()
                }
            }
        }
    }
}

@Composable
fun RecipeScreen() {
    // Листа каде ќе ги чуваме рецептите што ќе ги прочитаме
    val recipes = remember { mutableStateListOf<Recipe>() }
    val db = Firebase.firestore

    // Ова се извршува кога ќе се пушти екранот
    LaunchedEffect(Unit) {
        db.collection("public_recipes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val recipe = document.toObject(Recipe::class.java)
                    recipes.add(recipe)
                }
            }
    }

    // Приказ на листата
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Мои Рецепти од Firebase", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(recipes) { recipe ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = recipe.title, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Состојки: ${recipe.ingredients}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Подготовка: ${recipe.instructions}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}