package com.example.nextmeal

// Модел за намирница со категорија
data class Ingredient(val name: String, val category: String)

object IngredientsData {
    val list = listOf(
        // Месо, Риба & Протеини (Meat & Proteins)
        Ingredient("пилешко", "Месо, Риба & Протеини"),
        Ingredient("свинско", "Месо, Риба & Протеини"),
        Ingredient("телешко", "Месо, Риба & Протеини"),
        Ingredient("мелено месо", "Месо, Риба & Протеини"),
        Ingredient("сланина", "Месо, Риба & Протеини"),
        Ingredient("салама", "Месо, Риба & Протеини"),
        Ingredient("виршли", "Месо, Риба & Протеини"),
        Ingredient("јајца", "Месо, Риба & Протеини"),
        Ingredient("туна", "Месо, Риба & Протеини"),

        // Млечни производи (Dairy Products)
        Ingredient("кашкавал", "Млечни производи"),
        Ingredient("сирење", "Млечни производи"),
        Ingredient("млеко", "Млечни производи"),
        Ingredient("јогурт", "Млечни производи"),
        Ingredient("павлака", "Млечни производи"),
        Ingredient("путер", "Млечни производи"),
        Ingredient("маргарин", "Млечни производи"),
        Ingredient("кремаст сирење", "Млечни производи"),

        // Зеленчук & Свежи намирници (Vegetables)
        Ingredient("домат", "Зеленчук"),
        Ingredient("компир", "Зеленчук"),
        Ingredient("кромид", "Зеленчук"),
        Ingredient("лук", "Зеленчук"),
        Ingredient("пиперка", "Зеленчук"),
        Ingredient("печурки", "Зеленчук"),
        Ingredient("морков", "Зеленчук"),
        Ingredient("краставица", "Зеленчук"),
        Ingredient("марула", "Зеленчук"),
        Ingredient("спанаќ", "Зеленчук"),

        // Тестенини, Зрнести & База (Grains, Pasta & Bread)
        Ingredient("леб", "Тестенини & Зрнести"),
        Ingredient("тестенини", "Тестенини & Зрнести"),
        Ingredient("ориз", "Тестенини & Зрнести"),
        Ingredient("брашно", "Тестенини & Зрнести"),
        Ingredient("галета", "Тестенини & Зрнести"),
        Ingredient("грав", "Тестенини & Зрнести"),
        Ingredient("леќа", "Тестенини & Зрнести"),

        // Сосови, Масла & Кондименти (Sauces & Oils)
        Ingredient("кечап", "Сосови & Масла"),
        Ingredient("мајонез", "Сосови & Масла"),
        Ingredient("сенф", "Сосови & Масла"),
        Ingredient("доматно пире", "Сосови & Масла"),
        Ingredient("зејтин", "Сосови & Масла"),
        Ingredient("маслиново масло", "Сосови & Масла"),
        Ingredient("оцет", "Сосови & Масла"),

        // Зачини & Печење (Baking & Spices)
        Ingredient("сол", "Зачини & База"),
        Ingredient("црн бибер", "Зачини & База"),
        Ingredient("вегета", "Зачини & База"),
        Ingredient("црвен пипер", "Зачини & База"),
        Ingredient("оригано", "Зачини & База"),
        Ingredient("шеќер", "Зачини & База"),
        Ingredient("квасец", "Зачини & База"),
        Ingredient("пециво", "Зачини & База")
    )
}