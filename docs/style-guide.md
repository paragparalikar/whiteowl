# WhiteOwl Workbench — UI/UX Style Guide

> **Version:** 2.3
> **Target Platform:** JavaFX 21+ on Windows, macOS, Linux
> **Design Language:** IntelliJ IDEA Darcula — faithful reproduction

---

## 1. Design Philosophy

The UI must look and feel **identical to IntelliJ IDEA's Darcula theme**:

- **Bluish radial gradient background** — the root window uses a subtle radial gradient from `#2d3548` (center) to `#1e1f22` (edges).
- **Transparent chrome** — title bar, sidebar, and status bar are transparent so the gradient shows through.
- **Deep content area** — content panels use `#1e1f22`, a near-black background for contrast.
- **Zero visible borders** on controls — depth comes from background color layering only.
- **Compact, dense layout** — small fonts, tight padding, no wasted space.
- **Muted blue accent** (#4A78C2) for selection and active indicators — not purple, not bright.
- **System-native feel** — uses the same font families IntelliJ uses on each OS.
- **Icon-only buttons** — all window buttons and sidebar items use SVG icons, never Unicode text symbols.
- **Split pane layout** — sidebar and content area are in a SplitPane with a transparent 4px gap showing the gradient.
- **Rounded panel corners** — sidebar and content area have 8px rounded top-left corners.

---

## 2. Color Palette

### 2.1 Dark Theme (Darcula)

| Token            | HEX       | Usage                                       |
| ---------------- | --------- | ------------------------------------------- |
| `bg-gradient-center` | `#2d3548` | Center of radial gradient (bluish)      |
| `bg-gradient-edge`   | `#1e1f22` | Edge of radial gradient (near-black)    |
| `bg-content`     | `#1e1f22` | Content area, editor background             |
| `bg-card`        | `#2b2d30` | Cards, panels inside content area           |
| `bg-input`       | `#45494a` | Input fields, combo boxes, editable areas   |
| `bg-popup`       | `#46484a` | Popups, tooltips, context menus, dropdowns  |
| `bg-titlebar`    | `transparent` | Title bar — gradient shows through       |
| `bg-sidebar`     | `#2b2d30` | Sidebar — solid deep dark                   |
| `bg-statusbar`   | `transparent` | Status bar — gradient shows through      |
| `bg-tab`         | `#2b2b2b` | Inactive tab background                     |
| `bg-tab-active`  | `#4e5254` | Active/selected tab background              |
| `bg-hover`       | `#353739` | Hover highlight on lists, trees, buttons    |
| `bg-selected`    | `#2d5c88` | Selected row in lists, tables, trees        |
| `separator`      | `#515151` | Separator lines only (sparingly)            |
| `accent`         | `#4A78C2` | Focus ring, active tab underline, links     |
| `accent-hover`   | `#3E6BAD` | Hover state for primary accent elements     |
| `text-primary`   | `#bbbbbb` | Default text — labels, body, headings       |
| `text-secondary` | `#999999` | Subdued text — descriptions, metadata       |
| `text-chrome`    | `#6e7681` | Title bar, sidebar items, window button icons |
| `text-disabled`  | `#777777` | Disabled labels, inactive controls          |
| `text-on-accent` | `#ffffff` | Text on accent-colored backgrounds          |
| `green`          | `#6a8759` | Strings, success indicators                 |
| `orange`         | `#cc7832` | Keywords, warnings                          |
| `red`            | `#ff6b68` | Errors, close-button hover                  |
| `blue`           | `#6897bb` | Numbers, informational highlights           |
| `purple`         | `#9876aa` | Type names, special identifiers             |
| `yellow`         | `#ffc66d` | Function names, annotations                 |

### 2.2 Key Contrast Rules

- `text-primary` (#bbbbbb) on `bg-primary` (#2b2b2b) ≈ 7.5:1 — WCAG AAA.
- `text-secondary` (#999999) on `bg-primary` (#2b2b2b) ≈ 5.0:1 — WCAG AA.
- `text-primary` on `bg-selected` (#2d5c88) ≈ 4.6:1 — WCAG AA.

---

## 3. Typography

### 3.1 UI Font Stack

```css
-fx-font-family: ".SF NS Text", "Segoe UI", "Ubuntu", system;
```

IntelliJ uses the platform's native UI font. Do **not** use Inter or custom web fonts. Let the OS provide the font:

- **Windows:** Segoe UI
- **macOS:** .SF NS Text (San Francisco)
- **Linux:** Ubuntu or system default

### 3.2 Monospaced Font Stack

```css
-fx-font-family: "JetBrains Mono", "Consolas", "Menlo", monospace;
```

Used for code displays, data tables with numeric values, log output.

### 3.3 Type Scale

| Role             | Size   | Weight  | Usage                                      |
| ---------------- | ------ | ------- | ------------------------------------------ |
| **Section Head** | 13px   | Bold    | Panel titles, section headers (same as body but bold) |
| **Body**         | 13px   | Regular | All UI text — labels, buttons, fields      |
| **Small**        | 12px   | Regular | Tree nodes, table headers, sidebar items, metadata |
| **Caption**      | 11px   | Regular | Status bar, card titles, sidebar header, breadcrumbs |
| **Code**         | 13px   | Regular | Monospaced data, log output                |

> IntelliJ does **not** use large headings inside tool windows. Section titles are the same 13px body size, just bold. There are no 16px or 20px headings.

### 3.5 Sidebar Typography

- **Sidebar header:** 11px Regular, `text-secondary` (#999999) — understated, not prominent.
- **Sidebar items:** 12px Regular, `text-secondary` (#999999) — same as tree node text.
- **Sidebar active item:** 12px Regular, `text-on-accent` (#ffffff) — text turns white on selection.
- Items use **Small (12px)** size, not Body (13px). The sidebar must feel compact like a tool window tab strip.

### 3.4 JavaFX CSS

```css
.root {
    -fx-font-family: ".SF NS Text", "Segoe UI", "Ubuntu", system;
    -fx-font-size: 13px;
}

.section-head {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
}

.small-text {
    -fx-font-size: 12px;
}

.caption-text {
    -fx-font-size: 11px;
}
```

---

## 4. Spacing

### 4.1 Density

IntelliJ uses **compact spacing**. Padding is small. Gaps between elements are tight. There is no generous whitespace.

### 4.2 Component Padding

| Component       | Padding (T R B L)   | Notes                        |
| --------------- | -------------------- | ---------------------------- |
| Button          | `4 12 4 12`         | Compact, no excess height    |
| TextField       | `2 6 2 6`           | Minimal, just enough room    |
| ComboBox        | `2 6 2 6`           | Same as text field           |
| Tree cell       | `1 4 1 4`           | Very tight                   |
| Table cell      | `2 6 2 6`           | Dense data display           |
| Card / Panel    | `6 8 6 8`           | Compact panel padding        |
| Sidebar         | `0 0 0 0`           | No padding, items continuous, no VBox spacing |
| Content area    | `8 8 8 8`           | Tight content margin         |
| Status bar      | `2 8 2 8`           | Minimal                      |
| Title bar       | `0 0 0 8`           | Left-aligned title           |
| Tab             | `4 12 4 12`         | Compact tab padding          |

### 4.3 Layout Dimensions

| Element         | Value  |
| --------------- | ------ |
| Sidebar width   | 180px  |
| Title bar height| 28px   |
| Status bar height| 20px  |
| Tab height      | 26px   |
| Sidebar item height | 24px |
| Min control height | 24px |
| Card gap        | 0px (flush, no gaps between adjacent cards) |
| Min window size | 800×600 |
| Default size    | 85% of screen visual bounds, centered |

---

## 5. Borderless Design

### 5.1 Rule

**No borders on any control.** All `-fx-border-color` must be `transparent`, all `-fx-border-width` must be `0`.

Depth and separation are achieved **only** through background color differences between layers:

```
bg-primary (#2b2b2b)  →  bg-panel (#3c3f41)  →  bg-input (#45494a)
```

### 5.2 Exceptions

| Element               | Border                                              |
| --------------------- | --------------------------------------------------- |
| Sidebar active button | 2px left bar in `accent` (#4A78C2)                  |
| Selected tab          | 2px bottom underline in `accent`                    |
| Focus indicator       | 1px bottom underline in `accent` on `:focused`      |
| Separator lines       | 1px using `separator` (#515151) — use sparingly     |

### 5.3 Corner Radius

| Element         | Radius |
| --------------- | ------ |
| Buttons         | 3px    |
| Inputs, combos  | 3px    |
| Popups, tooltips| 3px    |
| Cards           | 0px    |
| Tabs            | 0px    |
| Sidebar buttons | 0px    |

---

## 6. Components

### 6.1 Button

```css
.button {
    -fx-background-color: #45494a;
    -fx-text-fill: #bbbbbb;
    -fx-background-radius: 3;
    -fx-background-insets: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 4 12 4 12;
    -fx-font-size: 13px;
    -fx-min-height: 24;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: #4e5254;
}

.button:focused {
    -fx-border-color: transparent transparent #4A78C2 transparent;
    -fx-border-width: 0 0 1 0;
}

.button:disabled {
    -fx-background-color: #3c3f41;
    -fx-text-fill: #777777;
    -fx-cursor: default;
}
```

**Primary action button:**

```css
.action-button {
    -fx-background-color: #365880;
    -fx-text-fill: #ffffff;
    -fx-background-radius: 3;
    -fx-background-insets: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-font-weight: bold;
}

.action-button:hover {
    -fx-background-color: #4A78C2;
}

.action-button:disabled {
    -fx-background-color: #3c3f41;
    -fx-text-fill: #777777;
}
```

### 6.2 TextField

```css
.text-field {
    -fx-background-color: #45494a;
    -fx-text-fill: #bbbbbb;
    -fx-prompt-text-fill: #777777;
    -fx-background-radius: 3;
    -fx-background-insets: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 2 6 2 6;
    -fx-min-height: 24;
}

.text-field > .content {
    -fx-background-color: #45494a;
    -fx-background-radius: 3;
}

.text-field:focused {
    -fx-border-color: transparent transparent #4A78C2 transparent;
    -fx-border-width: 0 0 1 0;
}

.text-field:disabled {
    -fx-background-color: #3c3f41;
    -fx-text-fill: #777777;
}
```

### 6.3 ComboBox

```css
.combo-box {
    -fx-background-color: #45494a;
    -fx-background-radius: 3;
    -fx-background-insets: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-min-height: 24;
}

.combo-box .list-cell {
    -fx-text-fill: #bbbbbb;
    -fx-background-color: transparent;
}

.combo-box-popup > .list-view {
    -fx-background-color: #46484a;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);
}

.combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell {
    -fx-text-fill: #bbbbbb;
    -fx-background-color: transparent;
    -fx-padding: 2 6 2 6;
}

.combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell:hover {
    -fx-background-color: #353739;
}

.combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell:filled:selected {
    -fx-background-color: #2d5c88;
    -fx-text-fill: #ffffff;
}
```

### 6.4 TableView

```css
.table-view {
    -fx-background-color: #2b2b2b;
    -fx-border-color: transparent;
    -fx-fixed-cell-size: 24;
}

.table-view .column-header-background {
    -fx-background-color: #3c3f41;
}

.table-view .column-header {
    -fx-background-color: #3c3f41;
    -fx-border-color: transparent;
}

.table-view .column-header .label {
    -fx-text-fill: #999999;
    -fx-font-size: 12px;
}

.table-view .table-row-cell {
    -fx-background-color: #2b2b2b;
    -fx-border-color: transparent;
}

.table-view .table-row-cell:odd {
    -fx-background-color: #313335;
}

.table-view .table-row-cell:selected {
    -fx-background-color: #2d5c88;
}

.table-view .table-row-cell:selected .text {
    -fx-fill: #ffffff;
}

.table-view .table-row-cell:empty {
    -fx-background-color: #2b2b2b;
}

.table-view .table-cell {
    -fx-text-fill: #bbbbbb;
    -fx-padding: 2 6 2 6;
    -fx-alignment: center-left;
}
```

### 6.5 TreeView

```css
.tree-view {
    -fx-background-color: #3c3f41;
    -fx-border-color: transparent;
    -fx-indent: 12;
    -fx-fixed-cell-size: 22;
}

.tree-view .tree-cell {
    -fx-text-fill: #bbbbbb;
    -fx-background-color: transparent;
    -fx-padding: 1 4 1 4;
    -fx-font-size: 12px;
}

.tree-view .tree-cell:selected {
    -fx-background-color: #2d5c88;
    -fx-text-fill: #ffffff;
}

.tree-view .tree-cell:hover {
    -fx-background-color: #353739;
}
```

### 6.6 TabPane

```css
.tab-pane > .tab-header-area > .tab-header-background {
    -fx-background-color: #3c3f41;
}

.tab-pane > .tab-header-area > .headers-region > .tab {
    -fx-background-color: #3c3f41;
    -fx-background-radius: 0;
    -fx-background-insets: 0;
    -fx-padding: 4 12 4 12;
    -fx-border-color: transparent;
}

.tab-pane > .tab-header-area > .headers-region > .tab:selected {
    -fx-background-color: #4e5254;
    -fx-border-color: transparent transparent #4A78C2 transparent;
    -fx-border-width: 0 0 2 0;
}

.tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {
    -fx-text-fill: #999999;
    -fx-font-size: 12px;
}

.tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {
    -fx-text-fill: #bbbbbb;
}
```

### 6.7 ScrollBar

```css
.scroll-bar {
    -fx-background-color: transparent;
}

.scroll-bar:vertical {
    -fx-pref-width: 6;
}

.scroll-bar:horizontal {
    -fx-pref-height: 6;
}

.scroll-bar .thumb {
    -fx-background-color: #515151;
    -fx-background-radius: 3;
}

.scroll-bar .thumb:hover {
    -fx-background-color: #666666;
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
}

.scroll-bar .increment-arrow,
.scroll-bar .decrement-arrow {
    -fx-shape: "";
    -fx-padding: 0;
}
```

### 6.8 CheckBox

```css
.check-box {
    -fx-text-fill: #bbbbbb;
    -fx-font-size: 13px;
}

.check-box .box {
    -fx-background-color: #45494a;
    -fx-background-radius: 3;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 3;
}

.check-box:selected .box {
    -fx-background-color: #365880;
}

.check-box:selected .box .mark {
    -fx-background-color: #ffffff;
}

.check-box:focused .box {
    -fx-border-color: transparent transparent #4A78C2 transparent;
    -fx-border-width: 0 0 1 0;
}
```

### 6.9 Tooltip

```css
.tooltip {
    -fx-background-color: #46484a;
    -fx-text-fill: #bbbbbb;
    -fx-background-radius: 3;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 4 8 4 8;
    -fx-font-size: 12px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);
}
```

### 6.10 Context Menu

```css
.context-menu {
    -fx-background-color: #46484a;
    -fx-background-radius: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 2 0 2 0;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);
}

.context-menu .menu-item {
    -fx-background-color: transparent;
    -fx-padding: 4 16 4 16;
}

.context-menu .menu-item:hover,
.context-menu .menu-item:focused {
    -fx-background-color: #2d5c88;
}

.context-menu .menu-item .label {
    -fx-text-fill: #bbbbbb;
    -fx-font-size: 13px;
}

.context-menu .separator {
    -fx-padding: 2 0 2 0;
}
```

### 6.11 Separator

```css
.separator .line {
    -fx-border-color: #515151;
    -fx-border-width: 0 0 1 0;
}
```

---

## 7. Shadows & Depth

### 7.1 Philosophy

Completely flat. Depth comes from **background color stepping** only. Shadows are used **exclusively** on floating elements (popups, dropdowns, tooltips, dialogs).

### 7.2 Layer Stack

```
radial-gradient(#2d3548 → #1e1f22)  →  Root window background
#1e1f22  →  Content area background (deepest, opaque)
#232427  →  Alternating table rows
#2b2d30  →  Cards, table headers
#2b2d30  →  Sidebar (solid dark, rounded top-left 8px)
transparent  →  Title bar, status bar (gradient shows through)
transparent  →  SplitPane divider (4px gap, gradient shows through)
rgba(255,255,255,0.06)  →  Hover highlight on transparent areas
rgba(74,120,194,0.25)   →  Active sidebar item
#45494a  →  Input fields, buttons
#46484a  →  Popups, tooltips, context menus (floating)
#4e5254  →  Active tab
```

### 7.3 Floating Shadow

```css
-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);
```

Applied only to popups, tooltips, context menus, and dialogs. Never on cards, panels, or content.

---

## 8. Custom Window Chrome

### 8.1 Title Bar

| Property     | Value            |
| ------------ | ---------------- |
| Height       | 28px             |
| Background   | `transparent`    |
| Title font   | 12px, Regular    |
| Title color  | `#999999`        |
| Left padding | 8px              |

> Title bar is transparent so the root radial gradient shows through.

### 8.2 Window Buttons (Minimize, Maximize, Close)

| Property         | Value                      |
| ---------------- | -------------------------- |
| Width            | 36px                       |
| Height           | 28px                       |
| Corner radius    | 0px                        |
| Icon style       | SVG stroke, 1.2px, no fill |
| Icon color       | `#999999` (stroke)         |
| Hover icon color | `#bbbbbb`                  |
| Hover background | `rgba(255,255,255,0.08)`   |
| Close hover bg   | `#ff6b68`                  |
| Close hover icon | `#ffffff`                  |

#### SVG Icon Paths (14×14 viewport)

| Button   | SVG Path (`d` attribute)                                          |
| -------- | ----------------------------------------------------------------- |
| Minimize | `M3,7 L11,7`                                                     |
| Maximize | `M2,2 L12,2 L12,12 L2,12 Z`                                      |
| Restore  | `M4,4 L4,1 L13,1 L13,10 L10,10 M1,4 L10,4 L10,13 L1,13 Z`      |
| Close    | `M2,2 L12,12 M12,2 L2,12`                                        |

> All window buttons are **icon-only** (no text). Icons use `SVGPath` with stroke rendering.

### 8.3 Resize Handles

| Property        | Value              |
| --------------- | ------------------ |
| Margin          | 5px from each edge |
| Cursor          | Directional resize |
| Min window size | 800 × 600         |

### 8.4 Sidebar

#### Layout Rules

- **Width:** Fixed 180px.
- **No padding** on the sidebar VBox — items go edge-to-edge.
- **Items are continuous** — no visual gaps, no spacing between buttons. VBox spacing must be `0`.
- **Sidebar header** is understated: 11px, `text-secondary` (#999999), with padding `4 12 4 12`. Not bold.
- **Each item** is exactly 24px tall, flush against the next item.
- The sidebar must look like IntelliJ's tool window sidebar — a dense, tight list of options.

#### Icons in Sidebar

- **Do NOT use Unicode text symbols** for sidebar icons. They look unprofessional and render inconsistently.
- Use `SVGPath` nodes (16×16) placed in an `HBox` alongside the text `Label`.
- Each sidebar button is an `HBox` containing: `[SVGPath icon, 6px gap, Label text]`.
- Icon fill color matches text color: `#999999` default, `#ffffff` on active, `#bbbbbb` on hover.
- See §9 Iconography for SVG path data.

```css
.sidebar {
    -fx-background-color: #3c3f41;
    -fx-padding: 0;
}

.sidebar-header {
    -fx-font-size: 11px;
    -fx-text-fill: #999999;
    -fx-padding: 4 12 4 12;
}

.sidebar-button {
    -fx-background-color: transparent;
    -fx-text-fill: #999999;
    -fx-font-size: 12px;
    -fx-padding: 4 12 4 12;
    -fx-alignment: center-left;
    -fx-background-radius: 0;
    -fx-background-insets: 0;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-min-height: 24;
    -fx-max-height: 24;
    -fx-cursor: hand;
}

.sidebar-button:hover {
    -fx-background-color: #353739;
    -fx-text-fill: #bbbbbb;
}

.sidebar-button-active {
    -fx-background-color: #2d5c88;
    -fx-text-fill: #ffffff;
    -fx-border-color: transparent transparent transparent #4A78C2;
    -fx-border-width: 0 0 0 2;
    -fx-padding: 4 12 4 10;
}

.sidebar-icon {
    -fx-fill: #999999;
}

.sidebar-button-active .sidebar-icon {
    -fx-fill: #ffffff;
}

.sidebar-button:hover .sidebar-icon {
    -fx-fill: #bbbbbb;
}
```

### 8.5 Status Bar

```css
.status-bar {
    -fx-background-color: #3c3f41;
    -fx-padding: 2 8 2 8;
}

.status-label {
    -fx-text-fill: #999999;
    -fx-font-size: 11px;
}
```

---

## 9. Iconography

### 9.1 Style

- **MANDATORY:** Use `SVGPath` nodes for all icons. Never use Unicode/emoji text symbols as icon substitutes.
- Monochrome, single-color icons. Color applied via `-fx-fill` on the `SVGPath` node, never embedded.
- Match IntelliJ's icon style: simple, recognizable silhouettes at small sizes. No decorative detail.
- Icons must be sharp and readable at 16×16. Avoid overly complex paths.

### 9.2 Why SVGPath over Unicode

Unicode symbols (⌂, ☷, ▄, ⤓) render inconsistently across platforms, look unprofessional, and cannot be color-controlled independently of text. `SVGPath` nodes give pixel-perfect, theme-aware icons that can change color on hover/active states.

### 9.3 Recommended SVG Paths

The following SVG path data provides IntelliJ-style icons for the sidebar sections. All paths are designed for a 16×16 viewport.

| Section    | SVG Path Content (`d` attribute)                                                         |
| ---------- | --------------------------------------------------------------------------------------- |
| Dashboard  | `M3,3 L8,3 L8,9 L3,9 Z M10,3 L13,3 L13,7 L10,7 Z M10,9 L13,9 L13,13 L10,13 Z M3,11 L8,11 L8,13 L3,13 Z` |
| Scrips     | `M4,2 L12,2 L12,14 L4,14 Z M6,5 L10,5 M6,7 L10,7 M6,9 L10,9 M6,11 L9,11`             |
| Bar Data   | `M3,13 L3,5 L5,5 L5,13 Z M7,13 L7,3 L9,3 L9,13 Z M11,13 L11,7 L13,7 L13,13 Z`        |
| Download   | `M8,2 L8,10 M5,7 L8,10 L11,7 M4,12 L12,12 L12,14 L4,14 Z`                             |

### 9.4 Java Implementation

```java
private SVGPath createIcon(String svgContent) {
    SVGPath icon = new SVGPath();
    icon.setContent(svgContent);
    icon.setFill(Color.web("#999999"));
    icon.getStyleClass().add("sidebar-icon");
    return icon;
}
```

### 9.5 Sizes

| Context    | Size  | Usage                            |
| ---------- | ----- | -------------------------------- |
| Sidebar    | 16×16 | Navigation items, prefixed icons |
| Tree node  | 16×16 | Project tree file/folder icons   |
| Toolbar    | 16×16 | Action buttons                   |
| Tab icon   | 13×13 | Editor/tool window tab icons     |

### 9.6 Icon Colors

| State    | Color     | Usage                |
| -------- | --------- | -------------------- |
| Default  | `#999999` | Normal state         |
| Active   | `#ffffff` | On selected/active bg|
| Hovered  | `#bbbbbb` | Hovered controls     |
| Disabled | `#777777` | Disabled controls    |
| Accent   | `#4A78C2` | Active, focused      |
| Error    | `#ff6b68` | Error indicators     |
| Success  | `#6a8759` | Success indicators   |
| Warning  | `#cc7832` | Warning indicators   |

---

## 10. Animations

### 10.1 Philosophy

Minimal to none. IntelliJ does not use animated transitions for most UI actions. When used, keep durations ≤100ms.

> **JavaFX CSS does not support transitions.** Use `Timeline` or `FadeTransition` in Java code only where necessary.

### 10.2 Permitted Animations

| Interaction    | Duration | Implementation                    |
| -------------- | -------- | --------------------------------- |
| Popup appear   | 80ms     | `FadeTransition` from 0 → 1      |
| Popup dismiss  | 60ms     | `FadeTransition` from 1 → 0      |
| Content switch | 100ms    | `FadeTransition` cross-fade       |
| Progress bar   | N/A      | Bound property, no animation      |

### 10.3 Rules

1. **No hover animations** — color changes on hover are instant via CSS pseudo-class.
2. **No layout animations** — never animate width, height, or padding.
3. **No scroll animations** — scroll position changes are instant.
4. **Respect reduced-motion** — disable all transitions when system preference is set.

---

## 11. Accessibility

| Requirement             | Specification                                    |
| ----------------------- | ------------------------------------------------ |
| Text contrast (primary) | ≥ 4.5:1 (WCAG AA) — verified in §2.2            |
| Focus indicator         | 1px bottom underline in `accent` (#4A78C2)       |
| Min hit target          | 24px height for all interactive controls         |
| Keyboard navigation     | All controls reachable via Tab / Shift+Tab       |
| Screen reader labels    | `accessibleText` on all icon-only buttons        |
| Reduced motion          | Disable animations per OS preference             |

---

## 12. Token Reference

JavaFX does not support CSS custom properties. Use a constants class:

```java
public final class ThemeToken {
    public static final String BG_GRADIENT_CENTER = "#2d3548";
    public static final String BG_GRADIENT_EDGE = "#1e1f22";
    public static final String BG_CONTENT = "#1e1f22";
    public static final String BG_CARD = "#2b2d30";
    public static final String BG_INPUT = "#45494a";
    public static final String BG_POPUP = "#46484a";
    public static final String BG_SELECTED = "#2d5c88";
    public static final String ACCENT = "#4A78C2";
    public static final String TEXT_PRIMARY = "#bbbbbb";
    public static final String TEXT_SECONDARY = "#999999";
    public static final String TEXT_DISABLED = "#777777";
    public static final String ICON_DEFAULT = "#999999";
    private ThemeToken() {}
}
```

For theme switching at runtime:

```java
scene.getStylesheets().clear();
scene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());
```
