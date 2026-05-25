# Feature: Sample

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Reference/demo feature. Demonstrates the full-stack feature scaffold: server route, client HTTP implementation, shared constants, plugin wiring. Used as a template when creating new features.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/sample/getSampleText` | Bearer | `→ String` | Returns a static sample text string |

## Models

None. No domain models — the feature deals in plain `String`.

## Architecture Notes

- `SampleFeature` interface declared in `server/commonMain` (not `common/commonMain`) because the client has its own copy in `client/commonMain`.
- `SimpleSampleFeatureService` returns a hardcoded string; swap implementation for real logic.
- Route is inside `authenticate {}` — requires a valid bearer token.
- This feature is the canonical reference for new feature scaffolding. Do not remove it.
