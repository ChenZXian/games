# UI Kit Factory Spec

Version: 1.1
Last updated: 2026-04-22

This document defines the mandatory UI foundation and skin contract for Android Java mini-game generation in this repository.

UI in this repository should be token-led and structure-first:
- gameplay rendering should stay in `GameView` or `SurfaceView`
- HUD and non-real-time screens should prefer Android View or XML layers
- binary UI assets and fonts are allowed through the repository UI workflow
- licensed open-source UI resources are allowed when provenance is tracked
- hardcoded styling values in Java are still forbidden

## 1. Token Contract

All colors must use the prefix `cst_`.
All dimens must use the prefix `cst_`.

### 1.1 Color Tokens (Required)

Each skin MUST define all tokens below in `res/values/colors.xml`:

- cst_bg_main
- cst_bg_alt
- cst_panel_bg
- cst_panel_stroke
- cst_panel_header_bg
- cst_card_bg
- cst_divider
- cst_shadow

- cst_text_primary
- cst_text_secondary
- cst_text_muted
- cst_text_on_primary
- cst_text_on_secondary

- cst_accent
- cst_accent_2
- cst_danger
- cst_success
- cst_warning

- cst_btn_primary_bg_start
- cst_btn_primary_bg_end
- cst_btn_primary_stroke

- cst_btn_secondary_bg_start
- cst_btn_secondary_bg_end
- cst_btn_secondary_stroke

- cst_chip_bg
- cst_chip_stroke

- cst_meter_track
- cst_meter_fill

### 1.2 Dimen Tokens (Required)

Each skin MUST define all tokens below in `res/values/dimens.xml`:

Radii
- cst_radius_xs
- cst_radius_s
- cst_radius_m
- cst_radius_l
- cst_radius_xl

Strokes
- cst_stroke_hair
- cst_stroke_s
- cst_stroke_m

Spacing
- cst_pad_2
- cst_pad_4
- cst_pad_6
- cst_pad_8
- cst_pad_10
- cst_pad_12
- cst_pad_16
- cst_pad_20
- cst_pad_24

Typography (sp)
- cst_text_xs
- cst_text_s
- cst_text_m
- cst_text_l
- cst_text_xl
- cst_text_xxl

HUD sizing
- cst_hud_height
- cst_hud_badge_min_w
- cst_hud_icon_size

## 2. Required UI Kit Files

All projects MUST include the following foundation files.

### 2.1 Values

- res/values/colors.xml
- res/values/dimens.xml
- res/values/styles.xml
- res/values/themes.xml

### 2.2 Drawable Resources

The logical resources below must exist.
Implementations may be XML, PNG, WEBP, or 9-patch unless a vector icon is explicitly preferred.

Foundations
- res/drawable/ui_panel.*
- res/drawable/ui_panel_header.*
- res/drawable/ui_card.*
- res/drawable/ui_divider.*

Buttons
- res/drawable/ui_button_primary.*
- res/drawable/ui_button_secondary.*
- res/drawable/ui_button_danger.*
- res/drawable/ui_button_icon.*

Badges and chips
- res/drawable/ui_badge.*
- res/drawable/ui_chip.*

Meters
- res/drawable/ui_meter_track.*
- res/drawable/ui_meter_fill.*

Overlays
- res/drawable/ui_toast.*
- res/drawable/ui_dialog.*

Icons
- res/drawable/ic_play.*
- res/drawable/ic_pause.*
- res/drawable/ic_restart.*
- res/drawable/ic_sound_on.*
- res/drawable/ic_sound_off.*
- res/drawable/ic_help.*

## 3. Style Contract

### 3.1 TextAppearance (Required)

Define TextAppearance styles in `res/values/styles.xml`:

- TextAppearance.Game.Title
- TextAppearance.Game.Subtitle
- TextAppearance.Game.Body
- TextAppearance.Game.Caption
- TextAppearance.Game.HudValue
- TextAppearance.Game.HudLabel

### 3.2 Widget Styles (Required)

Define Widget styles in `res/values/styles.xml`:

- Widget.Game.Button.Primary
- Widget.Game.Button.Secondary
- Widget.Game.Button.Icon
- Widget.Game.Panel
- Widget.Game.Card
- Widget.Game.Chip
- Widget.Game.Badge
- Widget.Game.Meter

Layouts MUST reference these styles instead of inline styling.

Widget styles MUST continue to exist even when binary UI assets or imported fonts are used.

### 3.3 Asset Provenance

Reusable third-party UI resources should be staged under `shared_assets/ui/<pack_id>/`.

Each imported pack should include:

- `manifest.json`
- `LICENSE` or equivalent license file
- source URL or origin metadata

Project-local copies may be derived from those shared assets as needed.

## 4. Skins

Each project MUST select exactly one skin id.
Skins define the primary UI direction.
A project may pair one skin with one compatible asset pack, but it must still have exactly one primary `ui_skin`.

### 4.1 skin_dark_arcade

Goal: Dark glass, strong contrast, punchy accent.

Suggested colors:
- cst_bg_main: #0B1020
- cst_bg_alt: #111A33
- cst_panel_bg: #121B36
- cst_panel_stroke: #2B3A66
- cst_panel_header_bg: #18244A
- cst_card_bg: #101938
- cst_divider: #2B3A66
- cst_shadow: #66000000

- cst_text_primary: #F2F6FF
- cst_text_secondary: #B9C7FF
- cst_text_muted: #7F93D6
- cst_text_on_primary: #071022
- cst_text_on_secondary: #F2F6FF

- cst_accent: #6EF6FF
- cst_accent_2: #9B7CFF
- cst_danger: #FF4D6D
- cst_success: #43F7A0
- cst_warning: #FFD166

- cst_btn_primary_bg_start: #6EF6FF
- cst_btn_primary_bg_end: #4AA7FF
- cst_btn_primary_stroke: #A7FBFF

- cst_btn_secondary_bg_start: #2B3A66
- cst_btn_secondary_bg_end: #1B274D
- cst_btn_secondary_stroke: #3D53A1

- cst_chip_bg: #18244A
- cst_chip_stroke: #2B3A66

- cst_meter_track: #2B3A66
- cst_meter_fill: #6EF6FF

### 4.2 skin_cartoon_light

Goal: Bright, friendly, thick strokes, soft shadows.

Suggested colors:
- cst_bg_main: #F7FBFF
- cst_bg_alt: #EAF4FF
- cst_panel_bg: #FFFFFF
- cst_panel_stroke: #B7D7FF
- cst_panel_header_bg: #DDF0FF
- cst_card_bg: #FFFFFF
- cst_divider: #B7D7FF
- cst_shadow: #33000000

- cst_text_primary: #17233B
- cst_text_secondary: #2E4A7A
- cst_text_muted: #5D7BB0
- cst_text_on_primary: #FFFFFF
- cst_text_on_secondary: #17233B

- cst_accent: #2ED6FF
- cst_accent_2: #7B61FF
- cst_danger: #FF4D4D
- cst_success: #2ED67A
- cst_warning: #FFB020

- cst_btn_primary_bg_start: #2ED6FF
- cst_btn_primary_bg_end: #33A1FF
- cst_btn_primary_stroke: #A6F0FF

- cst_btn_secondary_bg_start: #FFFFFF
- cst_btn_secondary_bg_end: #EAF4FF
- cst_btn_secondary_stroke: #B7D7FF

- cst_chip_bg: #EAF4FF
- cst_chip_stroke: #B7D7FF

- cst_meter_track: #B7D7FF
- cst_meter_fill: #2ED67A

### 4.3 skin_neon_future

Goal: Deep black, neon gradients, high-energy UI.

Suggested colors:
- cst_bg_main: #07070C
- cst_bg_alt: #0E0E17
- cst_panel_bg: #0E0E17
- cst_panel_stroke: #2A2A3D
- cst_panel_header_bg: #121227
- cst_card_bg: #0B0B14
- cst_divider: #2A2A3D
- cst_shadow: #77000000

- cst_text_primary: #F7F7FF
- cst_text_secondary: #C7C7FF
- cst_text_muted: #8A8AFF
- cst_text_on_primary: #07070C
- cst_text_on_secondary: #F7F7FF

- cst_accent: #00F5FF
- cst_accent_2: #FF3DFF
- cst_danger: #FF3D5A
- cst_success: #20FFB2
- cst_warning: #FFD23D

- cst_btn_primary_bg_start: #00F5FF
- cst_btn_primary_bg_end: #FF3DFF
- cst_btn_primary_stroke: #7CFFFF

- cst_btn_secondary_bg_start: #2A2A3D
- cst_btn_secondary_bg_end: #121227
- cst_btn_secondary_stroke: #3E3E63

- cst_chip_bg: #121227
- cst_chip_stroke: #2A2A3D

- cst_meter_track: #2A2A3D
- cst_meter_fill: #20FFB2

### 4.4 skin_post_apocalypse

Goal: Dusty metal, worn panels, hazard accents.

Suggested colors:
- cst_bg_main: #14120F
- cst_bg_alt: #1E1A15
- cst_panel_bg: #1E1A15
- cst_panel_stroke: #4A4036
- cst_panel_header_bg: #2A241D
- cst_card_bg: #171410
- cst_divider: #4A4036
- cst_shadow: #77000000

- cst_text_primary: #F3E9D7
- cst_text_secondary: #D6C3A3
- cst_text_muted: #A89273
- cst_text_on_primary: #14120F
- cst_text_on_secondary: #F3E9D7

- cst_accent: #F2B544
- cst_accent_2: #D97757
- cst_danger: #E84A3C
- cst_success: #67D37E
- cst_warning: #F2B544

- cst_btn_primary_bg_start: #F2B544
- cst_btn_primary_bg_end: #D97757
- cst_btn_primary_stroke: #FFE2A6

- cst_btn_secondary_bg_start: #4A4036
- cst_btn_secondary_bg_end: #2A241D
- cst_btn_secondary_stroke: #6A5A4B

- cst_chip_bg: #2A241D
- cst_chip_stroke: #4A4036

- cst_meter_track: #4A4036
- cst_meter_fill: #67D37E

### 4.5 skin_military_tech

Goal: Tactical, clean, readable, restrained accents.

Suggested colors:
- cst_bg_main: #0B1110
- cst_bg_alt: #101A18
- cst_panel_bg: #101A18
- cst_panel_stroke: #27413A
- cst_panel_header_bg: #13211E
- cst_card_bg: #0E1715
- cst_divider: #27413A
- cst_shadow: #66000000

- cst_text_primary: #E8FFF8
- cst_text_secondary: #BFEBDD
- cst_text_muted: #7FB8A8
- cst_text_on_primary: #0B1110
- cst_text_on_secondary: #E8FFF8

- cst_accent: #4FE3C1
- cst_accent_2: #73A7FF
- cst_danger: #FF5A5F
- cst_success: #4FE38A
- cst_warning: #FFD166

- cst_btn_primary_bg_start: #4FE3C1
- cst_btn_primary_bg_end: #2FA7FF
- cst_btn_primary_stroke: #B7FFF0

- cst_btn_secondary_bg_start: #27413A
- cst_btn_secondary_bg_end: #13211E
- cst_btn_secondary_stroke: #356154

- cst_chip_bg: #13211E
- cst_chip_stroke: #27413A

- cst_meter_track: #27413A
- cst_meter_fill: #4FE38A

## 5. Layout Usage Rules

- Use UI Kit drawables for panels and buttons.
- Use TextAppearance styles for all text.
- Use only tokens for colors and dimens.
- Avoid inline color values in layout XML.
- Avoid runtime allocations for UI styling.
- Decorative borders, bezels, corner ornaments, and panel chrome must not cover active gameplay tiles, lanes, routes, board cells, spawn zones, or touch-critical action areas.
- If a framed presentation is desired, reserve that area structurally with layout bounds, padding, or explicit dead-space margins instead of laying the frame over the live playfield.
- Floating HUD elements may overlap only non-interactive dead space that is declared in the UI brief.

End of document.
