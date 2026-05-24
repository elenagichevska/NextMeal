package com.example.nextmeal

// Модел за намирница со категорија и емоџи
data class Ingredient(val name: String, val category: String, val emoji: String)

object IngredientsData {
    val list = listOf(
        // Meat, Fish & Proteins
        Ingredient("chicken", "Meat & Proteins", "🍗"),
        Ingredient("pork", "Meat & Proteins", "🥩"),
        Ingredient("beef", "Meat & Proteins", "🍖"),
        Ingredient("minced beef", "Meat & Proteins", "🍔"),
        Ingredient("bacon", "Meat & Proteins", "🥓"),
        Ingredient("salami", "Meat & Proteins", "🍕"),
        Ingredient("sausage", "Meat & Proteins", "🌭"),
        Ingredient("egg", "Meat & Proteins", "🥚"),
        Ingredient("tuna", "Meat & Proteins", "🐟"),

        // Dairy Products
        Ingredient("cheese", "Dairy Products", "🧀"),
        Ingredient("cheddar", "Dairy Products", "🧀"),
        Ingredient("milk", "Dairy Products", "🥛"),
        Ingredient("yogurt", "Dairy Products", "🥛"),
        Ingredient("sour cream", "Dairy Products", "🥣"),
        Ingredient("butter", "Dairy Products", "🧈"),
        Ingredient("margarine", "Dairy Products", "🧈"),
        Ingredient("cream cheese", "Dairy Products", "🥣"),

        // Vegetables
        Ingredient("tomato", "Vegetables", "🍅"),
        Ingredient("potato", "Vegetables", "🥔"),
        Ingredient("onion", "Vegetables", "🧅"),
        Ingredient("garlic", "Vegetables", "🧄"),
        Ingredient("pepper", "Vegetables", "🫑"),
        Ingredient("mushrooms", "Vegetables", "🍄"),
        Ingredient("carrot", "Vegetables", "🥕"),
        Ingredient("cucumber", "Vegetables", "🥒"),
        Ingredient("lettuce", "Vegetables", "🥬"),
        Ingredient("spinach", "Vegetables", "🌱"),

        // Grains, Pasta & Bread
        Ingredient("bread", "Grains & Pasta", "🍞"),
        Ingredient("pasta", "Grains & Pasta", "🍝"),
        Ingredient("rice", "Grains & Pasta", "🍚"),
        Ingredient("flour", "Grains & Pasta", "🌾"),
        Ingredient("breadcrumbs", "Grains & Pasta", "🥖"),
        Ingredient("beans", "Grains & Pasta", "🫘"),
        Ingredient("lentils", "Grains & Pasta", "🥣"),

        // Sauces & Oils
        Ingredient("ketchup", "Sauces & Oils", "🍅"),
        Ingredient("mayonnaise", "Sauces & Oils", "🥚"),
        Ingredient("mustard", "Sauces & Oils", "🍯"),
        Ingredient("tomato puree", "Sauces & Oils", "🥫"),
        Ingredient("vegetable oil", "Sauces & Oils", "🫗"),
        Ingredient("olive oil", "Sauces & Oils", "🫒"),
        Ingredient("vinegar", "Sauces & Oils", "🍾"),

        // Baking & Spices
        Ingredient("salt", "Spices & Baking", "🧂"),
        Ingredient("pepper", "Spices & Baking", "🌶️"),
        Ingredient("oregano", "Spices & Baking", "🌿"),
        Ingredient("sugar", "Spices & Baking", "🍬"),
        Ingredient("yeast", "Spices & Baking", "🍞"),
        Ingredient("baking powder", "Spices & Baking", "📦"),
        Ingredient("cinnamon", "Spices & Baking", "🫚"),

        // Desserts & Sweets
        Ingredient("chocolate", "Desserts & Sweets", "🍫"),
        Ingredient("cocoa powder", "Desserts & Sweets", "🫕"),
        Ingredient("honey", "Desserts & Sweets", "🍯"),
        Ingredient("jam", "Desserts & Sweets", "🫙"),
        Ingredient("vanilla extract", "Desserts & Sweets", "🧪"),
        Ingredient("biscuits", "Desserts & Sweets", "🍪"),
        Ingredient("syrup", "Desserts & Sweets", "🍯"),
        Ingredient("whipping cream", "Desserts & Sweets", "🍦"),
        Ingredient("coconut flakes", "Desserts & Sweets", "🥥"),

        // Nuts & Seeds
        Ingredient("walnuts", "Nuts & Seeds", "🫘"),
        Ingredient("almonds", "Nuts & Seeds", "🥜"),
        Ingredient("hazelnuts", "Nuts & Seeds", "🌰")
        )
}