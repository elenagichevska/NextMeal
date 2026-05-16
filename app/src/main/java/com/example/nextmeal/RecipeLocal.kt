package com.example.nextmeal

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "local_recipes")
class LocalRecipe {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    var title: String = ""
    var ingredients: String = ""
    var instructions: String = ""
    var isPublic: Boolean = false // Ова вака напишано KSP нема да го збуни
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM local_recipes ORDER BY id DESC")
    fun getAllLocalRecipes(): Flow<List<LocalRecipe>>

    @Insert
    fun insertRecipe(recipe: LocalRecipe)
}

@Database(entities = [LocalRecipe::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nextmeal_database"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Ова го оставаме за да ни биде сигурно во MainActivity
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}