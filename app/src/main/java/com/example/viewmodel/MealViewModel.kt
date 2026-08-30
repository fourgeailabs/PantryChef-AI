package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.RetrofitClient
import com.example.data.SavedMealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class RecipeResult(
    val title: String,
    val description: String,
    val servings: Int,
    val ingredientsHave: String,
    val ingredientsNeeded: String,
    val instructions: String,
    val sources: String
)

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.savedMealDao()

    val savedMeals: Flow<List<SavedMealEntity>> = dao.getAllSavedMeals()

    private val _ingredients = MutableStateFlow<List<String>>(listOf("Chicken breasts", "Garlic", "Olive oil", "Rice", "Tomatoes"))
    val ingredients: StateFlow<List<String>> = _ingredients.asStateFlow()

    private val _peopleCount = MutableStateFlow(2)
    val peopleCount: StateFlow<Int> = _peopleCount.asStateFlow()

    private val _dietaryPref = MutableStateFlow("None")
    val dietaryPref: StateFlow<String> = _dietaryPref.asStateFlow()

    private val _generatedMeal = MutableStateFlow<RecipeResult?>(null)
    val generatedMeal: StateFlow<RecipeResult?> = _generatedMeal.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addIngredient(ingredient: String) {
        if (ingredient.isNotBlank() && !_ingredients.value.contains(ingredient.trim())) {
            _ingredients.value = _ingredients.value + ingredient.trim()
        }
    }

    fun removeIngredient(ingredient: String) {
        _ingredients.value = _ingredients.value - ingredient
    }

    fun setPeopleCount(count: Int) {
        if (count in 1..20) {
            _peopleCount.value = count
        }
    }

    fun setDietaryPref(pref: String) {
        _dietaryPref.value = pref
    }

    fun generateMeal() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _errorMessage.value = "Gemini API key is missing. Please set it in AI Studio Secrets panel."
                    _isLoading.value = false
                    return@launch
                }

                val ingredientsListStr = _ingredients.value.joinToString(", ")
                val people = _peopleCount.value
                val dietary = _dietaryPref.value

                val prompt = """
                    You are an expert chef and AI meal planner. Create a delicious, complete recipe for exactly $people people based on the following ingredients available on hand: [$ingredientsListStr].
                    Dietary preference / restriction: $dietary.
                    
                    Please structure your response clearly using the following exact labels so it can be parsed easily:
                    TITLE: [Recipe Name]
                    DESCRIPTION: [Short appetizing description]
                    SERVINGS: [$people]
                    INGREDIENTS_HAVE: [List ingredients used from the on-hand list with exact quantities scaled for $people people]
                    INGREDIENTS_NEEDED: [List any additional pantry staples or extra ingredients needed with exact quantities scaled for $people people]
                    INSTRUCTIONS: [Numbered step-by-step cooking instructions]
                    SOURCES: [Brief mention of culinary inspiration or web search results if applicable]
                """.trimIndent()

                val jsonBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": ${JSONObject.quote(prompt)}
                            }
                          ]
                        }
                      ],
                      "tools": [
                        {
                          "googleSearch": {}
                        }
                      ]
                    }
                """.trimIndent()

                val body = jsonBody.toRequestBody("application/json".toMediaType())
                val responseBody = RetrofitClient.service.generateContent(apiKey, body)
                val rawJson = responseBody.string()

                val root = JSONObject(rawJson)
                val candidates = root.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "No response text.") ?: "No response text."

                val recipe = parseRecipeText(textResponse, people)
                _generatedMeal.value = recipe
            } catch (e: Exception) {
                _errorMessage.value = "Error generating meal: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseRecipeText(text: String, defaultServings: Int): RecipeResult {
        var title = "AI Generated Meal"
        var description = text.take(150) + "..."
        var servings = defaultServings
        var have = "See full instructions"
        var needed = "None"
        var instructions = text
        var sources = "AI & Google Search Grounding"

        try {
            val lines = text.lines()
            var currentSection = ""
            val haveBuilder = StringBuilder()
            val neededBuilder = StringBuilder()
            val instBuilder = StringBuilder()
            val sourceBuilder = StringBuilder()

            for (line in lines) {
                when {
                    line.startsWith("TITLE:", true) -> {
                        title = line.substringAfter(":").trim()
                        currentSection = "TITLE"
                    }
                    line.startsWith("DESCRIPTION:", true) -> {
                        description = line.substringAfter(":").trim()
                        currentSection = "DESC"
                    }
                    line.startsWith("SERVINGS:", true) -> {
                        val s = line.substringAfter(":").trim().toIntOrNull()
                        if (s != null) servings = s
                        currentSection = "SERVINGS"
                    }
                    line.startsWith("INGREDIENTS_HAVE:", true) -> {
                        currentSection = "HAVE"
                        haveBuilder.append(line.substringAfter(":").trim()).append("\n")
                    }
                    line.startsWith("INGREDIENTS_NEEDED:", true) -> {
                        currentSection = "NEEDED"
                        neededBuilder.append(line.substringAfter(":").trim()).append("\n")
                    }
                    line.startsWith("INSTRUCTIONS:", true) -> {
                        currentSection = "INST"
                        instBuilder.append(line.substringAfter(":").trim()).append("\n")
                    }
                    line.startsWith("SOURCES:", true) -> {
                        currentSection = "SOURCE"
                        sourceBuilder.append(line.substringAfter(":").trim()).append("\n")
                    }
                    else -> {
                        when (currentSection) {
                            "HAVE" -> haveBuilder.append(line).append("\n")
                            "NEEDED" -> neededBuilder.append(line).append("\n")
                            "INST" -> instBuilder.append(line).append("\n")
                            "SOURCE" -> sourceBuilder.append(line).append("\n")
                            "DESC" -> description += " " + line
                        }
                    }
                }
            }

            if (haveBuilder.isNotEmpty()) have = haveBuilder.toString().trim()
            if (neededBuilder.isNotEmpty()) needed = neededBuilder.toString().trim()
            if (instBuilder.isNotEmpty()) instructions = instBuilder.toString().trim()
            if (sourceBuilder.isNotEmpty()) sources = sourceBuilder.toString().trim()
        } catch (_: Exception) {}

        return RecipeResult(
            title = title,
            description = description,
            servings = servings,
            ingredientsHave = have,
            ingredientsNeeded = needed,
            instructions = instructions,
            sources = sources
        )
    }

    fun saveCurrentMeal() {
        val meal = _generatedMeal.value ?: return
        viewModelScope.launch {
            dao.insertMeal(
                SavedMealEntity(
                    title = meal.title,
                    description = meal.description,
                    servings = meal.servings,
                    ingredientsHave = meal.ingredientsHave,
                    ingredientsNeeded = meal.ingredientsNeeded,
                    instructions = meal.instructions,
                    sources = meal.sources
                )
            )
        }
    }

    fun deleteSavedMeal(meal: SavedMealEntity) {
        viewModelScope.launch {
            dao.deleteMeal(meal)
        }
    }
}
