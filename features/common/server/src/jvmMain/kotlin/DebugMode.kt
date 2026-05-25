package project_group.project_name.features.common.server

actual val isInDebugMode
    get() = System.getenv("DEBUG") ?.lowercase() == "true"
