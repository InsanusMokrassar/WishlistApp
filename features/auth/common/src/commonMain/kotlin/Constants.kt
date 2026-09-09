package dev.inmo.wishlist.features.auth.common

/** Defines stable route segments shared by Auth clients and servers. */
object Constants {
    const val prefixPathPart = "auth"
    const val loginPathPart = "login"
    const val refreshPathPart = "refresh"
    const val logoutPathPart = "logout"
    const val getMePathPart = "getMe"
    const val registerPathPart = "register"
    const val configPathPart = "config"
    const val isRegistrationAvailablePathPart = "is_registration_available"
    /** Auth route segment for requesting an email approval. */
    const val requestPasswordChangeEmailPathPart = "requestPasswordChangeEmail"
    /** Auth route segment for anonymous approval completion. */
    const val completePasswordChangePathPart = "completePasswordChange"
    /** Client route segment for an actionable pending approval. */
    const val passwordChangePagePathPart = "password-change"
    /** Client route segment for credential-free completion. */
    const val passwordChangedPagePathPart = "password-changed"
}
