# Implementation Plan

- [x] 1. Set up domain layer foundation
  - Create domain models (Meal, Ingredient) as pure Kotlin data classes
  - Define MealRepository interface with Flow-based queries and suspend mutations
  - Implement MealValidator with validation logic for meal name and ingredients
  - Create custom exception classes (ValidationException, DatabaseException, NotFoundException)
  - _Requirements: 1.1, 1.2, 6.2, 6.3, 8.1_

- [x] 2. Implement data layer with Room database
  - [x] 2.1 Create Room entities and type converters
    - Define MealEntity with Room annotations
    - Define IngredientEntity as embedded data class
    - Implement MealConverters for ingredient list serialization using Gson
    - Update AppDatabase to include MealEntity and MealConverters
    - _Requirements: 7.1, 7.3_
  
  - [x] 2.2 Implement MealDao with database operations
    - Create getAllMeals() query returning Flow sorted by name
    - Create getMealById() query returning Flow
    - Implement insertMeal(), updateMeal(), and deleteMealById() suspend functions
    - _Requirements: 2.1, 2.2, 3.1, 5.2, 7.4_
  
  - [x] 2.3 Create mapper functions for entity/model conversion
    - Implement MealEntity.toDomainModel() extension function
    - Implement Meal.toEntity() extension function
    - Implement IngredientEntity.toDomainModel() extension function
    - Implement Ingredient.toEntity() extension function
    - _Requirements: 2.1, 3.2_
  
  - [x] 2.4 Implement MealRepositoryImpl
    - Implement getMeals() with Flow mapping and error handling
    - Implement getMealById() with null checking and error handling
    - Implement addMeal() with try-catch and Result wrapping
    - Implement updateMeal() with error handling
    - Implement deleteMeal() with error handling
    - _Requirements: 1.3, 2.1, 3.1, 4.3, 5.2, 7.2, 8.2_

- [ ] 3. Implement domain use cases
  - [ ] 3.1 Create GetMealsUseCase
    - Implement operator invoke returning Flow from repository
    - _Requirements: 2.1_
  
  - [ ] 3.2 Create GetMealByIdUseCase
    - Implement operator invoke with mealId parameter returning Flow
    - _Requirements: 3.1_
  
  - [ ] 3.3 Create AddMealUseCase
    - Inject MealRepository and MealValidator
    - Implement operator invoke with validation before repository call
    - Return validation errors or repository result
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [ ] 3.4 Create UpdateMealUseCase
    - Inject MealRepository and MealValidator
    - Implement operator invoke with validation before repository call
    - _Requirements: 4.2, 4.3, 4.4_
  
  - [ ] 3.5 Create DeleteMealUseCase
    - Implement operator invoke calling repository deleteMeal
    - _Requirements: 5.2_

- [ ] 4. Implement UI state models
  - Define MealListUiState sealed interface with Loading, Success, and Error states
  - Define MealDetailUiState sealed interface with Loading, Success, and Error states
  - Define AddEditMealUiState data class with meal, loading flags, and error fields
  - _Requirements: 2.3, 3.2, 8.1, 8.5_

- [ ] 5. Implement ViewModels with state management
  - [ ] 5.1 Create MealViewModel for meal list
    - Inject GetMealsUseCase and DeleteMealUseCase
    - Initialize with Loading state and load meals in init block
    - Collect Flow from use case and update StateFlow with Success or Error
    - Implement deleteMeal function with error handling
    - _Requirements: 2.1, 2.2, 2.4, 5.2, 5.3, 8.2_
  
  - [ ] 5.2 Create MealDetailViewModel for meal details
    - Inject GetMealByIdUseCase and extract mealId from SavedStateHandle
    - Load meal in init block and update StateFlow
    - Handle meal not found scenario
    - _Requirements: 3.1, 3.2, 3.5, 8.2_
  
  - [ ] 5.3 Create AddEditMealViewModel for add/edit form
    - Inject AddMealUseCase, UpdateMealUseCase, GetMealByIdUseCase
    - Extract optional mealId from SavedStateHandle for edit mode
    - Load existing meal if mealId present
    - Implement updateMealName, updateMealNotes, addIngredient, removeIngredient functions
    - Implement saveMeal function that calls appropriate use case
    - Handle validation errors and display in UI state
    - _Requirements: 1.1, 1.2, 1.4, 4.1, 4.2, 4.4, 4.5, 6.1, 6.4, 8.1, 8.5_

- [ ] 6. Create reusable UI components
  - Implement LoadingScreen composable with CircularProgressIndicator
  - Implement ErrorScreen composable with error message and retry button
  - Implement EmptyState composable with message and action button
  - _Requirements: 2.4, 8.4_

- [ ] 7. Implement meal list screen
  - [ ] 7.1 Create MealListScreen stateful composable
    - Inject MealViewModel with hiltViewModel()
    - Collect uiState as State
    - Pass state and callbacks to MealListContent
    - _Requirements: 2.1, 2.5_
  
  - [ ] 7.2 Create MealListContent stateless composable
    - Implement Scaffold with FloatingActionButton for add meal
    - Handle Loading, Success, and Error states
    - Show EmptyState when meal list is empty
    - Render MealList for Success state with meals
    - _Requirements: 2.2, 2.3, 2.4_
  
  - [ ] 7.3 Create MealList composable with LazyColumn
    - Use items with stable keys (meal.id)
    - Render MealCard for each meal
    - _Requirements: 2.2, 2.5_
  
  - [ ] 7.4 Create MealCard composable
    - Display meal name and ingredient count
    - Implement click handler for navigation
    - Add swipe-to-delete functionality
    - Show delete confirmation dialog
    - _Requirements: 2.3, 2.5, 5.1, 5.4_

- [ ] 8. Implement meal detail screen
  - [ ] 8.1 Create MealDetailScreen stateful composable
    - Inject MealDetailViewModel with hiltViewModel()
    - Collect uiState as State
    - Pass state and callbacks to MealDetailContent
    - _Requirements: 3.1, 3.5_
  
  - [ ] 8.2 Create MealDetailContent stateless composable
    - Implement Scaffold with TopAppBar and action buttons
    - Handle Loading, Success, and Error states
    - Display meal name, notes, and ingredient list
    - Add edit and delete action buttons
    - _Requirements: 3.2, 3.3, 3.4_
  
  - [ ] 8.3 Create IngredientList composable
    - Display ingredients with quantity and unit
    - Format ingredient display (e.g., "2 cups flour")
    - _Requirements: 3.2_

- [ ] 9. Implement add/edit meal screen
  - [ ] 9.1 Create AddEditMealScreen stateful composable
    - Inject AddEditMealViewModel with hiltViewModel()
    - Collect uiState as State
    - Handle navigation after successful save
    - _Requirements: 1.1, 4.1_
  
  - [ ] 9.2 Create AddEditMealContent stateless composable
    - Implement Scaffold with TopAppBar and save button
    - Create form with meal name TextField
    - Create notes TextField (optional)
    - Add ingredient list section with add/remove functionality
    - Display validation errors inline
    - Show loading indicator while saving
    - _Requirements: 1.1, 1.2, 4.1, 4.2, 4.5, 6.1, 6.4, 8.1_
  
  - [ ] 9.3 Create IngredientInput composable
    - Create form for ingredient name, quantity, and unit
    - Implement add button to add ingredient to list
    - Display current ingredient list with remove buttons
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 10. Set up navigation routes
  - Define Screen sealed class with MealList, MealDetail, AddMeal, and EditMeal routes
  - Implement route creation functions with parameters
  - Add composable destinations to NavHost
  - Wire up navigation callbacks in screens
  - _Requirements: 2.5, 3.3, 3.4_

- [ ] 11. Configure dependency injection
  - Create MealModule providing MealValidator
  - Create MealRepositoryModule binding MealRepository to MealRepositoryImpl
  - Update DatabaseModule to provide MealDao from AppDatabase
  - Verify Hilt generates all necessary components
  - _Requirements: 1.3, 2.1, 7.1_

- [ ] 12. Write unit tests for domain layer
  - [ ] 12.1 Test MealValidator
    - Test validation passes for valid meal
    - Test validation fails for empty meal name
    - Test validation fails for empty ingredient list
    - Test validation fails for ingredient with empty name
    - _Requirements: 1.1, 1.2, 6.2_
  
  - [ ] 12.2 Test use cases with fake repository
    - Test GetMealsUseCase returns meals from repository
    - Test AddMealUseCase validates before adding
    - Test UpdateMealUseCase validates before updating
    - Test DeleteMealUseCase calls repository
    - _Requirements: 1.1, 1.3, 4.3, 5.2_

- [ ] 13. Write unit tests for ViewModels
  - [ ] 13.1 Test MealViewModel
    - Test initial state is Loading
    - Test loads meals successfully and updates to Success state
    - Test handles error and updates to Error state
    - Test deleteMeal calls use case
    - _Requirements: 2.1, 5.2, 8.2_
  
  - [ ] 13.2 Test MealDetailViewModel
    - Test loads meal successfully
    - Test handles meal not found error
    - _Requirements: 3.1, 3.5_
  
  - [ ] 13.3 Test AddEditMealViewModel
    - Test loads existing meal in edit mode
    - Test updateMealName updates state
    - Test addIngredient adds to list
    - Test removeIngredient removes from list
    - Test saveMeal validates and calls appropriate use case
    - _Requirements: 1.1, 4.1, 4.5, 6.1, 6.4_

- [ ] 14. Write instrumented tests for data layer
  - [ ] 14.1 Test MealDao with in-memory database
    - Test insertMeal and getAllMeals
    - Test getMealById returns correct meal
    - Test updateMeal modifies existing meal
    - Test deleteMealById removes meal
    - Test getAllMeals returns meals sorted by name
    - _Requirements: 2.1, 2.2, 3.1, 5.2, 7.3_
  
  - [ ] 14.2 Test MealRepositoryImpl
    - Test getMeals maps entities to domain models
    - Test addMeal persists to database
    - Test error handling for database exceptions
    - _Requirements: 1.3, 7.2, 8.2_
