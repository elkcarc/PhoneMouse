# Fix Variation Deletion and Settings Menu Insets

This plan addresses two issues: the inability to delete automation variations and the settings menu being partially obscured by the system navigation bar.

## Proposed Changes

### 1. Automation Variation Deletion
Implement the missing deletion logic in the ViewModel and wire it to the UI.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/elkcarc/AndroidStudioProjects/PhoneMouse/app/src/main/java/com/example/phonemouse/MainViewModel.kt)
*   Add `deleteConfig(index: Int)` function.
*   Update `configs` StateFlow and persist changes to `SharedPreferences`.
*   Adjust `selectedConfigIndex` if the deleted item was at or before the current selection.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/elkcarc/AndroidStudioProjects/PhoneMouse/app/src/main/java/com/example/phonemouse/MainActivity.kt)
*   Wire the `onConfigDeleted` callback in `ConfigsAdapter` to `viewModel.deleteConfig(index)`.
*   Observe `viewModel.selectedConfigIndex` and update the adapter to ensure the visual selection highlight stays in sync.

---

### 2. UI Layout & Insets
Fix the overlap between the side drawer content and the system navigation bar.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/elkcarc/AndroidStudioProjects/PhoneMouse/app/src/main/java/com/example/phonemouse/MainActivity.kt)
*   Update `setupWindowInsets` to correctly apply bottom insets to both the Main and Settings drawer panels.
*   Ensure that manual padding updates *add* to the design-specified padding (32dp) rather than replacing it, preventing content from touching the screen edges.

#### [MODIFY] [ConfigsAdapter.kt](file:///C:/Users/elkcarc/AndroidStudioProjects/PhoneMouse/app/src/main/java/com/example/phonemouse/ConfigsAdapter.kt)
*   Add a `updateSelection(newIndex: Int)` method or update the property to allow the Activity to refresh the selection visual without a full list replacement.

## Verification Plan

### Automated Tests
*   Unit test `MainViewModel#deleteConfig` to ensure list size decreases and `selectedConfigIndex` remains valid.

### Manual Verification
*   Verify clicking the 'X' button on a variation removes it from the list and the drawer.
*   Confirm the Settings menu has adequate clearance from the bottom navigation bar (gesture or button-based).
*   Ensure selecting a variation highlights the correct item, and this highlight persists after reordering or deleting items.
