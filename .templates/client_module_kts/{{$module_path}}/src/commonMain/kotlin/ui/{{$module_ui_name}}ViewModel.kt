package dev.inmo.wishlist.{{$module_package}}.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig

class {{$module_ui_name}}ViewModel(
    private val node: NavigationNode<{{$module_ui_name}}ViewConfig, ViewConfig>,
    private val model: {{$module_ui_name}}Model,
    private val interactor: {{$module_ui_name}}ViewInteractor
) : ViewModel<ViewConfig>(node) {

}
