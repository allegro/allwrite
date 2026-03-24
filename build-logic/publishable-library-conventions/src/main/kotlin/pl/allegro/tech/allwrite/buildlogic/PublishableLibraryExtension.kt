package pl.allegro.tech.allwrite.buildlogic

import org.gradle.api.provider.Property

abstract class PublishableLibraryExtension {
    abstract val name: Property<String>
    abstract val description: Property<String>
}
