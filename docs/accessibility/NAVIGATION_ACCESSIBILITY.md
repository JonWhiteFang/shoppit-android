# Navigation Accessibility Guide

## Overview

This document describes the accessibility features implemented in the Shoppit navigation system, including support for screen readers (TalkBack), keyboard navigation, and focus management.

## Requirements Coverage

This implementation addresses the following requirements from the navigation-and-ui spec:

- **9.1**: TalkBack announces screen transitions with descriptive labels
- **9.2**: Keyboard navigation support through all interactive elements
- **9.3**: Content descriptions for all navigation elements
- **9.4**: Focus moves to appropriate element on new screen
- **9.5**: Focus order follows logical reading patterns

## Features

### 1. Content Descriptions

All navigation elements have descriptive content descriptions for screen readers:

#### Bottom Navigation Items
- **Meals Tab**: "Meals tab" with state "Selected" or "Not selected"
- **Planner Tab**: "Planner tab" with state "Selected" or "Not selected"
- **Shopping Tab**: "Shopping tab" with state "Selected" or "Not selected"

#### Navigation Icons
- **Meals Icon**: "Meals navigation button"
- **Planner Icon**: "Planner navigation button"
- **Shopping Icon**: "Shopping navigation button"

### 2. Screen Transition Announcements

The navigation system announces screen transitions using live regions:

- When navigating to a new screen, TalkBack announces the screen name
- Screen names are human-readable (e.g., "Meal List", "Meal Detail", "Shopping List")
- Announcements use polite mode to avoid interrupting user actions

### 3. Keyboard Navigation

#### Keyboard Shortcuts

The following keyboard shortcuts are available for navigation:

| Shortcut | Action |
|----------|--------|
| Alt+1 | Navigate to Meals section |
| Alt+2 | Navigate to Planner section |
| Alt+3 | Navigate to Shopping section |
| Escape | Navigate back (pop back stack) |
| Tab | Move focus to next element |
| Shift+Tab | Move focus to previous element |

#### Focus Management

- Focus is automatically cleared when navigating to a new screen
- Tab order follows logical reading patterns (left to right, top to bottom)
- Focus indicators are visible for keyboard users
- Interactive elements maintain proper focus order

### 4. State Management

Navigation items communicate their state to assistive technologies:

- **Selected state**: Announced as "Selected" by screen readers
- **Unselected state**: Announced as "Not selected" by screen readers
- State changes are announced when navigation items are activated

## Testing

### Manual Testing with TalkBack

1. **Enable TalkBack**:
   - Go to Settings > Accessibility > TalkBack
   - Turn on TalkBack
   - Use two-finger swipe to navigate

2. **Test Navigation Items**:
   - Swipe through bottom navigation items
   - Verify each item announces its label and state
   - Tap items to navigate and verify state changes are announced

3. **Test Screen Transitions**:
   - Navigate between screens
   - Verify screen names are announced
   - Check that focus moves appropriately

### Manual Testing with Keyboard

1. **Connect a physical keyboard** to your Android device or emulator

2. **Test Keyboard Shortcuts**:
   - Press Alt+1, Alt+2, Alt+3 to navigate between sections
   - Verify navigation occurs correctly
   - Press Escape to navigate back

3. **Test Tab Navigation**:
   - Press Tab to move focus through interactive elements
   - Verify focus order is logical
   - Press Shift+Tab to move focus backward

### Automated Testing

Run the accessibility tests:

```bash
# Run all navigation accessibility tests
./gradlew test --tests "AccessibilityNavigationTest"

# Run keyboard navigation tests
./gradlew test --tests "KeyboardNavigationTest"
```

## Implementation Details

### Components

#### MainScreen.kt
- Applies keyboard navigation shortcuts to the Scaffold
- Sets up keyboard event handling
- Adds semantic properties to navigation items

#### ShoppitNavHost.kt
- Implements live region for screen transition announcements
- Manages focus during screen transitions
- Logs navigation events for debugging

#### KeyboardNavigationHandler.kt
- Handles keyboard shortcuts for bottom navigation
- Manages back navigation via Escape key
- Provides focus management utilities

### Semantic Properties

The implementation uses Compose semantics to provide accessibility information:

```kotlin
Modifier.semantics {
    contentDescription = "Meals tab"
    stateDescription = if (selected) "Selected" else "Not selected"
}
```

### Live Regions

Screen transitions are announced using live regions:

```kotlin
Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
}
```

## Best Practices

### For Developers

1. **Always provide content descriptions** for icons and interactive elements
2. **Use semantic properties** to communicate state changes
3. **Test with TalkBack** during development
4. **Verify keyboard navigation** works for all interactive elements
5. **Maintain logical focus order** in layouts

### For Designers

1. **Design with accessibility in mind** from the start
2. **Ensure sufficient color contrast** for visual indicators
3. **Provide clear visual focus indicators** for keyboard users
4. **Use consistent patterns** across the app
5. **Test designs with accessibility tools**

## Troubleshooting

### TalkBack Not Announcing Screen Transitions

- Verify live region is set on NavHost
- Check that screen names are correctly mapped in `getScreenNameFromRoute()`
- Ensure TalkBack is enabled and configured correctly

### Keyboard Shortcuts Not Working

- Verify keyboard is properly connected
- Check that `SetupKeyboardNavigation()` is called in MainScreen
- Ensure keyboard event listeners are properly attached
- Test with different keyboard layouts

### Focus Order Issues

- Review layout structure for logical ordering
- Check that focusable elements are properly marked
- Verify tab order with keyboard navigation
- Test with different screen sizes

## Future Enhancements

Potential improvements for accessibility:

1. **Custom TalkBack gestures** for common actions
2. **Voice commands** for navigation
3. **Haptic feedback** for navigation events
4. **Customizable keyboard shortcuts**
5. **Accessibility settings** for user preferences
6. **Screen reader hints** for complex interactions
7. **Reduced motion** support for animations

## Resources

- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Compose Accessibility](https://developer.android.com/jetpack/compose/accessibility)
- [TalkBack User Guide](https://support.google.com/accessibility/android/answer/6283677)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

## Support

For accessibility issues or questions:

1. Check this documentation first
2. Review the test files for examples
3. Consult the Android accessibility documentation
4. File an issue with detailed reproduction steps
