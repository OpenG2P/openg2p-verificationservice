package org.openg2p.verificationservice.dto

import org.openg2p.verificationservice.api.SupportedBackend
import org.openg2p.verificationservice.api.UniqueLookupAttribute
import org.openg2p.verificationservice.models.Attribute
import javax.validation.constraints.NotEmpty

/**
 * DTO used to verify an identity against a single backend or identity source
 */
data class BackendQuery(

    /**
     * attributes we are sending across to backend. Should ideally be unique within that data source
     * e.g national ID, phone number, fingerprint
     *
     * Map Of: Attribute to Value. Example ID to 12344565 we wish to run or search against a backend
     * Where valid attribute is defined in the configuration
     * @see org.openg2p.verificationservice.config.Configurations.AttributeDefinition
     *
     */
    @NotEmpty
    val lookupAttributes: Map<Attribute, String>,

    /**
     * Check if these attributes match that from the identity source.
     * Uses attribute definition to clean and compare
     * Map of backend to map of attributes to values that should be matched against a single backend
     * @see org.openg2p.verificationservice.config.Configurations.AttributeDefinition
     */
    val matchAttributes: Map<Attribute, String>?,

    /**
     * A list of attributes we wish to return from identity source if source supports it
     * @todo check the backens supports kyc retrieval
     */
    @SupportedBackend
    val returnAttributes: List<Attribute>?,

    /**
     * For identity sources requiring tokens, e.g. fingerprints, or OTPs to verify consent, these can be keyed
     * such that the backend understands it.
     */
    val secretTokens: Map<String, String>?
)