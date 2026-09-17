# Feature: UI / Sample

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Reference UI screen. Root navigation node of the client application. Displays sample text fetched from `features/sample` and a live server status indicator driven by `features/common` echo endpoint. Used as the canonical MVVM scaffold reference — do not remove.

## Routes

None. Client-only feature; no server component.

## Models

| Type | Description |
|------|-------------|
| `SampleViewConfig` | Empty `@Serializable class` — root navigation screen identifier |
| `SampleModel` | Interface: `getSampleText(): String`, `serverStatusFlow(): Flow<String?>` |
| `SampleViewModel` | Holds `textState: StateFlow<String>`; `init` collects `serverStatusFlow` and prepends sample text |

## Architecture Notes

- `SampleViewConfig` is pushed as the root node by `ClientPlugin.startPlugin` via `InjectNavigationNode(SampleViewConfig())`. It is always present in the navigation stack; the auth overlay sits on top of it.
- `DefaultSampleModel` implements `SampleModel` in the common UI package, receives `SampleFeature` and `EchoFeature` as private constructor dependencies, and is registered as an interface `single` in `Plugin.kt`. It delegates HTTP text and status polling with the existing 1-second delay.
- `SampleViewModel` has no `SampleViewInteractor` — it requires no side-effecting app-level behavior.
- Platform views: JS uses `org.jetbrains.compose.web.dom.Text`; JVM uses `androidx.compose.material.Text`; Android uses `androidx.compose.material3.Text`.
