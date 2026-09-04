# Task Prompt

Change `Toaster.message` to store `@Composable () -> String`. Preserve the current string-based function without breaking callers by making the existing function push `{ text }` into `_message`.

## Follow-up requirement

Replace the direct composable payload with a data class containing `val message: @Composable () -> String` and a per-notification timeout whose default equals the current timeout. Replace `_message` with a shared flow and make notifications behave as a queue.
