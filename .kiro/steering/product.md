---
inclusion: always
---

# Product Requirements

Shoppit is an offline-first Android meal planning app that generates smart shopping lists from weekly meal plans.

## Domain Model

### Core Entities

#### Meal
**Purpose:** Represents a recipe or dish with its ingredients.

**Properties:**
- `id: Long` - Unique identifier (auto-generated)
- `name: String` - Meal name (e.g., "Spaghetti Carbonara")
- `ingredients: List<Ingredient>` - List of required ingredients
- `notes: String?` - Optional cooking notes or instructions
- `tags: List<String>?` - Optional categorization tags
- `createdAt: Long` - Timestamp of creation
- `updatedAt: Long` - Timestamp of last modification

**Constraints:**
- Name must not be blank
- Must have at least one ingredient
- Name length: 1-100 characters

#### Ingredient
**Purpose:** Represents a single ingredient with quantity and unit.

**Properties:**
- `name: String` - Ingredient name (e.g., "Tomato")
- `quantity: String` - Amount needed (e.g., "2", "1.5")
- `unit: String` - Measurement unit (e.g., "cups", "kg", "pcs")

**Constraints:**
- Name must not be blank
- Quantity must be a positive number
- Unit must not be blank

#### MealPlan
**Purpose:** Associates a meal with a specific date and meal type.

**Properties:**
- `id: Long` - Unique identifier
- `mealId: Long` - Reference to Meal
- `date: Long` - Date timestamp (midnight UTC)
- `mealType: MealType` - Type of meal (breakfast/lunch/dinner/snack)
- `servings: Int` - Number of servings planned (default: 1)

**MealType Enum:**
- `BREAKFAST`
- `LUNCH`
- `DINNER`
- `SNACK`

**Constraints:**
- Date must be valid
- Servings must be positive
- One meal per date+mealType combination

#### ShoppingListItem
**Purpose:** Represents an aggregated ingredient for shopping.

**Properties:**
- `id: Long` - Unique identifier
- `ingredientName: String` - Name of ingredient
- `totalQuantity: String` - Aggregated quantity
- `unit: String` - Measurement unit
- `isChecked: Boolean` - Whether item is checked off
- `mealIds: List<Long>` - Source meals for this ingredient

**Constraints:**
- Ingredient name must not be blank
- Total quantity must be positive

### Business Rules

#### Ingredient Aggregation
- Ingredients with the same name (case-insensitive) are combined across all planned meals
- Quantities are summed: "2 cups" + "1 cup" = "3 cups"
- Unit conversion is NOT performed - users manage units manually
- Example: "2 cups flour" + "500g flour" = two separate items

#### Meal Planning
- Users can assign multiple meals to the same date (different meal types)
- Users can assign the same meal to multiple dates
- Deleting a meal from library does NOT delete it from existing plans
- Modifying a meal in library updates all future meal plans

#### Shopping List
- Auto-updates when meal plans change (add/remove/modify)
- Checked items remain checked when list updates
- Items from deleted meal plans are removed from shopping list
- Users can manually add items not tied to meal plans

#### Data Persistence
- All data persists locally in SQLite database via Room
- No network required for core functionality
- Sync is a background operation (future feature)
- App works fully offline

#### Data Integrity
- Deleting a meal from library: Warn if used in meal plans
- Deleting a meal plan: Remove associated shopping list items
- Modifying ingredient quantities: Update shopping list automatically
- Duplicate meal names: Allowed (different recipes with same name)

## User Capabilities

### 1. Meal Library Management ✅
**Status:** Implemented

**Features:**
- Create new meals with name, ingredients, and optional notes
- Edit existing meals (name, ingredients, notes)
- Delete meals from library
- View all meals in a list
- Search meals by name
- View meal details with full ingredient list

**User Actions:**
- Add meal: Tap FAB → Enter details → Save
- Edit meal: Tap meal → Edit icon → Modify → Save
- Delete meal: Swipe meal left → Confirm deletion
- Search: Type in search bar → Results filter in real-time

### 2. Weekly Meal Planning ✅
**Status:** Implemented

**Features:**
- View calendar with current week
- Assign meals to specific dates and meal types
- View planned meals for each day
- Remove meals from plan
- Navigate between weeks
- See meal details from planner

**User Actions:**
- Add to plan: Select date → Select meal type → Choose meal → Confirm
- Remove from plan: Long-press planned meal → Confirm removal
- Navigate weeks: Swipe left/right or use arrow buttons
- View details: Tap planned meal → See full meal info

### 3. Shopping List Generation 📋
**Status:** Planned

**Features:**
- Auto-generated from current meal plans
- Aggregated ingredients with combined quantities
- Check off items while shopping
- Manually add/remove items
- Clear checked items
- Share shopping list

**User Actions:**
- View list: Navigate to Shopping tab
- Check item: Tap checkbox
- Add item: Tap FAB → Enter details → Save
- Remove item: Swipe left → Delete
- Share: Tap share icon → Select app

### 4. Offline Operation ✅
**Status:** Implemented

**Features:**
- Full functionality without network connection
- Local data storage with Room database
- No data loss when offline
- Sync when network available (future)

### 5. Navigation ✅
**Status:** Implemented

**Features:**
- Bottom navigation between main sections
- Three tabs: Meals, Planner, Shopping
- Persistent navigation state
- Deep link support for all screens
- Back navigation support

### 6. Accessibility ✅
**Status:** Implemented

**Features:**
- TalkBack support with content descriptions
- Keyboard navigation with shortcuts
- Minimum touch target sizes (48dp)
- Sufficient color contrast (WCAG AA)
- Screen reader announcements
- Focus management

## UI/UX Principles

### Design System
- **Material3 Design**: Modern, adaptive UI components
- **Dynamic Color**: Supports user's system theme colors
- **Dark Mode**: Full support for light and dark themes
- **Typography**: Material3 type scale for hierarchy
- **Spacing**: Consistent 8dp grid system

### Navigation Patterns
- **Bottom Navigation**: Primary navigation between main sections
  - Meals: Restaurant icon
  - Planner: Calendar icon
  - Shopping: Shopping cart icon
- **FAB (Floating Action Button)**: Primary action for each screen
  - Meals: Add new meal
  - Planner: Add meal to plan
  - Shopping: Add manual item
- **Top App Bar**: Screen title and contextual actions
- **Back Navigation**: Hardware back button and up navigation

### Interaction Patterns
- **Swipe Gestures**: Swipe left to delete items
- **Long Press**: Additional options for items
- **Pull to Refresh**: Update data (when sync available)
- **Tap**: Primary action (view details, select item)
- **Double Tap**: Quick action (future: favorite meal)

### Visual Feedback
- **Loading States**: Progress indicators for async operations
- **Empty States**: Helpful messages and actions when no data
- **Error States**: Clear error messages with retry options
- **Success States**: Snackbar confirmations for actions
- **Sync Status**: Indicator when syncing (future feature)

### Empty State Guidance
- **Empty Meal Library**: "No meals yet. Tap + to add your first meal!"
- **Empty Meal Plan**: "No meals planned. Tap + to start planning!"
- **Empty Shopping List**: "Your shopping list is empty. Add meals to your plan to generate a list."
- **Search No Results**: "No meals found. Try a different search term."

### Accessibility Principles
- **Content Descriptions**: All interactive elements labeled
- **Touch Targets**: Minimum 48dp × 48dp
- **Color Contrast**: WCAG AA compliance (4.5:1 for text)
- **Focus Indicators**: Visible keyboard focus
- **Screen Reader**: Meaningful announcements for state changes
- **Text Scaling**: Support up to 200% text size
- **Keyboard Navigation**: Full keyboard support with shortcuts

## Platform Constraints

### Android Version Support
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 7.0 (API 24)
- **Reason**: Covers 95%+ of active Android devices
- **Implications**: 
  - No support for Android 6.0 and below
  - Can use modern Android APIs
  - Must handle API level differences gracefully

### Device Orientation
- **Supported**: Portrait only
- **Reason**: Typical for meal planning and shopping apps
- **Implications**:
  - Simpler layout management
  - Better use of vertical space for lists
  - Consistent user experience

### Permissions
- **Required**: None for core functionality
- **Optional** (future):
  - `INTERNET`: For sync functionality
  - `POST_NOTIFICATIONS`: For meal reminders
  - `CAMERA`: For meal photos
- **Privacy**: No sensitive permissions required

### Storage
- **Type**: Local SQLite database via Room
- **Location**: App-private storage
- **Size**: Minimal (< 10MB for typical usage)
- **Backup**: Android Auto Backup eligible

### Performance Targets
- **App Launch**: < 2 seconds cold start
- **Screen Navigation**: < 100ms transition
- **Database Queries**: < 50ms for typical operations
- **List Scrolling**: 60 FPS smooth scrolling
- **Memory**: < 100MB RAM usage

### Accessibility Compliance
- **Standard**: WCAG 2.1 Level AA
- **Requirements**:
  - Color contrast: 4.5:1 for text, 3:1 for UI components
  - Touch targets: Minimum 48dp × 48dp
  - Screen reader: Full TalkBack support
  - Keyboard: Complete keyboard navigation
  - Text scaling: Support up to 200%

### Build Configuration
- **Language**: Kotlin 2.1.0
- **Build System**: Gradle 8.9 with Kotlin DSL
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java Version**: 17

### Dependencies
- **UI**: Jetpack Compose (BOM 2023.10.01)
- **Architecture**: Hilt, Room, Navigation Compose
- **Async**: Kotlin Coroutines, Flow
- **Testing**: JUnit, MockK, Compose UI Testing
