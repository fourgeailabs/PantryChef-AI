package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.RetrofitClient
import com.example.data.SavedMealEntity
import com.example.data.ShoppingItemEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
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

data class GroceryItemPrice(
    val name: String,
    val recommendedVersion: String,
    val price: Double
)

data class StoreResult(
    val name: String,
    val address: String,
    val distance: String,
    val items: List<GroceryItemPrice>,
    val totalCost: Double
)

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val mealDao = database.savedMealDao()
    private val shoppingDao = database.shoppingItemDao()

    val savedMeals: Flow<List<SavedMealEntity>> = mealDao.getAllSavedMeals()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllShoppingItems()

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

    // Location State
    private val _userManualLocation = MutableStateFlow("")
    val userManualLocation: StateFlow<String> = _userManualLocation.asStateFlow()

    private val _detectedLocation = MutableStateFlow<Location?>(null)
    val detectedLocation: StateFlow<Location?> = _detectedLocation.asStateFlow()

    // Grocery Comparison Results
    private val _groceryCompareResults = MutableStateFlow<List<StoreResult>>(emptyList())
    val groceryCompareResults: StateFlow<List<StoreResult>> = _groceryCompareResults.asStateFlow()

    private val _isComparingPrices = MutableStateFlow(false)
    val isComparingPrices: StateFlow<Boolean> = _isComparingPrices.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

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

    fun setManualLocation(loc: String) {
        _userManualLocation.value = loc
    }

    // Shopping List Operations
    fun addShoppingItem(name: String, qty: String = "") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                shoppingDao.insertItem(ShoppingItemEntity(name = name.trim(), quantity = qty.trim()))
            }
        }
    }

    fun toggleShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingDao.updateItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingDao.deleteItem(item)
        }
    }

    fun deleteCheckedItems() {
        viewModelScope.launch {
            shoppingDao.deleteCheckedItems()
        }
    }

    fun clearShoppingList() {
        viewModelScope.launch {
            shoppingDao.clearAll()
        }
    }

    fun importNeededIngredients() {
        val meal = _generatedMeal.value ?: return
        viewModelScope.launch {
            val lines = meal.ingredientsNeeded.lines()
            for (line in lines) {
                val cleanLine = line.trim().removePrefix("-").removePrefix("•").trim()
                if (cleanLine.isNotBlank() && !cleanLine.startsWith("None", true)) {
                    // Extract name and quantity if possible (e.g. "2 cloves Garlic" -> name: "Garlic", quantity: "2 cloves")
                    val parts = cleanLine.split(" ", limit = 2)
                    if (parts.size > 1 && parts[0].any { it.isDigit() }) {
                        shoppingDao.insertItem(ShoppingItemEntity(name = parts[1], quantity = parts[0]))
                    } else {
                        shoppingDao.insertItem(ShoppingItemEntity(name = cleanLine))
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchDeviceLocation(onComplete: () -> Unit = {}) {
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    _detectedLocation.value = location
                }
                onComplete()
            }.addOnFailureListener {
                onComplete()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun compareGroceryPrices(shoppingItemsList: List<ShoppingItemEntity>) {
        if (shoppingItemsList.isEmpty()) {
            _errorMessage.value = "Shopping list is empty! Please add some items to compare prices."
            return
        }

        viewModelScope.launch {
            _isComparingPrices.value = true
            _errorMessage.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _errorMessage.value = "Gemini API key is missing. Please set it in AI Studio Secrets."
                    _isComparingPrices.value = false
                    return@launch
                }

                // Determine query location
                val latLng = _detectedLocation.value
                val locationQuery = when {
                    _userManualLocation.value.isNotBlank() -> _userManualLocation.value
                    latLng != null -> "Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}"
                    else -> "Los Angeles, CA (default fallback - grant GPS permissions or type city/ZIP above)"
                }

                val itemsString = shoppingItemsList.joinToString(", ") { "${it.quantity} ${it.name}".trim() }

                val prompt = """
                    You are an expert shopping assistant and price matching engine. Locate the nearest 4 grocery stores to the following location: $locationQuery.
                    For each of these 4 stores, find the real-world or estimated prices (ordered from lowest to highest total and individual price for the recommended brand/version of each item) of these items: [$itemsString].
                    
                    Return STRICTLY a raw JSON object matching this schema exactly. Do not include any markdown wrappers (like ```json) or explanation.
                    {
                      "stores": [
                        {
                          "name": "Store Name",
                          "address": "Store Address",
                          "distance": "e.g. 1.2 miles",
                          "items": [
                            {
                              "name": "Item Name",
                              "recommended": "Recommended brand and packaging",
                              "price": 2.99
                            }
                          ]
                        }
                      ]
                    }
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
                      ],
                      "generationConfig": {
                        "temperature": 0.2
                      }
                    }
                """.trimIndent()

                val body = jsonBody.toRequestBody("application/json".toMediaType())
                
                var responseBody: ResponseBody? = null
                var retries = 3
                var delayTime = 1000L // Start with 1 second delay
                
                while (retries > 0) {
                    try {
                        responseBody = RetrofitClient.service.generateContent(apiKey, body)
                        break // Success!
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 429 && retries > 1) {
                            retries--
                            kotlinx.coroutines.delay(delayTime)
                            delayTime *= 2 // Exponential backoff
                        } else {
                            throw e
                        }
                    }
                }
                var rawJson = responseBody?.string() ?: throw Exception("No response from Gemini API.")

                // Parse the search grounded response containing raw json
                val root = JSONObject(rawJson)
                val candidates = root.optJSONArray("candidates")
                var textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "{}") ?: "{}"

                // Sanitize response from potential markdown format
                if (textResponse.contains("```")) {
                    textResponse = textResponse.substringAfter("```json").substringBefore("```").trim()
                }

                val parsedStores = mutableListOf<StoreResult>()
                val responseJson = JSONObject(textResponse)
                val storesArray = responseJson.optJSONArray("stores") ?: JSONArray()

                for (i in 0 until storesArray.length()) {
                    val storeObj = storesArray.getJSONObject(i)
                    val name = storeObj.optString("name", "Unknown Store")
                    val address = storeObj.optString("address", "Nearby")
                    val distance = storeObj.optString("distance", "N/A")
                    val itemsArray = storeObj.optJSONArray("items") ?: JSONArray()

                    val itemsList = mutableListOf<GroceryItemPrice>()
                    var storeTotal = 0.0

                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        val itemName = itemObj.optString("name", "")
                        val recommended = itemObj.optString("recommended", "Standard Brand")
                        val price = itemObj.optDouble("price", 0.0)

                        itemsList.add(GroceryItemPrice(itemName, recommended, price))
                        storeTotal += price
                    }

                    // Sort items from lowest to highest price inside each store
                    val sortedItemsList = itemsList.sortedBy { it.price }

                    parsedStores.add(StoreResult(name, address, distance, sortedItemsList, storeTotal))
                }

                // Sort stores by lowest total cost
                _groceryCompareResults.value = parsedStores.sortedBy { it.totalCost }

            } catch (e: Exception) {
                _errorMessage.value = "Error comparing prices: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isComparingPrices.value = false
            }
        }
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
                
                var responseBody: ResponseBody? = null
                var retries = 3
                var delayTime = 1000L // Start with 1 second delay
                
                while (retries > 0) {
                    try {
                        responseBody = RetrofitClient.service.generateContent(apiKey, body)
                        break // Success!
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 429 && retries > 1) {
                            retries--
                            kotlinx.coroutines.delay(delayTime)
                            delayTime *= 2 // Exponential backoff
                        } else {
                            throw e
                        }
                    }
                }
                val rawJson = responseBody?.string() ?: throw Exception("No response from Gemini API.")

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
            mealDao.insertMeal(
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
            mealDao.deleteMeal(meal)
        }
    }
}
