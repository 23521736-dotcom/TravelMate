# Implementation Plan - Voice-to-Text & UI Enhancements

This plan outlines the implementation of voice-to-text and several UI improvements requested by the user.

## User Review Required

> [!IMPORTANT]
> - Voice-to-text requires `RECORD_AUDIO` permission.
> - Search bar click on Home will now switch the active tab to Explore.
> - Destination ratings will be dynamically calculated from the actual reviews stored in Firebase.

## Proposed Changes

### 1. Voice-to-Text Integration (Ongoing)
- Already implemented `VoiceToTextParser.kt`.
- Integrating into `ExploreScreen.kt` and `ChatFloatingWindow.kt`.

### 2. Destination Detail Enhancements
#### [DestinationDetailViewModel.kt](file:///C:/Users/PC/AndroidStudioProjects/TravelMate/app/src/main/java/com/example/travelmate/ui/viewmodel/DestinationDetailViewModel.kt)
- Update `uiState` to calculate and show aggregate rating from the `reviews` list fetched from Firebase.
- This ensures the UI reflects actual user feedback rather than static data.

### 3. Home to Search Flow
#### [HomeScreen.kt](file:///C:/Users/PC/AndroidStudioProjects/TravelMate/app/src/main/java/com/example/travelmate/ui/screen/HomeScreen.kt)
- Add `onSearchClick` parameter.
- Wrap the Search Bar in a `Box` with a `clickable` modifier to trigger navigation.

#### [MainContainer.kt](file:///C:/Users/PC/AndroidStudioProjects/TravelMate/app/src/main/java/com/example/travelmate/ui/screen/MainContainer.kt)
- Pass `onSearchClick = { selectedScreen = NavItem.Explore.route }` to `HomeScreen`.

### 4. Expense Screen Improvements
#### [ExpenseScreen.kt](file:///C:/Users/PC/AndroidStudioProjects/TravelMate/app/src/main/java/com/example/travelmate/ui/screen/ExpenseScreen.kt)
- Redesign `ExpenseChartCard` to include a clearer legend with percentages.
- Improve the `DonutChart` visualization.
- Add a "Category Breakdown" section with detailed spending per category.

## Verification Plan

### Manual Verification
1. **Place Detail**:
   - Open a destination.
   - Add a new review.
   - Verify that the average rating at the top and the review count update correctly.
2. **Search Flow**:
   - Go to Home screen.
   - Click on the search bar.
   - Verify that the app switches to the "Explore" tab.
3. **Expense Screen**:
   - Add several expenses in different categories.
   - Verify that the Donut Chart and category list display correct percentages and amounts.
4. **Voice-to-Text**:
   - Verify in both Explore and Chat screens as planned previously.
