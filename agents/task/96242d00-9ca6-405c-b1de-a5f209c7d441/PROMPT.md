# Issue #45: Add deeplinks feature

This feature will:

1. Store declared deeplinks uuids + handler info
2. Provide server only feature for creating deeplinks with attached handler info
3. Call handler when deeplink have been called.

Deeplink here is just subpart like `links/<deeplink_uui>`

Handler info is serializable data class, stored as json. In this feature must be declared interface `DeepLinkHandler` with suspend fun `tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean` - this fun must return true when deeplink have been processed (handler knows the type of this handler info and tried to process it)

This feature must not have any code (excluding template one) on client side, during it is planned as server-only feature
