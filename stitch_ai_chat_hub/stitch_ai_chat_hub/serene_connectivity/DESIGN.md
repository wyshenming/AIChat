---
name: Serene Connectivity
colors:
  surface: '#f8f9ff'
  surface-dim: '#d0dbed'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dee9fc'
  surface-container-highest: '#d9e3f6'
  on-surface: '#121c2a'
  on-surface-variant: '#424754'
  inverse-surface: '#27313f'
  inverse-on-surface: '#eaf1ff'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#0060ac'
  on-secondary: '#ffffff'
  secondary-container: '#64a8fe'
  on-secondary-container: '#003c70'
  tertiary: '#924700'
  on-tertiary: '#ffffff'
  tertiary-container: '#b75b00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#d4e3ff'
  secondary-fixed-dim: '#a4c9ff'
  on-secondary-fixed: '#001c39'
  on-secondary-fixed-variant: '#004883'
  tertiary-fixed: '#ffdcc6'
  tertiary-fixed-dim: '#ffb786'
  on-tertiary-fixed: '#311400'
  on-tertiary-fixed-variant: '#723600'
  background: '#f8f9ff'
  on-background: '#121c2a'
  surface-variant: '#d9e3f6'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  chat-bubble:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style
This design system focuses on a **Modern, Minimalist, and Approachable** aesthetic tailored for seamless communication. The brand personality is calm, dependable, and human-centric, aiming to reduce the cognitive load and "noise" often associated with digital messaging.

The design style utilizes **Corporate Modern** principles with a soft, friendly twist. It features generous whitespace, subtle depth, and a high degree of legibility. The goal is to create an interface that feels like a quiet, organized space for meaningful conversation, avoiding aggressive visual elements in favor of smooth transitions and tactile responsiveness.

## Colors
The palette is anchored by a soft, trustworthy Blue as the primary brand color, used for key actions and active states. 

- **Primary (#3B82F6):** Used for primary buttons, active chat bubbles, and notification badges.
- **Secondary (#60A5FA):** A lighter tint for decorative elements or secondary interactive states.
- **Neutral (#1F2937):** A deep charcoal for primary text, ensuring high contrast against the light background.
- **Background (#F9FAFB):** A cool, neutral grey that provides a soft canvas, reducing screen glare compared to pure white.
- **Surface (#FFFFFF):** Reserved for cards, input fields, and message containers to create clear separation from the background.

## Typography
The system uses a pairing of **Plus Jakarta Sans** for headings and **Inter** for body text. Plus Jakarta Sans provides a modern, slightly rounded geometry that enhances the "approachable" feel, while Inter ensures maximum readability for long chat histories and system labels.

- Use `headline-lg` for screen titles (e.g., "Messages").
- Use `body-lg` or `chat-bubble` for the primary message text.
- Use `label-md` for timestamps, metadata, and small captions.
- Mobile scaling: Shift to `-mobile` variants for headers to ensure content remains the hero on smaller viewports.

## Layout & Spacing
The layout follows a **Fluid Grid** model with a base unit of 4px. 

- **Mobile:** Uses a single-column layout with 16px side margins. Touch targets are prioritized, with a minimum height of 48px for interactive elements.
- **Desktop/Tablet:** Transitions to a multi-pane layout (Sidebar for contacts, Main area for the active chat). 
- **Chat Rhythm:** Use 8px spacing between messages from the same sender, and 16px spacing when the sender changes. This creates a visual grouping of "thought blocks."

## Elevation & Depth
This design system employs **Ambient Shadows** and **Tonal Layers** to establish hierarchy without visual clutter.

- **Level 0 (Background):** #F9FAFB.
- **Level 1 (Cards/Bubbles):** #FFFFFF with a very soft, diffused shadow (0px 4px 12px rgba(0, 0, 0, 0.03)). Used for chat bubbles and contact list items.
- **Level 2 (Floating/Active):** Slightly more pronounced shadow (0px 8px 24px rgba(0, 0, 0, 0.06)). Used for floating action buttons (FABs) or active dropdown menus.
- **Interactive Depth:** On press/active states, elements should slightly decrease in elevation (reduce shadow) to simulate physical touch.

## Shapes
The shape language is defined by **Rounded-2xl** (1rem / 16px) corners. 

- **Chat Bubbles:** Use 16px rounding. Incoming bubbles have the bottom-left corner squared (4px); outgoing bubbles have the bottom-right corner squared (4px) to indicate directionality.
- **Buttons & Inputs:** Consistent 12px to 16px rounding to maintain the friendly, soft aesthetic.
- **Avatars:** Always circular to distinguish human elements from UI containers.

## Components

### Buttons
- **Primary:** Solid #3B82F6 fill with White text. 16px rounded corners.
- **Secondary:** Light blue tint background with #3B82F6 text.
- **Icon Buttons:** Circular background with a subtle shadow on hover.

### Chat Bubbles
- **Incoming:** White background, Neutral (#1F2937) text. Subtle 1px border (#F1F5F9).
- **Outgoing:** Primary (#3B82F6) background, White text. No border.

### Input Fields
- **Message Bar:** White background, 24px height minimum, 16px rounding. Uses a "ghost" placeholder text in a light grey.
- **Search:** Subtle #F1F5F9 fill to differentiate from the main surface.

### Cards & Lists
- **Contact List Item:** Transparent background by default, transitions to a very light grey (#F3F4F6) on hover or active state.
- **Avatars:** 40px (Mobile) or 48px (Desktop) diameter with a status indicator dot (Success Green) at the bottom right.

### Chips
- Used for "Quick Replies" or "Status Tags." Small (12px font), 100px rounded, with a light primary tint background.