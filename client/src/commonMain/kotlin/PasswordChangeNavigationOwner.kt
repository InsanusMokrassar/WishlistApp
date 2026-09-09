package dev.inmo.wishlist.client

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findChainInSubTree
import dev.inmo.navigation.core.extensions.findNodeInSubTree
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.collect
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

    /** Number of currently owned replacement operations; exposed to client lifecycle tests. */
    internal val activeTransitionCount: Int
        get() = binding?.transitions?.size ?: 0

    /**
     * Connects this owner to one live root composition.
     *
     * @param root Root navigation chain whose hierarchy may be persisted.
     * @param scope Composition-owned scope that outlives replaced child ViewModels.
     * @return Cleanup action that cancels this binding's transitions without clearing a newer binding.
     */
    fun bind(root: NavigationChain<ViewConfig>, scope: CoroutineScope): () -> Unit {
        val newBinding = Binding(root, scope)
        binding?.transitions?.values?.toList()?.forEach { transition -> transition.cancel() }
        binding = newBinding
        return {
            newBinding.transitions.values.toList().forEach { transition -> transition.cancel() }
            if (binding === newBinding) {
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
                var replacement: NavigationNode<out ViewConfig, ViewConfig>? = null
                val observedReplacement = CompletableDeferred<Boolean>()
                val acceptedChain = node.chain
                coroutineScope {
                    fun sampleTransition() {
                        when {
                            binding !== acceptedBinding || !acceptedBinding.scopeIsActive() -> {
                                observedReplacement.complete(false)
                            }
                            acceptedBinding.root.findChainInSubTree { candidate -> candidate === acceptedChain } !== acceptedChain -> {
                                observedReplacement.complete(false)
                            }
                            else -> {
                                val expected = replacement
                                when {
                                    expected != null &&
                                        acceptedBinding.root.findNodeInSubTree { candidate -> candidate === expected } === expected &&
                                        acceptedChain.stackFlow.value.any { candidate -> candidate === expected } -> {
                                        observedReplacement.complete(true)
                                    }
                                    acceptedChain.stackFlow.value.none { candidate -> candidate === node } ||
                                        acceptedBinding.root.findNodeInSubTree { candidate -> candidate === node } !== node -> {
                                        observedReplacement.complete(false)
                                    }
                                }
                            }
                        }
                    }

                    val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                        merge(
                            acceptedBinding.root.changesInSubTreeFlow().map { Unit },
                            acceptedChain.stackFlow.map { Unit },
                        ).onStart { emit(Unit) }.collect {
                            sampleTransition()
                        }
                    }
                    try {
                        replacement = acceptedChain.replace(node, destination)?.second ?: return@coroutineScope
                        sampleTransition()
                        if (withTimeoutOrNull(5_000) { observedReplacement.await() } != true) return@coroutineScope
                    } finally {
                        observer.cancelAndJoin()
                    }
                }
                val expectedReplacement = replacement ?: return@launch
                if (binding !== acceptedBinding || !acceptedBinding.scopeIsActive()) return@launch
                if (acceptedBinding.root.findChainInSubTree { candidate -> candidate === acceptedChain } !== acceptedChain) return@launch
                if (acceptedBinding.root.findNodeInSubTree { candidate -> candidate === expectedReplacement } !== expectedReplacement) return@launch
                if (acceptedChain.stackFlow.value.none { candidate -> candidate === expectedReplacement }) return@launch
                navigationConfigsRepo.save(checkNotNull(acceptedBinding.root.storeHierarchy()))
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Throwable) {
                logger.w("Password-change navigation transition failed")
            }
        }
        acceptedBinding.transitions[node] = transition
        transition.invokeOnCompletion {
            if (acceptedBinding.transitions[node] === transition) {
                acceptedBinding.transitions.remove(node)
            }
        }
        transition.start()
    }

    /** Returns the only binding allowed to replace the supplied current last node. */
    private fun activeBindingFor(
        node: NavigationNode<PasswordChangeViewConfig, ViewConfig>,
    ): Binding? {
        val candidate = binding ?: return null
        if (!candidate.scopeIsActive()) return null
        if (candidate.root.findNodeInSubTree { current -> current === node } !== node) return null
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

        /** Reports whether the composition scope can still own a navigation transition. */
        fun scopeIsActive(): Boolean = scope.coroutineContext[Job]?.isActive == true
    }
}
