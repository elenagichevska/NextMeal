package com.example.nextmeal

import androidx.annotation.StringRes

// Модел за намирница кој содржи клуч за база, ID за превод на категорија и ID за превод на името
data class Ingredient(
    val name: String, // Ова останува "chicken" за споредба со API/база во позадина
    @StringRes val nameRes: Int, // Превод за името на екранот
    @StringRes val categoryRes: Int, // Превод за категоријата на екранот
    val emoji: String
)

object IngredientsData {
    val list = listOf(
        // Meat, Fish & Proteins
        Ingredient("chicken", R.string.ing_chicken, R.string.cat_meat_proteins, "🍗"),
        Ingredient("pork", R.string.ing_pork, R.string.cat_meat_proteins, "🥩"),
        Ingredient("beef", R.string.ing_beef, R.string.cat_meat_proteins, "🍖"),
        Ingredient("minced beef", R.string.ing_minced_beef, R.string.cat_meat_proteins, "🍔"),
        Ingredient("bacon", R.string.ing_bacon, R.string.cat_meat_proteins, "🥓"),
        Ingredient("salami", R.string.ing_salami, R.string.cat_meat_proteins, "🍕"),
        Ingredient("sausage", R.string.ing_sausage, R.string.cat_meat_proteins, "🌭"),
        Ingredient("egg", R.string.ing_egg, R.string.cat_meat_proteins, "🥚"),
        Ingredient("tuna", R.string.ing_tuna, R.string.cat_meat_proteins, "🐟"),

        // Dairy Products
        Ingredient("cheese", R.string.ing_cheese, R.string.cat_dairy_products, "🧀"),
        Ingredient("cheddar", R.string.ing_cheddar, R.string.cat_dairy_products, "🧀"),
        Ingredient("milk", R.string.ing_milk, R.string.cat_dairy_products, "🥛"),
        Ingredient("yogurt", R.string.ing_yogurt, R.string.cat_dairy_products, "🥛"),
        Ingredient("sour cream", R.string.ing_sour_cream, R.string.cat_dairy_products, "🥣"),
        Ingredient("butter", R.string.ing_butter, R.string.cat_dairy_products, "🧈"),
        Ingredient("margarine", R.string.ing_margarine, R.string.cat_dairy_products, "🧈"),
        Ingredient("cream cheese", R.string.ing_cream_cheese, R.string.cat_dairy_products, "🥣"),

        // Vegetables
        Ingredient("tomato", R.string.ing_tomato, R.string.cat_vegetables, "🍅"),
        Ingredient("potato", R.string.ing_potato, R.string.cat_vegetables, "🥔"),
        Ingredient("onion", R.string.ing_onion, R.string.cat_vegetables, "🧅"),
        Ingredient("garlic", R.string.ing_garlic, R.string.cat_vegetables, "🧄"),
        Ingredient("pepper", R.string.ing_pepper, R.string.cat_vegetables, "𫠑"),
        Ingredient("mushrooms", R.string.ing_mushrooms, R.string.cat_vegetables, "🍄"),
        Ingredient("carrot", R.string.ing_carrot, R.string.cat_vegetables, "🥕"),
        Ingredient("cucumber", R.string.ing_cucumber, R.string.cat_vegetables, "🥒"),
        Ingredient("lettuce", R.string.ing_lettuce, R.string.cat_vegetables, "🥬"),
        Ingredient("spinach", R.string.ing_spinach, R.string.cat_vegetables, "🌱"),

        // Grains, Pasta & Bread
        Ingredient("bread", R.string.ing_bread, R.string.cat_grains_pasta, "🍞"),
        Ingredient("pasta", R.string.ing_pasta, R.string.cat_grains_pasta, "🍝"),
        Ingredient("rice", R.string.ing_rice, R.string.cat_grains_pasta, "🍚"),
        Ingredient("flour", R.string.ing_flour, R.string.cat_grains_pasta, "🌾"),
        Ingredient("breadcrumbs", R.string.ing_breadcrumbs, R.string.cat_grains_pasta, "🥖"),
        Ingredient("beans", R.string.ing_beans, R.string.cat_grains_pasta, "🫘"),
        Ingredient("lentils", R.string.ing_lentils, R.string.cat_grains_pasta, "🥣"),

        // Sauces & Oils
        Ingredient("ketchup", R.string.ing_ketchup, R.string.cat_sauces_oils, "🍅"),
        Ingredient("mayonnaise", R.string.ing_mayonnaise, R.string.cat_sauces_oils, "🥚"),
        Ingredient("mustard", R.string.ing_mustard, R.string.cat_sauces_oils, "🍯"),
        Ingredient("tomato puree", R.string.ing_tomato_puree, R.string.cat_sauces_oils, "🥫"),
        Ingredient("vegetable oil", R.string.ing_vegetable_oil, R.string.cat_sauces_oils, "🫗"),
        Ingredient("olive oil", R.string.ing_olive_oil, R.string.cat_sauces_oils, "🫒"),
        Ingredient("vinegar", R.string.ing_vinegar, R.string.cat_sauces_oils, "🍾"),

        // Baking & Spices
        Ingredient("salt", R.string.ing_salt, R.string.cat_spices_baking, "🧂"),
        Ingredient("pepper", R.string.ing_spice_pepper, R.string.cat_spices_baking, "🌶️"),
        Ingredient("oregano", R.string.ing_oregano, R.string.cat_spices_baking, "🌿"),
        Ingredient("sugar", R.string.ing_sugar, R.string.cat_spices_baking, "🍬"),
        Ingredient("yeast", R.string.ing_yeast, R.string.cat_spices_baking, "🍞"),
        Ingredient("baking powder", R.string.ing_baking_powder, R.string.cat_spices_baking, "📦"),
        Ingredient("cinnamon", R.string.ing_cinnamon, R.string.cat_spices_baking, "🫚"),

        // Desserts & Sweets
        Ingredient("chocolate", R.string.ing_chocolate, R.string.cat_desserts_sweets, "🍫"),
        Ingredient("cocoa powder", R.string.ing_cocoa_powder, R.string.cat_desserts_sweets, "🫕"),
        Ingredient("honey", R.string.ing_honey, R.string.cat_desserts_sweets, "🍯"),
        Ingredient("jam", R.string.ing_jam, R.string.cat_desserts_sweets, "🫙"),
        Ingredient("vanilla extract", R.string.ing_vanilla_extract, R.string.cat_desserts_sweets, "🧪"),
        Ingredient("biscuits", R.string.ing_biscuits, R.string.cat_desserts_sweets, "🍪"),
        Ingredient("syrup", R.string.ing_syrup, R.string.cat_desserts_sweets, "🍯"),
        Ingredient("whipping cream", R.string.ing_whipping_cream, R.string.cat_desserts_sweets, "🍦"),
        Ingredient("coconut flakes", R.string.ing_coconut_flakes, R.string.cat_desserts_sweets, "🥥"),

        // Nuts & Seeds
        Ingredient("walnuts", R.string.ing_walnuts, R.string.cat_nuts_seeds, "🫘"),
        Ingredient("almonds", R.string.ing_almonds, R.string.cat_nuts_seeds, "🥜"),
        Ingredient("hazelnuts", R.string.ing_hazelnuts, R.string.cat_nuts_seeds, "🌰")
    )
}