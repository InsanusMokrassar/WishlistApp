# Coding Rules

## Pattern Library (load on demand)

Detailed implementation walkthroughs live in `agents/patterns/`. Do NOT read them all — before coding, read ONLY the pattern file(s) matching the task:

| Task | Pattern file |
|---|---|
| New full-stack feature (server + client modules, wiring) | `agents/patterns/full-stack-feature.md` |
| UI screen / MVVM (Model, Interactor, ViewModel, View, Koin registration, localization) | `agents/patterns/mvvm.md` |
| New or changed HTTP routes / server configurators | `agents/patterns/server-routes.md` |
| Persistent CRUD repository (Exposed + cache) | `agents/patterns/crud-repo.md` |
| Authentication (tokens, bearer, login/refresh endpoints) | `agents/patterns/bearer-auth.md` |
| Login/gate screens, auth overlay navigation | `agents/patterns/auth-ui.md` |
| Small per-client persistent state, stored-URL HttpClient plumbing | `agents/patterns/local-storage.md` |

Everything below this section is hard rules — always in force.

---

## Feature README.md

See `ALL.md` for the full rule. Role-specific duty:

### AFTER ANY CODE CHANGE

Update the feature `README.md`:

- New/removed/changed routes (path, method, auth, behavior)
- Changed models or data types
- Changed ownership/auth semantics
- New dependencies between modules

Apply any README delta the Architecture step specified in its step report.

---

After you are done with changes - you must run required compilation task. In most cases, it is `./gradlew :<MODULE_NAME>:build`. If there are errors in output, you must try to fix them. Do only one cycle of fixing; in new cycles do not fix the same (fully same) issues again but add them in report.

---

## Control Flow

NEVER use `else if`. Always use `when` in cases it is suitable for the situation (multi-branch conditionals, value matching, range/type checks). Replace every `if (a) { ... } else if (b) { ... } else { ... }` chain with a `when`:

```kotlin
when {
    loading -> showLoading()
    items.isEmpty() -> showEmpty()
    else -> showList(items)
}
```

A single binary `if` / `else` (no `else if`) stays allowed — the ban is specifically on `else if` chains.

---

## JS Stylesheet Rule

Every JS (`jsMain`) view that needs custom CSS **MUST** define its styles in a dedicated
`object <ViewName>Stylesheet : StyleSheet()` placed in the same `ui/` package as the view class.

- Class name: `<ViewName>Stylesheet` (e.g. `ScaffoldViewStylesheet` for `ScaffoldView`).
- Base class: `org.jetbrains.compose.web.css.StyleSheet`.
- Apply in `onDraw()` with `Style(<ViewName>Stylesheet)` before rendering DOM elements.
- Keep all CSS declarations (flex, grid, sizing, overflow, etc.) inside the stylesheet object — do not use inline `style { }` blocks in DOM elements for anything that belongs to the layout skeleton.

---

## Design System Rule (web — Calm Studio)

The web client's design is the **Calm Studio** language. It is delivered as Kotlin, **never as a `.css`
file**. Five hard rules govern every web design change:

1. **Styles live in a Compose `StyleSheet`, not in CSS.** The single design stylesheet is
   `features/common/client/src/jsMain/kotlin/ui/CalmStudioStyleSheet.kt`
   (`object CalmStudioStyleSheet : StyleSheet(usePrefix = false)`). It self-registers in its `init` block
   with `StyleSheetsAggregator.addStyleSheet(this)`, and is force-loaded once at JS startup via
   `CalmStudioStyleSheet.ensureRegistered()` in `client/.../ClientJSPlugin.startPlugin`.
   - Each class is a **root-level `val name by style { }` delegate**. Because the sheet uses
     `usePrefix = false`, a delegate's property name **is** its emitted class name verbatim (`val btn` →
     `.btn`, `` val `content-inner` `` → `.content-inner`).
   - A class that is styled only *in context* (e.g. `.count` is styled by `.navitem .count`) still gets a
     root **token delegate with an empty body** (`val count by style {}`), declared **before** the
     components that use it, so it can be referenced typed from both the call site and the owning rule.
   - **Compound / state / descendant rules nest inside the base delegate** (the Style DSL "inheritance"):
     inside a `style { }` block `self` is the current selector, `className(count) style { }` is the
     descendant `self .count`, and `self + className(primary)` / `self + hover` build compound/pseudo
     selectors. Element children (`"svg"`, `"h3"`, …) stay raw — they are not classes.
   - Selectors that are **not a single class** — the `:root` `--cs-*` token block, the element reset,
     `::selection`, and the grouped input rule (built with `group(className(input), …)`) — stay raw in
     `init`.
   - **Do NOT create or ship `.css` files for the web client.** There is no `<link rel="stylesheet">`
     to a project stylesheet in `index.html` (only the Manrope web font remains).

2. **No raw class-name strings — reference `CalmStudioStyleSheet.<name>` everywhere.** Both at HTML call
   sites (`classes(CalmStudioStyleSheet.btn)`, never `classes("btn")`) and inside the stylesheet's nested
   rules (`className(count)`, `self + className(on)`).
   - Hyphenated names and the keyword `val` use backticks: `` CalmStudioStyleSheet.`content-inner` ``,
     `` CalmStudioStyleSheet.`reserved-flag` ``, `` CalmStudioStyleSheet.`val` ``.
   - **Exceptions that MUST stay raw strings:** `empty` and `right` — a `val empty` / `val right` clashes
     with the `SelectorsScope.empty` / `:right` members and the compiler demands an impossible `override`,
     so they stay `classes("empty")` / `classes("right")` and raw `".empty…"` / `".right"` rules. Dead
     Bootstrap utility classes still on a couple of avatar views (`rounded`, `rounded-circle`,
     `flex-shrink-0`) are not Calm Studio classes and keep their raw strings.

3. **Every design component is a `@Composable`.** For each component in the Calm Studio design
   (`local.new_design/ui_kits/calm-studio/`), there is a matching `@Composable` in
   `features/common/client/src/jsMain/kotlin/ui/components/`. Views compose screens from these
   components instead of hand-writing the design's DOM/classes inline.

   - Existing components: buttons (`CalmButton`, `IconButton`), data display (`ItemCard`, `ItemRow`,
     `ListCard`, `NewListCard`, `PersonCard`, `CalmPill`, `PriorityPill`, `CardBadge`, `ReservedFlag`,
     and the `ItemGrid`/`RowsList`/`ListCardsGrid`/`PeopleGrid` containers), layout/nav (`ContentColumn`,
     `PageHead`, `Subline`, `Breadcrumb`, `Toolbar`, `SegmentedControl`), forms (`CalmForm`, `FieldSet`,
     `CalmTextField`, `CalmTextArea`, `FormRow`, `FormHint`, `PriorityOptions`), detail (`DetailLayout`,
     `DetailMedia`, `DetailField`, `PriceTag`, `LinkRow`, `ActionBar`), feedback (`EmptyState`,
     `CalmModal` + `ModalHeader`/`ModalBody`/`ModalFooter`/`ModalTabs`, `ConfirmModal`), toast
     (`ToastHost`/`Toaster`), and icons (`CalmIcon`/`CalmIcons`).
   - When the design gains or changes a component: add/update its `@Composable` here (mirroring the
     reference markup and class names) and the corresponding rules in `CalmStudioStyleSheet`.
   - Components take already-translated strings and primitive props / slots; they must not depend on
     feature domain types, so they stay reusable across every UI feature.

4. **Standard components everywhere possible — raw DOM / custom composables are the exception.** A view
   MUST compose from the standard Calm Studio `@Composable` components (rule 3) wherever one fits.
   Hand-written DOM with `classes(CalmStudioStyleSheet.<name>)`, or a new per-view/custom composable, is
   permitted **only** when the element needs customization an existing component cannot express, and the
   raw/custom code is then the minimum needed to achieve that result.
   - "Cannot express" means the component lacks the required capability or prop, e.g.: a real `<img>`
     where the component only renders a tint (`ItemCard`/`ItemRow`/`PersonCard` media), a custom non-string
     header (`PageHead` takes a `String` title), a number / read-only input (`CalmTextField` is
     string-only and always enabled-or-disabled), a native `.select`, or a control needing an extra
     positioning class `CalmButton`/etc. do not accept. App-shell chrome (`.sidebar`, `.topbar`, `.crumb`,
     `.navitem`, …) and pure layout utilities (`.formactions`, `.hstack`, `.sectionhead`) have no
     component and stay raw.
   - When you fall back to raw/custom, keep it local and say **why** (one line) so the exception is
     auditable. Do not hand-roll something a component already covers for convenience.

5. **Components MUST NOT be modified without a direct request to do so.** The shared composables in
   `features/common/client/src/jsMain/kotlin/ui/components/` (and their `CalmStudioStyleSheet` rules) are
   frozen unless the operator explicitly asks to change a component. If a view needs a capability a
   component lacks, do **not** edit the component to add it — either fall back to the raw/custom exception
   (rule 4) or stop and ask the operator to extend the component. Adding/altering a component's props,
   markup, or styling on your own initiative is the error.

---

## KDoc Requirements

Priority of selecting model for KDocs fills: `haiku` / `sonnet` / `opus`

**ALL created `.kt` files MUST contain valid KDocs.**

Rules:
- Every `class`, `interface`, `object`, `fun`, `val`/`var` at class/interface level must have a KDoc comment.
- KDocs must describe purpose, not restate the name.
- When updating existing code that has KDocs — update the KDocs to match.
- Constructor parameters documented via `@param` tags.
- Return values documented via `@return` tag when non-obvious.
- No placeholder or empty KDoc blocks (`/** */`).

---

## Value-Class Property Naming

In an inline value class that wraps a single value, the wrapped property MUST be named as its type name, lowercased — `String` → `string`, `Long` → `long`, `Int` → `int`. Do not use ad-hoc names like `raw`, `value`, or `content`. Examples: `value class ItemName(val string: String)`, `value class ItemId(val long: Long)`.

---

## Plugin Composition Pattern

Each feature has platform-specific plugin objects that delegate to shared ones:

```kotlin
// Pattern used in every platform plugin (JVM, JS, Android)
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ParentPlugin) { setupDI(config) }   // delegate to parent
        with(Plugin) { setupDI(config) }          // delegate to common
        // ... platform-specific registrations
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        ParentPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### Plugin Registration

- **Server**: plugin FQCNs listed in `config.json` `"plugins"` array, loaded by reflection.
- **Client (JS/JVM/Android)**: plugin instances hardcoded in `Main.kt` / `MainActivity.kt`.

---

## Plugins (`StartPlugin` inheritors) note

You MUST NOT add new `setupDI` and `startPlugin` methods calls in plugins for the other plugins outside of feature. It is
permitted to call `setupDI` and `startPlugin` methods only within the same feature and only to the plugin with greater commonized meaning.
For example, `JVMPlugin` can't call `JSPlugin.setupDI`, but must call `Plugin.setupDI`.

---

## Koin Named Qualifiers

When a Koin `named("...")` qualifier is used more than once within a plugin (e.g. registering a dependency under a qualifier and resolving it elsewhere with `get(qualifier = ...)`), extract the qualifier into a single `private val` on the plugin object instead of repeating the inline `named("...")` literal. This keeps the qualifier string defined once and prevents drift between registration and resolution sites.

```kotlin
object Plugin : StartPlugin {
    private val myDependencyQualifier = named("myDependency")

    override fun Module.setupDI(config: JsonObject) {
        single(qualifier = myDependencyQualifier) { /* ... */ }
        single { Consumer(dep = get(qualifier = myDependencyQualifier)) }
    }
}
```

### Typed definition & accessor helpers

When a qualified dependency is registered in one plugin and resolved at other call sites, do not repeat raw `single<T>(qualifier = ...)` / `get<T>(qualifier = ...)` calls. Instead, define typed helpers in a dedicated file (one per logical dependency) so the qualifier, the type, and the resolution rule are declared exactly once:

- one `Module.singleXxx(...)` registration extension wrapping `single(qualifier, createdAtStart, definition)`;
- typed read accessors `Koin.xxx` and `Scope.xxx` wrapping `get(qualifier = ...)` — `Scope` accessor lets other `single { }`/`factory { }` blocks resolve the dependency without naming the qualifier.

Visibility mirrors the dependency's public surface: expose the read-only/public type with public helpers; keep any mutable backing dependency `private`/`internal` (private qualifier + `internal` helpers and accessors).

```kotlin
// MyThing.kt — declares qualifier, registration helper, and accessors once
val myThingQualifier: StringQualifier = named("myThing")

internal fun Module.singleMyThing(
    createdAtStart: Boolean = false,
    definition: Definition<MyThing>,
): KoinDefinition<MyThing> = single(myThingQualifier, createdAtStart, definition)

val Koin.myThing: MyThing
    get() = get(qualifier = myThingQualifier)

val Scope.myThing: MyThing
    get() = get(qualifier = myThingQualifier)
```

```kotlin
// Plugin.kt — registration and resolution stay free of raw qualifier/type literals
override fun Module.setupDI(config: JsonObject) {
    singleMyThing { MyThing(/* ... */) }
    single { Consumer(dep = myThing) } // Scope.myThing accessor
}
```

For a dependency with a public read-only view backed by a private mutable instance, declare two pairs of helpers in the same file: public `singleXxx` + `Koin.xxx`/`Scope.xxx` for the read-only type, and `internal` `singleSecretXxxMutable` + `internal` `Koin.secretXxxMutable`/`Scope.secretXxxMutable` (with a `private` qualifier) for the mutable backing instance.

---

## Ktor Client Realization Rule

`KtorXxxFeature` classes must **only** call HTTP endpoints and return the result. They must not implement any additional logic — no storage writes, no caching, no business rules. If such logic is needed, wrap `KtorXxxFeature` in a service class (e.g. `MyFeatureService`) that holds the storage and delegates HTTP calls to the Ktor realization. Register the service as the `MyFeature` binding in Koin, not the Ktor class directly.

---

## Exposed repositories notes

> **IMPORTANT**: Each exposed database must contains `init` block with calling of `initTable()` (package `dev.inmo.micro_utils.repos.exposed`) to init table
