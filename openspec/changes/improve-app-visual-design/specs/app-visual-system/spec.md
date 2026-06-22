## ADDED Requirements

### Requirement: Stable product theme
The app SHALL use a stable light and dark color system that communicates recording, local processing, success, warning, and error states without depending on Android dynamic color.

#### Scenario: Product colors remain stable
- **WHEN** the app is opened on two Android 12+ devices with different system wallpapers
- **THEN** the app uses the same product color roles on both devices

#### Scenario: Dark theme remains legible
- **WHEN** the device uses dark theme
- **THEN** primary text, secondary text, controls, status colors, and surfaces retain accessible visual contrast

### Requirement: Consistent typography and spacing
The app SHALL use a shared typography scale and spacing rhythm so page titles, section titles, metadata, body content, and control labels have consistent hierarchy.

#### Scenario: Content hierarchy is scannable
- **WHEN** a user opens any top-level or detail screen
- **THEN** the page title, section titles, primary content, and supporting metadata are visually distinguishable without relying only on color

### Requirement: Familiar icon controls
The app SHALL use familiar Material icons for navigation and common commands where an icon exists, while preserving accessible labels or content descriptions.

#### Scenario: Icon-only command is accessible
- **WHEN** a screen-reader user focuses an icon-only command
- **THEN** the command exposes a meaningful content description

#### Scenario: Navigation uses recognizable symbols
- **WHEN** the bottom navigation is displayed
- **THEN** Home, AI, and Mine destinations each show a recognizable icon and a text label

### Requirement: Predictable component geometry
The app SHALL use stable control dimensions, card radii no greater than 8 dp, and responsive layouts that do not resize or overlap when labels or state text change.

#### Scenario: Compact screen has no horizontal overflow
- **WHEN** the app is displayed on a compact phone viewport
- **THEN** controls wrap, stack, or collapse without clipping text or overlapping adjacent content

#### Scenario: Dynamic state does not shift core navigation
- **WHEN** recording, processing, error, or completion labels change
- **THEN** the bottom navigation and primary page chrome remain in stable positions

### Requirement: Semantic status presentation
The app SHALL pair status color with text and, where appropriate, an icon so recording and processing states are not communicated by color alone.

#### Scenario: Model status is understandable without color
- **WHEN** model status is loaded, missing, or failed
- **THEN** each state has a readable text label and semantic icon in addition to its color treatment
