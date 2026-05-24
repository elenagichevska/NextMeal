package com.example.nextmeal

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ApiMeal(
    val strMeal: String,
    val strMealThumb: String,
    val idMeal: String,
    val strInstructions: String? = null,
    val strIngredient1: String? = null,
    val strIngredient2: String? = null,
    val strIngredient3: String? = null,
    val strIngredient4: String? = null,
    val strIngredient5: String? = null,
    val strIngredient6: String? = null,
    val strIngredient7: String? = null,
    val strIngredient8: String? = null,
    val strIngredient9: String? = null,
    val strIngredient10: String? = null,
) {
    // 💡 ЈА ДОДАВАМЕ ТУКА: Оваа функција ги собира состојките во еден текст одделен со запирки
    fun getFormattedIngredients(): String {
        val list = listOfNotNull(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5
        ).filter { it.trim().isNotEmpty() }

        return if (list.isEmpty()) "Ingredients look-up complete" else list.joinToString(", ")
    }
}

data class MealResponse(
    val meals: List<ApiMeal>?
)

interface MealApiService {
    // Ова останува за Фрижидерот (по состојка)
    @GET("filter.php")
    suspend fun getMealsByIngredient(
        @Query("i") ingredient: String
    ): MealResponse

    // Ова е за Глобално (по име на јадење)
    @GET("search.php")
    suspend fun searchMealsByName(
        @Query("s") name: String
    ): MealResponse

    // 🛠️ ПОПРАВЕНО: Сменето е од @Query("id") во @Query("i") за да работи со TheMealDB
    @GET("lookup.php")
    suspend fun getMealDetailsById(
        @Query("i") id: String
    ): MealResponse

    companion object {
        private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

        val instance: MealApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MealApiService::class.java)
        }
    }
}