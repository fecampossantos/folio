---
name: Folio Sanctuary
colors:
  surface: '#121412'
  surface-dim: '#121412'
  surface-bright: '#383a37'
  surface-container-lowest: '#0d0f0d'
  surface-container-low: '#1a1c1a'
  surface-container: '#1e201e'
  surface-container-high: '#292a28'
  surface-container-highest: '#333533'
  on-surface: '#e2e3df'
  on-surface-variant: '#c3c8bd'
  inverse-surface: '#e2e3df'
  inverse-on-surface: '#2f312e'
  outline: '#8d9289'
  outline-variant: '#434840'
  surface-tint: '#b0cfa8'
  primary: '#c3e2ba'
  on-primary: '#1d361b'
  primary-container: '#a8c6a0'
  on-primary-container: '#395335'
  inverse-primary: '#4a6545'
  secondary: '#c0c9c1'
  on-secondary: '#2a322d'
  secondary-container: '#404943'
  on-secondary-container: '#aeb7b0'
  tertiary: '#ffccdd'
  on-tertiary: '#472633'
  tertiary-container: '#e1b1c1'
  on-tertiary-container: '#674250'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ccebc3'
  primary-fixed-dim: '#b0cfa8'
  on-primary-fixed: '#082108'
  on-primary-fixed-variant: '#334d2f'
  secondary-fixed: '#dce5dc'
  secondary-fixed-dim: '#c0c9c1'
  on-secondary-fixed: '#151d18'
  on-secondary-fixed-variant: '#404943'
  tertiary-fixed: '#ffd9e4'
  tertiary-fixed-dim: '#ebb9ca'
  on-tertiary-fixed: '#2f121e'
  on-tertiary-fixed-variant: '#603c4a'
  background: '#121412'
  on-background: '#e2e3df'
  surface-variant: '#333533'
typography:
  display-lg:
    fontFamily: Source Serif 4
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Source Serif 4
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-reading:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  caption:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-padding: 24px
  unit: 8px
  gutter: 24px
  margin-mobile: 20px
  margin-desktop: 64px
---

## Brand & Style

Folio Sanctuary is a digital reading environment designed to evoke the serenity of a private library. The brand personality is **scholarly, calm, and premium**, targeting readers who value deep focus and a tactile-digital hybrid experience. 

The design style is **Modern Minimalist with a Tactile twist**. It utilizes a sophisticated "Fidelity" color variant where hues are derived directly from the primary sage green, creating a monochromatic harmony that reduces cognitive load. The interface avoids aggressive digital tropes, opting instead for soft tonal layering, subtle backdrop blurs (Glassmorphism), and traditional typographic hierarchy that mimics high-end editorial publishing.

## Colors

The palette is rooted in a **Deep Sage and Charcoal** scheme. The dark mode utilizes a "Surface" color that isn't true black, but a very dark olive-tinted charcoal (#121412) to reduce eye strain.

- **Primary:** A soft, desaturated sage green used for progress indicators, interactive accents, and brand identification.
- **Surface Tiers:** Depth is communicated through subtle increments in lightness rather than shadows. `surface-container-low` is used for the base navigation background, while `high` and `highest` are used for elevated cards and dropdown menus.
- **Accents:** Tertiary pinks are reserved for rare, high-contrast alerts or specific "love" actions (e.g., favorites), though they are absent from the main library view to maintain the "sanctuary" feel.

## Typography

The typographic system relies on the contrast between a **sturdy, academic serif** (Source Serif 4) and a **utilitarian, neutral sans-serif** (Inter).

- **Serif Roles:** Used for headers, book titles, and the primary reading experience. It conveys the "Folio" brand identity—authoritative yet warm.
- **Sans-Serif Roles:** Used for metadata (authors, percentages), UI controls, labels, and micro-copy. This ensures that functional elements feel distinct from content elements.
- **Scale:** On mobile, headers stay within the 20px-24px range to ensure a single-column layout doesn't feel overwhelmed.

## Layout & Spacing

The system follows a **fluid-to-contained layout model**.
- **Mobile:** Uses a single-column stack with generous 24px side margins (`container-padding`) to create a "breathing room" effect.
- **Vertical Rhythm:** Elements are spaced in multiples of 8px. Card gaps are 24px to distinguish between different entries clearly.
- **Reading Margins:** Text-heavy content is capped at a `reading-margin-max` of 640px to maintain optimal line lengths for legibility.
- **Navigation:** A fixed top header (64px height) and a bottom safe-area-aware navigation bar provide persistent anchors.

## Elevation & Depth

Elevation is primarily achieved through **Tonal Layering and Glassmorphism** rather than traditional drop shadows.

- **Level 0 (Base):** `surface` background (#121412).
- **Level 1 (Cards):** `surface-container` provides a subtle 2-3% lift in lightness.
- **Level 2 (Active/Floating):** `surface-container-high` used for currently reading cards or active states.
- **Overlays:** Menus and Headers use `surface/80` with a high `backdrop-blur` (20px+) and a very thin `outline-variant/10` border. This creates a "frosted obsidian" look that feels premium and depth-aware.
- **Shadows:** When used (e.g., book covers), shadows are "Hard" (sm or lg) with low spread to simulate the physical thickness of a book.

## Shapes

The shape language is **Organic and Welcoming**.
- **Primary Containers:** 16px (1rem) for book cards and dropdowns to feel soft to the touch.
- **Interactive Controls:** Search bars and primary buttons use "Full" roundedness (Pill-shaped) to distinguish them from content containers.
- **Book Covers:** Use a minimal 4px radius to preserve the rectangular nature of physical books while removing sharp digital edges.

## Components

- **Buttons:** Primary buttons are pill-shaped with `primary` background and `on-primary` text. Secondary buttons (like Sort) use text-only with an icon suffix and no background until hovered.
- **Search Bar:** A full-width pill with `surface-container-low` background. On focus, it transitions to `surface-container-high` with a subtle 20% opacity primary ring.
- **Book Cards:** Horizontal layout with a fixed-width cover image (96px). Includes a integrated progress bar at the bottom-right.
- **Progress Bars:** Ultra-thin (6px height) with rounded caps. The track uses `surface-variant` and the indicator uses `primary`.
- **Dropdown Menus:** Use `surface-container-highest` with `backdrop-blur`. Items have 8px padding and use `label-lg` typography.
- **Icons:** Material Symbols Outlined, weight 300, optical size 24. Icons should always be centered within a 40px touch target.