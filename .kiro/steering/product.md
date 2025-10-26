---
inclusion: always
---

# Product Requirements

Shoppit is an offline-first Android meal planning app that generates smart shopping lists from weekly meal plans.

## Domain Model

### Core Entities
- **Meal**: Name, ingredients list, optional notes/tags
- **Ingredient**: Name, quantity, unit (e.g., "2 cups flour")
- **MealPlan**: Date, meal type (breakfast/lunch/dinner/snack), associated meal
- **ShoppingListItem**: Ingredient name, aggregated quantity, checked status

### Business Rules
- Ingredients with same name aggregate quantities across planned meals
- Unit conversion not required (users manage units manually)
- Shopping list auto-updates when meal plans change
- All data persists locally; sync is background operation

## User Capabilities

1. **Meal Library**: Create, edit, delete meals with ingredient lists
2. **Weekly Planner**: Assign meals to specific dates and meal types
3. **Shopping List**: View aggregated ingredients, check off items while shopping
4. **Offline Operation**: Full functionality without network; sync when available

## UI/UX Principles

- Material3 design system
- Bottom navigation for main sections (Meals, Planner, Shopping)
- Floating action buttons for primary actions (Add Meal, Add to Plan)
- Swipe gestures for delete/edit actions
- Clear visual feedback for sync status
- Empty states guide users to first actions

## Platform Constraints

- **Target**: Android 7.0+ (API 24-34)
- **Orientation**: Portrait only (typical for shopping/planning apps)
- **Permissions**: None required for core functionality
- **Storage**: Local SQLite database via Room
