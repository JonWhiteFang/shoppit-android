# Requirements Document

## Introduction

The Shopping List Generation feature automatically creates and maintains a shopping list by aggregating ingredients from all meals in the user's weekly meal plan. This feature eliminates manual shopping list creation by intelligently combining ingredients with the same name, providing a consolidated view of what needs to be purchased. The shopping list updates automatically as meal plans change, ensuring the list always reflects the current week's needs.

## Glossary

- **Shopping_List_System**: The subsystem responsible for generating, aggregating, and managing shopping list items
- **Shopping_List_Item**: A record representing an aggregated ingredient with name, quantity, unit, and checked status
- **Ingredient_Aggregation**: The process of combining ingredients with identical names from multiple meals
- **Meal_Plan_Repository**: The data source providing meal plan assignments for the current week
- **Meal_Repository**: The data source providing meal details including ingredient lists
- **Room_Database**: Local SQLite database for offline shopping list storage
- **Auto_Sync**: The automatic update mechanism that regenerates the shopping list when meal plans change
- **Checked_Status**: A boolean flag indicating whether a shopping list item has been marked as purchased

## Requirements

### Requirement 1

**User Story:** As a user, I want the shopping list to automatically generate from my weekly meal plan, so that I don't have to manually create a shopping list.

#### Acceptance Criteria

1. WHEN the user navigates to the shopping list screen, THE Shopping_List_System SHALL retrieve all meal plans for the current week from Meal_Plan_Repository
2. THE Shopping_List_System SHALL retrieve meal details for each planned meal from Meal_Repository
3. THE Shopping_List_System SHALL extract all ingredients from the retrieved meals
4. THE Shopping_List_System SHALL perform Ingredient_Aggregation by combining ingredients with identical names
5. THE Shopping_List_System SHALL display the aggregated ingredients as Shopping_List_Item records in the shopping list view

### Requirement 2

**User Story:** As a user, I want ingredients with the same name to be combined into a single shopping list item, so that I can see the total quantity needed.

#### Acceptance Criteria

1. WHEN multiple meals contain ingredients with identical names, THE Shopping_List_System SHALL combine them into a single Shopping_List_Item
2. THE Shopping_List_System SHALL perform case-insensitive name matching during Ingredient_Aggregation
3. THE Shopping_List_System SHALL concatenate quantity values from all matching ingredients into a single quantity string
4. THE Shopping_List_System SHALL preserve the unit from the first occurrence of each ingredient
5. THE Shopping_List_System SHALL display the aggregated quantity in the format "quantity1 + quantity2 + quantity3 unit"

### Requirement 3

**User Story:** As a user, I want to check off items as I shop, so that I can track what I've already purchased.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL display a checkbox next to each Shopping_List_Item
2. WHEN the user taps a checkbox, THE Shopping_List_System SHALL toggle the Checked_Status for that Shopping_List_Item
3. THE Shopping_List_System SHALL persist the Checked_Status to Room_Database within 500 milliseconds
4. WHEN a Shopping_List_Item has Checked_Status true, THE Shopping_List_System SHALL apply visual styling to indicate completion
5. THE Shopping_List_System SHALL maintain Checked_Status across app restarts and device reboots

### Requirement 4

**User Story:** As a user, I want the shopping list to automatically update when I change my meal plan, so that my list always reflects my current plans.

#### Acceptance Criteria

1. WHEN a meal plan is added to the current week, THE Shopping_List_System SHALL automatically regenerate the shopping list
2. WHEN a meal plan is removed from the current week, THE Shopping_List_System SHALL automatically regenerate the shopping list
3. WHEN a meal plan is updated in the current week, THE Shopping_List_System SHALL automatically regenerate the shopping list
4. THE Shopping_List_System SHALL preserve Checked_Status for Shopping_List_Item records that still exist after regeneration
5. THE Shopping_List_System SHALL complete Auto_Sync within 1 second of meal plan changes

### Requirement 5

**User Story:** As a user, I want to manually add items to my shopping list, so that I can include non-meal items I need to purchase.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide an add item action in the shopping list view
2. WHEN the user taps the add item action, THE Shopping_List_System SHALL display an input dialog for item name, quantity, and unit
3. WHEN the user confirms the input, THE Shopping_List_System SHALL create a Shopping_List_Item with isManual flag set to true
4. THE Shopping_List_System SHALL persist manual Shopping_List_Item records to Room_Database
5. THE Shopping_List_System SHALL display manual items separately from auto-generated items in the shopping list view

### Requirement 6

**User Story:** As a user, I want to edit manually added items, so that I can correct mistakes or update quantities.

#### Acceptance Criteria

1. WHEN the user long-presses a manual Shopping_List_Item, THE Shopping_List_System SHALL display edit and delete options
2. WHEN the user selects edit, THE Shopping_List_System SHALL display an input dialog pre-filled with current values
3. WHEN the user confirms changes, THE Shopping_List_System SHALL update the Shopping_List_Item in Room_Database
4. THE Shopping_List_System SHALL prevent editing of auto-generated Shopping_List_Item records
5. THE Shopping_List_System SHALL display a visual indicator distinguishing manual items from auto-generated items

### Requirement 7

**User Story:** As a user, I want to delete manually added items, so that I can remove items I no longer need.

#### Acceptance Criteria

1. WHEN the user long-presses a manual Shopping_List_Item, THE Shopping_List_System SHALL display a delete option
2. WHEN the user confirms deletion, THE Shopping_List_System SHALL remove the Shopping_List_Item from Room_Database
3. WHEN deletion succeeds, THE Shopping_List_System SHALL update the shopping list view to remove the item
4. THE Shopping_List_System SHALL prevent deletion of auto-generated Shopping_List_Item records
5. THE Shopping_List_System SHALL complete deletion within 500 milliseconds under normal conditions

### Requirement 8

**User Story:** As a user, I want to clear all checked items from my list, so that I can quickly clean up after shopping.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide a clear checked items action in the shopping list view
2. WHEN the user taps the clear checked action, THE Shopping_List_System SHALL display a confirmation dialog showing the count of items to be removed
3. WHEN the user confirms, THE Shopping_List_System SHALL delete all Shopping_List_Item records with Checked_Status true
4. THE Shopping_List_System SHALL preserve unchecked Shopping_List_Item records
5. THE Shopping_List_System SHALL update the shopping list view to show only unchecked items after clearing

### Requirement 9

**User Story:** As a user, I want to see which meals each ingredient is used in, so that I can understand why I need certain items.

#### Acceptance Criteria

1. WHEN the user taps a Shopping_List_Item, THE Shopping_List_System SHALL display a detail view showing the item information
2. THE Shopping_List_System SHALL display a list of meal names that contain the ingredient
3. THE Shopping_List_System SHALL display the quantity needed from each meal separately
4. THE Shopping_List_System SHALL provide navigation to view full meal details from the ingredient detail view
5. THE Shopping_List_System SHALL display "Manually added" for Shopping_List_Item records with isManual flag true

### Requirement 10

**User Story:** As a user, I want to organize my shopping list by categories, so that I can shop more efficiently.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL group Shopping_List_Item records by predefined categories (produce, dairy, meat, pantry, other)
2. THE Shopping_List_System SHALL automatically assign categories based on ingredient name matching against a category dictionary
3. THE Shopping_List_System SHALL display category headers in the shopping list view
4. THE Shopping_List_System SHALL allow the user to collapse and expand category sections
5. THE Shopping_List_System SHALL display the count of unchecked items in each category header

### Requirement 11

**User Story:** As a user, I want to see the total number of items and checked items, so that I can track my shopping progress.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL display a summary header showing total item count and checked item count
2. THE Shopping_List_System SHALL update the summary in real-time as items are checked or unchecked
3. THE Shopping_List_System SHALL display a progress indicator showing the percentage of items checked
4. THE Shopping_List_System SHALL calculate counts separately for auto-generated and manual items
5. THE Shopping_List_System SHALL display "All items checked!" message when all items have Checked_Status true

### Requirement 12

**User Story:** As a user, I want the shopping list to work offline, so that I can use it in stores without internet connection.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL perform all shopping list operations using Room_Database without requiring network connectivity
2. WHEN the device is offline, THE Shopping_List_System SHALL provide full shopping list functionality
3. THE Shopping_List_System SHALL persist all Shopping_List_Item records locally in Room_Database
4. THE Shopping_List_System SHALL load shopping list data from Room_Database on screen navigation within 1 second
5. THE Shopping_List_System SHALL maintain data consistency across app restarts and device reboots

### Requirement 13

**User Story:** As a user, I want to share my shopping list with others, so that someone else can shop for me.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide a share action in the shopping list view
2. WHEN the user taps the share action, THE Shopping_List_System SHALL generate a plain text representation of the shopping list
3. THE Shopping_List_System SHALL format the text with category headers and item details
4. THE Shopping_List_System SHALL invoke the system share dialog with the formatted text
5. THE Shopping_List_System SHALL include only unchecked items in the shared text by default

### Requirement 14

**User Story:** As a user, I want to filter the shopping list to show only unchecked items, so that I can focus on what I still need to buy.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide a filter toggle in the shopping list view
2. WHEN the filter is enabled, THE Shopping_List_System SHALL display only Shopping_List_Item records with Checked_Status false
3. WHEN the filter is disabled, THE Shopping_List_System SHALL display all Shopping_List_Item records
4. THE Shopping_List_System SHALL persist the filter state across screen navigations
5. THE Shopping_List_System SHALL update the item count display to reflect the filtered view

### Requirement 15

**User Story:** As a user, I want to search for specific items in my shopping list, so that I can quickly find what I need.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide a search input field in the shopping list view
2. WHEN the user enters text in the search field, THE Shopping_List_System SHALL filter Shopping_List_Item records to show only items with names containing the search text
3. THE Shopping_List_System SHALL perform case-insensitive search matching
4. THE Shopping_List_System SHALL update the filtered list in real-time as the user types
5. WHEN the search field is empty, THE Shopping_List_System SHALL display all Shopping_List_Item records according to the current filter state

### Requirement 16

**User Story:** As a user, I want clear error messages when shopping list operations fail, so that I understand what went wrong.

#### Acceptance Criteria

1. WHEN a database operation fails, THE Shopping_List_System SHALL display a user-friendly error message without technical details
2. WHEN no meal plans exist for the current week, THE Shopping_List_System SHALL display a message prompting the user to create meal plans first
3. THE Shopping_List_System SHALL log detailed error information for debugging purposes
4. WHEN an unexpected error occurs, THE Shopping_List_System SHALL display a generic error message and allow the user to retry
5. THE Shopping_List_System SHALL clear error messages when the user successfully completes an operation or navigates away

### Requirement 17

**User Story:** As a user, I want to uncheck all items at once, so that I can reuse my shopping list for the next week.

#### Acceptance Criteria

1. THE Shopping_List_System SHALL provide an uncheck all action in the shopping list view
2. WHEN the user taps the uncheck all action, THE Shopping_List_System SHALL display a confirmation dialog
3. WHEN the user confirms, THE Shopping_List_System SHALL set Checked_Status to false for all Shopping_List_Item records
4. THE Shopping_List_System SHALL update the shopping list view to show all items as unchecked
5. THE Shopping_List_System SHALL complete the operation within 1 second for lists up to 100 items

### Requirement 18

**User Story:** As a user, I want to see visual indicators for items that appear in multiple meals, so that I know which ingredients are used frequently.

#### Acceptance Criteria

1. WHEN a Shopping_List_Item is derived from ingredients in multiple meals, THE Shopping_List_System SHALL display a badge showing the meal count
2. THE Shopping_List_System SHALL use distinct visual styling for items used in 3 or more meals
3. WHEN the user taps the badge, THE Shopping_List_System SHALL navigate to the ingredient detail view showing all associated meals
4. THE Shopping_List_System SHALL calculate meal count based on unique meal IDs contributing to the aggregated ingredient
5. THE Shopping_List_System SHALL not display the badge for manual Shopping_List_Item records
