# Pattern: Adding Server Routes

> Read together with the hard rules in `agents/CODING.md`.

Routes are not hardcoded. Feature plugins register `ApplicationRoutingConfigurator.Element` instances into Koin using `singleWithRandomQualifier`. The common server plugin collects all of them via `InternalApplicationRoutingConfigurator`, which wraps every feature route under the global `/api` prefix and installs them into Ktor's routing tree. The web client (static SPA) is served separately at the site root `/`. Register paths RELATIVE (no `/api`) — the prefix is added centrally; adding it yourself would double it to `/api/api/...`.

```kotlin
// In a feature's JVMPlugin.setupDI:
singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
    ApplicationRoutingConfigurator.Element {
        // `this` is Route. Register paths WITHOUT the `/api` prefix — it is added centrally by
        // InternalApplicationRoutingConfigurator, so this is served as `GET /api/my-endpoint`.
        get("/my-endpoint") {
            call.respondText("Hello")
        }
    }
}
```

## Server Configurator Extension Points

Each configurator type has its own `Element` fun interface:

| Configurator | Element interface | What it configures |
|---|---|---|
| `ApplicationRoutingConfigurator` | `Routing.() -> Unit` | HTTP routes |
| `ApplicationAuthenticationConfigurator` | `AuthenticationConfig.() -> Unit` | Auth providers |
| `ApplicationSessionsConfigurator` | (from microutils) | Session types |
| `StatusPagesConfigurator` | (from microutils) | Error handlers |
| `ApplicationCachingHeadersConfigurator` | (from microutils) | Cache headers |

Register elements with `singleWithRandomQualifier<ConfiguratorType.Element>` in `setupDI`.
