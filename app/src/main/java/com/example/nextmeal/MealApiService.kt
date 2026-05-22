package com.example.nextmeal

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ApiMeal(
    val strMeal: String,
    val strMealThumb: String,
    val idMeal: String
)

data class MealResponse(
    val meals: List<ApiMeal>?
)

interface MealApiService {
    // Ова останува за Фрижидерот (по состојка)
    @GET("filter.php")
    suspend fun getMealsByIngredient(
        @Query("i") ingredient: String
    ): MealResponse

    // ОВА ГО ДОДАВАМЕ за Глобално (по име на јадење)
    @GET("search.php")
    suspend fun searchMealsByName(
        @Query("s") name: String
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