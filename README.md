# AI Meal Generator

An advanced Android application powered by Gemini AI and Google Search grounding to help users generate delicious recipes based on ingredients they currently have on hand and the exact number of people eating.

## Features
- **Ingredients Inventory**: Input and manage what ingredients are currently in your pantry and fridge.
- **Portion Scaling**: Specify how many people will be eating to ensure proper recipe proportions and prevent food shortages.
- **AI & Google Search Grounding**: Leverages Gemini 3.5 Flash with Google Search grounding for fresh, real-world recipe suggestions, cooking instructions, and nutritional insights.
- **Smart Shopping List**: Custom checklist tab to track all extra ingredients you need to buy.
- **Instant Ingredient Import**: Click "Import Needed" from any generated recipe to automatically transfer missing items directly to your Shopping List.
- **GPS Grocery Store Price Matcher**: Uses Android GPS (or manual ZIP/City override) to find the 4 nearest grocery stores using Google Search grounding.
- **Itemized Price Comparison**: Fetches prices of the recommended brand version of each ingredient, sorting stores and item costs from lowest to highest.
- **Saved Recipes**: Save your favorite generated meals locally using Room database.
- **What's New & Release History**: Interactive accordion menu tracking historical app updates and version logs.
- **About Section**: Information about app creator **FourgeAI LABS** (`https://github.com/fourgeailabs`) and app repository link.

## Version History
### Version 1.01.00 (Current Update)
- Added visual Shopping List checklist with manual item additions and toggles.
- Added recipe integration to import needed ingredients instantly.
- Added FusedLocation service with dynamic GPS permission requests.
- Added ZIP code or City fallback entry for manual location lookup.
- Integrated Gemini 3.5 Flash with Google Search grounding to match prices at the nearest 4 grocery stores, sorted lowest-to-highest.

### Version 1.00.00 (Initial Release)
- Initial release of AI Meal Generator with Gemini AI integration, Google Search grounding, ingredient inventory management, portion scaling, saved recipes, What's New accordion, and About section.
- App Creator: FourgeAI LABS

