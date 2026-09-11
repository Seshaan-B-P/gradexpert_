# Implementation Plan - Full Project Theme & Font Update

The user wants the entire project to support both Dark and Light themes correctly and to use the standard "mobile font" (sans-serif) instead of the serif/italic style recently added.

## Proposed Changes

### 1. Typography Update
- **[MODIFY] [activity_login.xml](file:///D:/MAD/GradeXpert/app/src/main/res/layout/activity_login.xml)**: Remove all `android:fontFamily="serif"` and `android:textStyle="italic"` attributes. This will revert the text to the system's modern sans-serif "mobile font".

### 2. Full Theme Support (Light/Dark Mode)
- **[MODIFY] [values-night/colors.xml](file:///D:/MAD/GradeXpert/app/src/main/res/values-night/colors.xml)**: Add dark mode overrides for all core colors (`primary`, `background`, `surface`, `text_primary`, etc.).
- **[MODIFY] [values/themes.xml](file:///D:/MAD/GradeXpert/app/src/main/res/values/themes.xml)**: Update the main theme to use semantic color names that work across both modes. Ensure `android:statusBarColor` and `android:windowBackground` reference dynamic colors.

### 3. Layout Color Normalization
- **[MODIFY] Multiple Layout Files**: Scan and replace hardcoded hex colors (e.g., `#E0F2F1`, `#333333`) with appropriate `@color/` references (e.g., `@color/text_secondary`, `@color/primary_light`) to ensure they adapt when the system theme changes.

## Verification Plan

### Manual Verification
- Toggle Dark/Light mode in the Android Studio preview for `activity_login.xml`.
- Run the app on an emulator/device and switch the system theme to verify the entire project UI adapts.
- Verify that the font on the login screen is now the standard sans-serif font.
