package ext

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

val Project.libs: VersionCatalog
    get() {
        val catalogs = extensions.getByType(VersionCatalogsExtension::class.java)
        return catalogs.named("libs")
    }