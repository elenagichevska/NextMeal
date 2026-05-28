package com.example.nextmeal

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import android.content.Context
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "local_recipes")
class LocalRecipe {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var title: String = ""
    var ingredients: String = ""
    var instructions: String = ""
    var isPublic: Boolean = false
    var imageUrl: String = ""
    var userId: String = ""
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM local_recipes WHERE userId = :currentUid ORDER BY id DESC")
    fun getLocalRecipesForUser(currentUid: String): Flow<List<LocalRecipe>>

    @Insert
    fun insertRecipe(recipe: LocalRecipe)

    @Update
    fun updateRecipe(recipe: LocalRecipe)

    @Delete
    fun deleteRecipe(recipe: LocalRecipe)
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
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}