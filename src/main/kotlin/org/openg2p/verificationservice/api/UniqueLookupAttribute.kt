package org.openg2p.verificationservice.api

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates at least one unique attribute is provided in the query
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.VALUE_PARAMETER)
@Constraint(validatedBy = [UniqueLookupAttributeValidator::class])
annotation class UniqueLookupAttribute(
    val message: String = "{verificationservice.validation.UniqueLookupAttribute.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
