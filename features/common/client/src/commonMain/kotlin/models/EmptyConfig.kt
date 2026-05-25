package project_group.project_name.features.common.client.models

import project_group.project_name.features.common.client.models.ViewConfig
import kotlinx.serialization.Serializable

@Serializable
class EmptyConfig : ViewConfig {
    override fun toString(): String {
        return "EmptyConfig"
    }
}
