# Implementation Plan

- [ ] 1. Set up domain layer foundation
  - Create domain models (MealPlan, MealType enum, WeekPlanData, MealPlanWithMeal) as pure Kotlin data classes
  - Define MealPlanRepository interface with Flow-based queries and suspend mutations
  - Create custom exception classes (ConflictException for duplicate slots)
  - _Requirements: 1.1, 2.1, 8.1_

- [ ] 2. Implement data layer with Room database
  - [ ] 2.1 Create Room entities and type converters
    - Define MealPlanEntity with Room annotations and foreign key to MealEntity
    - Add type converters for LocalDate (toEpochDay/fromEpochDay)
    - Update AppDatabase to version 3 with MealPlanEntity
    - Create database migration from version 2 to 3
    - _Requirements: 2.3, 8.3, 8.4_
  
  - [ ] 2.2 Implement MealPlanDao with database operations
    - Create getMealPlansForWeek() query with date range returning Flow
    - Create getMealPlansForDate() query returning Flow
    - Create getMealPlanById() query returning Flow
    - Implement insertMealPlan() with REPLACE conflict strategy
    - Implement insertMealPlans() for batch operations
    - Implement updateMealPlan() suspend function
    - Implement deleteMealPlanById() suspend function
    - Implement deleteMealPlansForDate() for clearing day
    - Add unique index on (date, meal_type) to prevent double booking
    - Add indexes on date and meal_id for query performance
    - _Requirements: 1.2, 2.1, 3.2, 4.3, 5.2, 7.1, 10.3_
  
  - [ ] 2.3 Create mapper functions for entity/model conversion
    - Implement MealPlanEntity.toDomainModel() extension function
    - Implement MealPlan.toEntity() extension function
    - Handle LocalDate to epoch day conversions
    - Handle MealType enum to string conversions
    - _Requirements: 1.4, 2.4_
  
  - [ ] 2.4 Implement MealPlanRepositoryImpl
    - Implement getMealPlansForWeek() with Flow mapping and error handling
    - Implement getMealPlansForDate() with Flow mapping
    - Implement getMealPlanById() with null checking
    - Implement addMealPlan() with try-catch and Result wrapping
    - Implement addMealPlans() for batch inserts
    - Implement updateMealPlan() with error handling
    - Implement deleteMealPlan() with error handling
    - Implement deleteMealPlansForDate() with error handling
    - _Requirements: 2.3, 3.3, 4.4, 5.3, 7.2, 8.2, 10.2_

- [ ] 3. Implement domain use cases
  - [ ] 3.1 Create GetMealPlansForWeekUseCase
    - Inject MealPlanRepository and MealRepository
    - Implement operator invoke that combines meal plans with meal details
    - Calculate week start and end dates (Monday to Sunday)
    - Group MealPlanWithMeal by date in WeekPlanData
    - Handle cases where meals are deleted but plans exist
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.2_
  
  - [ ] 3.2 Create GetMealPlansForDateUseCase
    - Inject MealPlanRepository and MealRepository
    - Implement operator invoke combining plans with meal details for single date
    - _Requirements: 6.2, 10.1_
  
  - [ ] 3.3 Create AssignMealToPlanUseCase
    - Inject MealPlanRepository
    - Implement operator invoke creating MealPlan from mealId, date, and mealType
    - Call repository addMealPlan
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  
  - [ ] 3.4 Create UpdateMealPlanUseCase
    - Inject MealPlanRepository
    - Implement operator invoke to replace meal in existing plan
    - Fetch existing plan, update mealId, call repository updateMealPlan
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ] 3.5 Create DeleteMealPlanUseCase
    - Inject MealPlanRepository
    - Implement operator invoke calling repository deleteMealPlan
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [ ] 3.6 Create CopyDayPlansUseCase
    - Inject MealPlanRepository
    - Implement operator invoke to copy all plans from source date to target date
    - Support replaceExisting flag to clear target date first
    - Create new MealPlan instances with target date
    - Use batch insert for efficiency
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ] 3.7 Create ClearDayPlansUseCase
    - Inject MealPlanRepository
    - Implement operator invoke calling repository deleteMealPlansForDate
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 4. Implement UI state models
  - Define MealPlannerUiState data class with weekData, currentWeekStart, loading, error, and meal selection fields
  - Define MealSlot data class representing a calendar slot with date, mealType, and optional existing plan
  - _Requirements: 1.1, 1.5, 2.1, 8.1_

- [ ] 5. Implement MealPlannerViewModel with state management
  - Inject all use cases (GetMealPlansForWeekUseCase, GetMealsUseCase, AssignMealToPlanUseCase, UpdateMealPlanUseCase, DeleteMealPlanUseCase, CopyDayPlansUseCase, ClearDayPlansUseCase)
  - Initialize with current week (Monday to Sunday)
  - Load week data in init block collecting Flow and updating StateFlow
  - Load available meals for selection dialog
  - Implement navigateToNextWeek, navigateToPreviousWeek, navigateToToday functions
  - Implement onSlotClick to show meal selection dialog
  - Implement onMealSelected to assign or update meal plan
  - Implement deleteMealPlan function
  - Implement copyDay function with replace option
  - Implement clearDay function
  - Handle all errors and update error state
  - _Requirements: 1.1, 1.4, 2.1, 2.4, 3.1, 4.1, 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 7.1, 10.1, 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 6. Create reusable UI components
  - Implement WeekNavigationBar composable with previous/next/today buttons and week range display
  - Implement DayHeader composable showing date, day name, and completion indicator
  - Implement MealSlotCard composable for individual meal slots with add/view/delete actions
  - _Requirements: 1.3, 5.4, 11.1, 11.2, 11.3, 11.4, 11.5, 14.3_

- [ ] 7. Implement meal planner screen
  - [ ] 7.1 Create MealPlannerScreen stateful composable
    - Inject MealPlannerViewModel with hiltViewModel()
    - Collect uiState as State
    - Pass state and callbacks to MealPlannerContent
    - _Requirements: 1.1, 5.1_
  
  - [ ] 7.2 Create MealPlannerContent stateless composable
    - Implement Scaffold with WeekNavigationBar as topBar
    - Handle loading, error, and success states
    - Render WeekCalendarView for success state
    - Show MealSelectionDialog when showMealSelection is true
    - _Requirements: 1.1, 1.4, 1.5, 2.1, 8.1, 8.2_
  
  - [ ] 7.3 Create WeekCalendarView composable
    - Use LazyColumn for scrollable calendar
    - Render header row with DayHeader for each day
    - Render rows for each MealType (breakfast, lunch, dinner, snack)
    - Create grid layout with MealSlotCard for each date/mealType combination
    - Support long-press on day header for copy/clear actions
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 10.1, 11.1_
  
  - [ ] 7.4 Create MealSlotCard composable
    - Display meal name if assigned, or add icon if empty
    - Handle click to open meal selection or show options
    - Show view details and delete buttons for filled slots
    - Apply appropriate styling for empty vs filled states
    - _Requirements: 1.5, 2.1, 3.1, 4.1, 6.1_
  
  - [ ] 7.5 Create MealSelectionDialog composable
    - Display dialog with meal list from available meals
    - Implement search TextField with real-time filtering
    - Use LazyColumn for meal list with MealSelectionItem
    - Handle empty states (no meals, no search results)
    - Call onMealSelected when meal is tapped
    - Show appropriate title for assign vs replace mode
    - _Requirements: 2.1, 2.2, 12.1, 12.2, 12.3, 12.4, 12.5, 13.2_
  
  - [ ] 7.6 Create MealSelectionItem composable
    - Display meal name and ingredient count
    - Make clickable to select meal
    - _Requirements: 2.2, 9.2_

- [ ] 8. Implement day action dialogs
  - [ ] 8.1 Create CopyDayDialog composable
    - Show date picker for target date selection
    - Display checkbox for "Replace existing meals"
    - Call onCopyDay with selected date and replace flag
    - _Requirements: 7.1, 7.2, 7.3, 7.5_
  
  - [ ] 8.2 Create ClearDayDialog composable
    - Show confirmation dialog with meal count
    - Display source date and meal names to be cleared
    - Call onClearDay when confirmed
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 9. Set up navigation integration
  - Add MealPlanner route to Screen sealed class
  - Add composable destination to NavHost
  - Wire up navigation to meal detail screen from planner
  - Maintain week position when navigating back from detail
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 10. Configure dependency injection
  - Create MealPlanRepositoryModule binding MealPlanRepository to MealPlanRepositoryImpl
  - Update DatabaseModule to provide MealPlanDao from AppDatabase
  - Update DatabaseModule to include migration from version 2 to 3
  - Verify Hilt generates all necessary components
  - _Requirements: 2.3, 8.3_

- [ ] 11. Update bottom navigation
  - Add Planner tab to bottom navigation bar
  - Add navigation icon for meal planner
  - Wire up navigation to MealPlannerScreen
  - _Requirements: 1.1, 5.1_

- [ ] 12. Write unit tests for domain layer
  - [ ] 12.1 Test MealType enum
    - Test displayName() returns correct strings
    - Test all enum values are defined
    - _Requirements: 1.2_
  
  - [ ] 12.2 Test use cases with fake repositories
    - Test GetMealPlansForWeekUseCase combines plans with meals correctly
    - Test GetMealPlansForWeekUseCase handles deleted meals gracefully
    - Test AssignMealToPlanUseCase creates correct MealPlan
    - Test UpdateMealPlanUseCase updates mealId correctly
    - Test CopyDayPlansUseCase copies all plans to target date
    - Test CopyDayPlansUseCase clears target when replaceExisting is true
    - Test ClearDayPlansUseCase calls repository correctly
    - _Requirements: 2.1, 4.3, 7.3, 10.3_

- [ ] 13. Write unit tests for ViewModels
  - [ ] 13.1 Test MealPlannerViewModel
    - Test initial state loads current week
    - Test loads week data successfully and updates state
    - Test handles error and updates error state
    - Test navigateToNextWeek updates currentWeekStart
    - Test navigateToPreviousWeek updates currentWeekStart
    - Test navigateToToday resets to current week
    - Test onSlotClick shows meal selection dialog
    - Test onMealSelected assigns meal for empty slot
    - Test onMealSelected updates meal for filled slot
    - Test deleteMealPlan calls use case
    - Test copyDay calls use case with correct parameters
    - Test clearDay calls use case
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 7.1, 10.1, 14.1_

- [ ] 14. Write instrumented tests for data layer
  - [ ] 14.1 Test MealPlanDao with in-memory database
    - Test insertMealPlan and getMealPlansForDate
    - Test getMealPlansForWeek returns plans in date range
    - Test getMealPlanById returns correct plan
    - Test updateMealPlan modifies existing plan
    - Test deleteMealPlanById removes plan
    - Test deleteMealPlansForDate removes all plans for date
    - Test unique constraint prevents double booking same slot
    - Test foreign key cascade deletes plans when meal is deleted
    - Test insertMealPlans batch insert works correctly
    - _Requirements: 1.2, 2.3, 3.2, 4.3, 5.2, 7.3, 10.3_
  
  - [ ] 14.2 Test MealPlanRepositoryImpl
    - Test getMealPlansForWeek maps entities to domain models
    - Test addMealPlan persists to database
    - Test error handling for database exceptions
    - Test deleteMealPlansForDate removes all plans
    - _Requirements: 2.3, 8.2, 10.3_
