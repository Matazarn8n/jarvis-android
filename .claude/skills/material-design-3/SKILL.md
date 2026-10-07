---
name: material-design-3
description: Use when designing, building, or reviewing the UI of an Android or mobile app — choosing components, spacing, color, typography, or motion — to follow Google's Material Design 3 (M3), a free, open design system with a matching Jetpack Compose implementation (androidx.compose.material3). Complements the android-profiler, r8-analyzer, and edge-to-edge skills, which cover performance and system-bar behavior rather than visual design.
---

# Material Design 3

## Overview

Material Design 3 (M3, "Material You") is Google's current design system: a token-based
color/typography/shape system plus a component library. It is free and open — no license, no
paywall. On Android, `androidx.compose.material3` is the reference implementation; on the web,
`@material/web` or any token-driven CSS system can follow the same specs.

Apply this skill when: creating a new screen or component, migrating an app's look away from
generic/default styling, reviewing a UI for consistency, or picking values (spacing, color,
type scale, motion duration) that should follow M3 rather than be invented ad hoc.

## Core principle: tokens, not hardcoded values

M3 is built on **design tokens** — named roles (`primary`, `onPrimary`, `surfaceVariant`,
`titleLarge`, `bodyMedium`...) rather than raw hex/px values. Never hardcode a color hex or a
font size when a token exists for the role being expressed — that is what breaks dark mode,
dynamic color, and future re-theming. Look up the right file below by task.

## Reference files

| Need | File |
|---|---|
| Color roles, dynamic color, dark theme, contrast rules | `references/couleur.md` |
| Type scale (display/headline/title/body/label × large/medium/small) | `references/typographie.md` |
| Spacing, grids, breakpoints, adaptive layout panes | `references/layout.md` |
| Motion durations, easing curves, when to animate | `references/motion.md` |
| Buttons, cards, chips, text fields, dialogs, snackbars | `references/composants.md` |
| Navigation bar/rail/drawer, top/bottom app bars | `references/navigation.md` |
| Touch target size, contrast minimums, focus order | `references/accessibilite.md` |

## Quick defaults (when in doubt)

- Spacing scale: 4dp base unit — 4, 8, 12, 16, 24, 32, 48, 64dp. Screen edge padding: 16dp
  (phone), 24dp (tablet/foldable unfolded).
- Corner radius: small components (chips, buttons) 8–20dp (`shapes.small`/`medium`); cards
  12–16dp; large sheets/dialogs 28dp.
- Minimum touch target: 48×48dp, even if the visible element is smaller (use padding, not a
  bigger visible shape, to hit this).
- Text contrast: 4.5:1 minimum for body text, 3:1 for large text (≥18sp regular or ≥14sp bold),
  against its background token — check `references/accessibilite.md` before shipping a custom
  color pairing.
- Elevation is expressed via **tonal surface color**, not just drop shadows, in M3 (surfaces get
  progressively lighter/darker tints at higher elevation levels, not just a shadow) — do not
  reintroduce Material 2-style pure-shadow elevation.

## Jetpack Compose setup

```kotlin
implementation("androidx.compose.material3:material3:<latest-stable>")
```

Wrap the app root in `MaterialTheme` (M3's `androidx.compose.material3.MaterialTheme`, not the
M2 one from `androidx.compose.material`) with a `ColorScheme` built via
`dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` on Android 12+ (API 31+)
for Material You dynamic color from the user's wallpaper, falling back to a hand-picked
`lightColorScheme()`/`darkColorScheme()` on older API levels — never assume dynamic color is
available without an SDK check.
