package com.example.environment

/** Values supplied by the platform host before the composition root exists. */
expect fun platformAppEnvironmentValues(): Map<String, String>
