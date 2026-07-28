---
name: Folio
colors:
  surface: '#fff8f1'
  surface-dim: '#e2d9ca'
  surface-bright: '#fff8f1'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fcf2e3'
  surface-container: '#f6edde'
  surface-container-high: '#f0e7d8'
  surface-container-highest: '#eae1d2'
  on-surface: '#1f1b12'
  on-surface-variant: '#434843'
  inverse-surface: '#343026'
  inverse-on-surface: '#f9f0e0'
  outline: '#737872'
  outline-variant: '#c3c8c1'
  surface-tint: '#516353'
  primary: '#475949'
  on-primary: '#ffffff'
  primary-container: '#5f7161'
  on-primary-container: '#e1f5e1'
  inverse-primary: '#b8ccb9'
  secondary: '#625e56'
  on-secondary: '#ffffff'
  secondary-container: '#e6dfd5'
  on-secondary-container: '#67625b'
  tertiary: '#575547'
  on-tertiary: '#ffffff'
  tertiary-container: '#706d5f'
  on-tertiary-container: '#f5f0de'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d4e8d4'
  primary-fixed-dim: '#b8ccb9'
  on-primary-fixed: '#0f1f13'
  on-primary-fixed-variant: '#3a4b3c'
  secondary-fixed: '#e9e1d8'
  secondary-fixed-dim: '#ccc5bd'
  on-secondary-fixed: '#1e1b16'
  on-secondary-fixed-variant: '#4a463f'
  tertiary-fixed: '#e7e3d1'
  tertiary-fixed-dim: '#cbc7b5'
  on-tertiary-fixed: '#1d1c11'
  on-tertiary-fixed-variant: '#49473a'
  background: '#fff8f1'
  on-background: '#1f1b12'
  surface-variant: '#eae1d2'
typography:
  display-lg:
    fontFamily: Source Serif 4
    fontSize: 40px
    fontWeight: '600'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Source Serif 4
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Source Serif 4
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  reading-body:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 30px
  ui-label-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  ui-label-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.02em
  caption:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-padding: 24px
  gutter: 16px
  reading-margin-max: 640px
---

## Brand & Style
The design system is centered on the concept of "Digital Sanctuary." It aims to evoke the tactile comfort of a physical library and the mental clarity of a quiet reading nook. The brand personality is scholarly yet approachable, prioritizing the user's focus and emotional well-being over engagement metrics.

The visual style is a blend of **Soft Minimalism** and **Contemporary Editorial**. It utilizes generous whitespace to reduce cognitive load and emphasizes a "paper-like" quality through color and texture. The goal is to make the digital screen feel less like a backlit device and more like a reflective surface, fostering a sense of calm, rest, and intellectual immersion.

## Colors
The palette is derived from natural, sun-bleached elements to ensure low eye strain during long reading sessions.

- **Primary (Sage Green):** Used for active states, primary actions, and branding accents. It represents growth and tranquility.
- **Secondary (Muted Stone):** Used for borders, inactive states, and subtle UI dividers.
- **Tertiary (Cream/Paper):** The core background color for the reading environment, mimicking high-quality book paper.
- **Neutral (Warm Charcoal):** Used for typography to provide high legibility without the harshness of pure black.
- **Background (Parchment):** A very soft, off-white base for the application frame to maintain a warm, cozy atmosphere.

## Typography
The system employs a dual-font strategy to distinguish between "Content" and "Utility."

- **Source Serif 4** is used for all headlines and the primary reading experience. Its high x-height and classic proportions provide an authoritative yet warm editorial feel.
- **Inter** is used for the user interface (menus, settings, buttons). Its systematic, neutral design ensures that the UI remains functional and unobtrusive, never competing with the book content.

Reading text should be set with generous line heights (1.6x minimum) to ensure maximum comfort.

## Layout & Spacing
The layout philosophy is **Fixed-Width Centered** for reading and **Fluid** for library management. 

- **Reading View:** Content is constrained to a `reading-margin-max` of 640px on desktop to maintain optimal line lengths (50-75 characters). 
- **Grid:** A soft 8px grid governs all spatial relationships.
- **Mobile:** Margins reduce to 20px, and the grid shifts to a single column. 
- **Whitespace:** Use "negative space" aggressively. Dialogs and menus should have significant internal padding to feel airy rather than cramped.

## Elevation & Depth
This design system avoids heavy shadows and high-contrast depth. It uses **Tonal Layering** and **Ambient Softness**.

- **Surface Levels:** The base layer is the Parchment background. Floating elements (like the reader bar or settings panel) use the Tertiary Cream color with a very soft, large-radius shadow (15% opacity of the Neutral color) to suggest a gentle lift rather than a harsh drop.
- **Glassmorphism:** Subtle backdrop blurs (8px) are used on the navigation bar to maintain a sense of context of the "page" beneath the interface.
- **Transitions:** All elevation changes should be animated with slow, graceful "ease-in-out" curves (300ms) to maintain the peaceful atmosphere.

## Shapes
The shape language is organic and soft. There are no sharp corners in this design system. 

- **Primary Radius:** 0.5rem (8px) is the standard for cards and input fields.
- **Large Radius:** 1.5rem (24px) is used for "cozy" containers like bottom sheets and the main reader interface.
- **Interactive Elements:** Buttons utilize the `rounded-lg` (16px) setting to feel approachable and tactile, like a smooth stone.

## Components
- **Buttons:** Use a "pill" or highly rounded shape. The primary button is filled Sage Green with white text. Secondary buttons use a Sage Green outline with a subtle cream hover state.
- **Cards (Library):** Book covers should have a very slight 2px radius and a soft bottom shadow to look like they are resting on a shelf. Title and Author metadata are set in Inter.
- **Input Fields:** Soft cream background with a 1px Stone border. Focus states use a Sage Green glow rather than a thick border.
- **Progress Bars:** Thin, Sage Green lines with rounded caps. Avoid "percentage numbers" unless necessary; use a subtle "Chapter X of Y" label instead.
- **Reading Controls:** Icons should be "Line" style (2pt stroke) with rounded ends to match the typography's softness.
- **The "Bookmark":** A signature component that uses a vertical ribbon shape in a Muted Earth Tone to mark current progress.