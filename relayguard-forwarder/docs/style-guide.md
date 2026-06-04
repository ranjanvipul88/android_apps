# Style Guide

## Visual Direction

RelayGuard uses a calm operational UI for repeated configuration and troubleshooting. It avoids copied layouts and iconography from the analyzed APK.

## Colors

- Primary: `#0B6B61`
- Secondary: `#5B5F97`
- Tertiary: `#B44B37`
- Background: `#FAFBF8`
- Surface: `#FFFFFF`
- Error: `#B3261E`

The palette intentionally mixes teal, indigo, and clay accents instead of a one-note hue.

## Typography

- Use Material 3 default typography.
- Screen titles: `titleLarge` or `headlineSmall`.
- Cards and panels: `titleMedium` and `bodyMedium`.
- Technical notes: `bodySmall`.

## Spacing

- Screen edge padding: 16dp on phones, 24dp on tablets.
- Card internal padding: 16dp.
- List spacing: 12dp.
- Minimum touch target: 48dp.

## Components

- Bottom navigation for Rules, History, Privacy, and Settings.
- Switches for rule enablement.
- Floating action button for creating rules.
- Assist chips for event categories and status.
- Cards only for repeated records and concise panels.

## Accessibility

- All actionable icons need content descriptions.
- Contrast should meet WCAG AA.
- Dynamic type should not clip controls.
- Navigation must work with keyboard and screen reader focus order.
