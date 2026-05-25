package project_group.project_name.features.common.client.models

import dev.inmo.navigation.core.NavigationNodeFactory

fun interface RootNodeFactoryGetter {
    operator fun invoke(): NavigationNodeFactory<ViewConfig>
}
