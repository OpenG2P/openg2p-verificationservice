package org.openg2p.verificationservice.api

import org.openg2p.verificationservice.config.Configurations
import org.openg2p.verificationservice.services.backends.Backend
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class SupportedBackendValidator(private val backends: List<Backend>)
    : ConstraintValidator<UniqueLookupAttribute, Map<String, Any>> {
    override fun isValid(subject: Map<String, Any>?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        return subject?.let {
                i -> backends.map { it.name }.let {  i.keys - it.toSet() }.isEmpty()
        } ?: true
    }
}
