package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.java.AddOrUpdateAnnotationAttribute

internal fun AddOrUpdateAnnotationAttribute(
    annotationType: String,
    attributeName: String = "value",
    attributeValue: String? = null,
    oldAttributeValue: String? = null,
    addOnly: Boolean = false,
    appendArray: Boolean = false,
) = AddOrUpdateAnnotationAttribute(annotationType, attributeName, attributeValue, oldAttributeValue, addOnly, appendArray)
