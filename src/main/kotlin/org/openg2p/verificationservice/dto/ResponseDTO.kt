package org.openg2p.verificationservice.dto

import org.openg2p.verificationservice.models.Attribute

/**
 * Response from backend for each verification request
 */
data class ResponseDTO(

    /**
     * Indicates if a match was found for the lookup attributes
     */
    val matched: Boolean,

    /**
     * KYC information of the match
     */
    val kycData: Map<Attribute, String>? = null

)