package dev.inmo.wishlist.client

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.repo.storeHierarchy
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Persists password-page replacement transitions from the root composition lifetime.
 *
 * @param navigationConfigsRepo Synchronous repository receiving the current root hierarchy after an
 *   accepted replacement has reached the active stack.
 */
internal class PasswordChangeNavigationOwner(
    private val navigationConfigsRepo: NavigationConfigsRepo<ViewConfig>,
) : PasswordChangeViewInteractor {
    /** Credential-free logger used when a root-owned transition cannot be persisted. */
    private val logger = KSLog

    /** Current root-composition binding, or `null` outside a composed root. */
    private var binding: Binding? = null

    /**
     * Connects this owner to one live root composition.
     *
     * @param root Root navigation chain whose hierarchy may be persisted.
     * @param scope Composition-owned scope that outlives replaced child ViewModels.
     * @return Cleanup action that cancels this binding's transitions without clearing a newer binding.
     */
    fun bind(root: NavigationChain<ViewConfig>, scope: CoroutineScope): () -> Unit {
        val newBinding = Binding(root, scope)
        binding?.transitions?.values?.forEach { transition -> transition.cancel() }
        binding = newBinding
        return {
            if (binding === newBinding) {
                newBinding.transitions.values.forEach { transition -> transition.cancel() }
                newBinding.transitions.clear()
                binding = null
            }
        }
    }

    /**
     * Acknowledges a successful password change and schedules its exact-node completion transition.
     *
     * Returning acknowledges root ownership of the handoff; returning does not promise a durable
     * navigation save when the root is disposed, replacement times out, or the repository fails.
     *
     * @param node Pending node whose immutable approval has already been accepted by the server.
     */
    override suspend fun onChanged(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        currentCoroutineContext().ensureActive()
        if (node.config !is PasswordChangeViewConfig.Pending) return
        startTransition(node, PasswordChangeViewConfig.Completed)
    }

    /**
     * Replaces the current password page with the non-empty users-list destination.
     *
     * @param node Current Pending, InvalidApproval, or Completed password-page node.
     */
    override suspend fun onContinue(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        currentCoroutineContext().ensureActive()
        startTransition(node, UsersListViewConfig())
    }

    /** Starts one deduplicated root-owned exact-node replacement and subsequent synchronous save. */
    private fun startTransition(
        node: NavigationNode<PasswordChangeViewConfig, ViewConfig>,
        destination: ViewConfig,
    ) {
        val acceptedBinding = activeBindingFor(node) ?: return
        if (acceptedBinding.transitions.containsKey(node)) return
        val transition = acceptedBinding.scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (binding !== acceptedBinding || activeBindingFor(node) !== acceptedBinding) return@launch
                val replacement = node.chain.replace(node, destination)?.second ?: return@launch
                withTimeoutOrNull(5_000) {
                    node.chain.stackFlow.first { stack ->
                        stack.any { candidate -> candidate === replacement } ||
                            stack.none { candidate -> candidate === node }
                    }
                } ?: return@launch
                if (binding !== acceptedBinding) return@launch
                if (node.chain.rootChain() !== acceptedBinding.root) return@launch
                if (node.chain.stackFlow.value.none { candidate -> candidate === replacement }) return@launch
                navigationConfigsRepo.save(checkNotNull(acceptedBinding.root.storeHierarchy()))
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Throwable) {
                logger.w("Password-change navigation transition failed")
            } finally {
                acceptedBinding.transitions.remove(node)
            }
        }
        acceptedBinding.transitions[node] = transition
        transition.start()
    }

    /** Returns the only binding allowed to replace the supplied current last node. */
    private fun activeBindingFor(
        node: NavigationNode<PasswordChangeViewConfig, ViewConfig>,
    ): Binding? {
        val candidate = binding ?: return null
        if (node.chain.rootChain() !== candidate.root) return null
        if (node.chain.stackFlow.value.lastOrNull() !== node) return null
        return candidate
    }

    /** Root-scoped transition ownership and exact-node deduplication state. */
    private class Binding(
        /** Root chain tied to the active composition. */
        val root: NavigationChain<ViewConfig>,
        /** Scope cancelled by root-composition disposal. */
        val scope: CoroutineScope,
    ) {
        /** Pending root jobs keyed by the exact page node accepted for replacement. */
        val transitions = mutableMapOf<NavigationNode<PasswordChangeViewConfig, ViewConfig>, Job>()
    }
}
