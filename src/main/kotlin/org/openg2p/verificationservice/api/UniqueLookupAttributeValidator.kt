package org.openg2p.verificationservice.api

import org.openg2p.verificationservice.config.Configurations
import org.openg2p.verificationservice.models.Attribute
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class UniqueLookupAttributeValidator(private val configurations: Configurations)
    : ConstraintValidator<UniqueLookupAttribute, Map<Attribute, Any>> {
    override fun isValid(lookupAttribute: Map<Attribute, Any>?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        return lookupAttribute?.let { attributes ->
            attributes.keys.firstOrNull { it in configurations.uniqueAttributes } != null
        } ?: true
    }
}
