# 03_UI_Spec.md — Meemo Design System & Visual Specification
**Project Name:** Meemo — Android Digital Assistant  
**Package Namespace:** `com.marufallammahim.meemo`  
**Document Version:** 1.0.0  

---

## 1. Visual Direction & Brand Identity
Meemo embodies a **warm, trustworthy, high-craft digital assistant aesthetic** anchored in **Deep Burgundy (`#5A0719`)**, rose-gold accents (`#D96B86`), and neutral obsidian/alabaster surfaces.
- **Original Identity:** Features the original geometric vector **Meemo Cat Mascot** rendered natively through Jetpack Compose `Canvas` and Android Vector Drawables (`res/drawable/ic_meemo_mascot.xml` + `assets/mascot/*.svg`). Zero external PNGs or copyrighted assistant logos are used.
- **Clarity Over Clutter:** Status badges clearly distinguish between **Verified Hardware Execution**, **Settings Opened (User Action Needed)**, **Permission Required**, and **Hardware Unavailable**.

---

## 2. Color Tokens & Contrast Validation

All color tokens are defined in `presentation/theme/Color.kt` and mapped cleanly to Material 3 `ColorScheme` plus custom `MeemoExtendedColors`.

| Token Name | Hex Value | Usage Role | WCAG Contrast Check |
| :--- | :--- | :--- | :--- |
| `brand.primary` | `#5A0719` | Primary burgundy brand surface, active FAB in light theme, headers | `11.8:1` vs `#F5F5F7` (AAA) |
| `brand.primaryVariant` | `#7A1730` | Elevated primary containers, listening ring border, gradient stop | `8.4:1` vs `#F5F5F7` (AAA) |
| `brand.accent` | `#D96B86` | Dark-theme primary interactive accent, mascot ears/aura, active tabs | `5.6:1` vs `#101114` (AA) |
| `brand.accentSoft` | `#F8D7E0` | Subtle highlight chips and light-theme secondary containers | `10.2:1` vs `#5A0719` (AAA) |
| `status.success` | `#2E7D5B` | Verified execution badge, available capability indicator | `4.9:1` vs `#FFFFFF` (AA) |
| `status.warning` | `#B7791F` | Partial success, settings redirect notice, confirmation prompt | `4.6:1` vs `#FFFFFF` (AA) |
| `status.error` | `#B42332` | Failed execution, permission denied, root rejected | `5.8:1` vs `#FFFFFF` (AA) |
| `status.info` | `#3569A8` | Transient notification read-aloud banner, capability info | `5.4:1` vs `#FFFFFF` (AA) |
| `dark.background` | `#101114` | Root screen background in Dark theme | — |
| `dark.surface` | `#191B20` | Standard cards, bottom bar, input container in Dark theme | — |
| `dark.surfaceElevated` | `#242730` | Dialogs, assistant response bubbles, status pills in Dark theme | — |
| `dark.textPrimary` | `#F5F5F7` | Primary headline and body text in Dark theme | `16.4:1` vs `#101114` (AAA) |
| `dark.textSecondary` | `#B6BAC4` | Subtitles, timestamps, capability metadata in Dark theme | `9.2:1` vs `#101114` (AAA) |
| `light.background` | `#F7F7FA` | Root screen background in Light theme | — |
| `light.surface` | `#FFFFFF` | Cards, dialogs, input surfaces in Light theme | — |
| `light.surfaceElevated` | `#EFEFF5` | Secondary cards and suggestion chips in Light theme | — |
| `light.textPrimary` | `#20212A` | Primary headline and body text in Light theme | `15.1:1` vs `#FFFFFF` (AAA) |
| `light.textSecondary` | `#626675` | Subtitles, metadata, and helper labels in Light theme | `5.5:1` vs `#FFFFFF` (AA) |

---

## 3. Typography Scale (English & Bengali Support)

Defined in `presentation/theme/Type.kt`. Uses system `FontFamily.SansSerif` (which maps automatically to Roboto + **Noto Sans Bengali** on Android 8.0+ without requiring runtime network font downloads) and `FontFamily.Monospace` for diagnostic codes and approved command inspection.

| Style | Size (`sp`) | Line Height (`sp`) | Weight | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | `32.sp` | `40.sp` | `Bold (700)` | Onboarding hero titles |
| `headlineMedium` | `24.sp` | `32.sp` | `SemiBold (600)` | Screen titles (`Meemo`, `Capabilities`) |
| `titleLarge` | `20.sp` | `28.sp` | `SemiBold (600)` | Section headers & modal dialog titles |
| `titleMedium` | `16.sp` | `24.sp` | `Medium (500)` | Card titles, app names, capability titles |
| `bodyLarge` | `16.sp` | `24.sp` | `Normal (400)` | Assistant responses, user chat bubbles (comfortable for Bengali Matra/conjuncts) |
| `bodyMedium` | `14.sp` | `20.sp` | `Normal (400)` | Settings descriptions, error recovery explanations |
| `labelLarge` | `14.sp` | `20.sp` | `SemiBold (600)` | Button labels, filter chips, action links |
| `labelSmall` | `11.sp` | `16.sp` | `Medium (500)` | Error codes (`ERR_TORCH_UNAVAILABLE`), latency badges, timestamps |

---

## 4. Spacing, Layout & Responsive Grid (`Dimens.kt`)

- **Spacing Scale:**
  - `spaceXXS = 4.dp`
  - `spaceXS = 8.dp`
  - `spaceSM = 12.dp`
  - `spaceMD = 16.dp` (Standard screen horizontal margin and card padding)
  - `spaceLG = 24.dp` (Section spacing)
  - `spaceXL = 32.dp`
  - `spaceXXL = 40.dp`
- **Touch Targets:** Every interactive button, chip, icon button, and switch enforces a minimum `48.dp x 48.dp` touch target (`Modifier.minimumInteractiveComponentSize()`).
- **Corner Radii:**
  - Small chips/badges: `8.dp`
  - Cards & text fields: `16.dp`
  - Chat bubbles & modals: `20.dp`
  - Voice FAB: `28.dp` (full circle / squircle)
- **Responsive Layout & Window Insets:**
  - Respects `WindowInsets.systemBars` and `WindowInsets.ime` so the bottom command bar glides smoothly above the software keyboard.
  - Content width is constrained to `640.dp` max on tablets/foldables (`Modifier.widthIn(max = 640.dp)`) so cards never stretch awkwardly on wide screens.

---

## 5. Original Meemo Cat Mascot Specification (`MeemoMascot.kt`)

The Meemo mascot is an original minimalist cyber-cat emblem crafted natively with Compose `Canvas` and bundled SVG/Vector Drawable assets (`assets/mascot/meemo_mascot_*.svg` and `res/drawable/ic_meemo_mascot.xml`).

| Mascot State | Visual Expression & Geometry | Animation Behavior (When Reduced Motion is Off) |
| :--- | :--- | :--- |
| `Idle` | Rounded burgundy cat head, rose-gold inner ears, calm curved eyes, subtle whisker lines. | Soft vertical breathing float (`±3.dp` over `2200ms`). |
| `Listening` | Ears perked outward, bright circular eyes, concentric pulsing rose-gold soundwave rings. | Concentric ripple expansion (`1.0x` to `1.35x` over `1000ms`). |
| `Thinking` | One ear tilted, eyes looking upward toward three orbiting thought dots. | Orbiting phase rotation on the three indicator dots. |
| `Speaking` | Happy arc eyes, animated mouth waveform bars synchronized with active TTS state. | Smooth bar height oscillation (`600ms` cycle). |
| `Success` | Cheerful wink/chevron eyes (`^ ^`) with a emerald verification badge (`#2E7D5B`). | Gentle scale bounce on state entry. |
| `Error` | Concerned angled brows, amber/crimson indicator badge (`#B42332`). | Single subtle horizontal nudge, then steady state. |

**Accessibility Rule:** `MeemoMascot` exposes `Modifier.semantics { contentDescription = ... }` describing the current assistant state and checks `LocalContext` animator scale so animations freeze into crisp static frames when reduced motion is enabled.
