package org.openg2p.verificationservice.dto

import org.openg2p.verificationservice.models.Attribute

/**
 * Individual responses from each backend we are verifying against
 */
data class BackendResponse(

    /**
     *  The lookup attributes was matched/found in the identity source
     */
    val lookupMatched: Boolean,

    /**
     * Attributes matched against that from the identity source.
     */
    val matchedAttributes: Map<Attribute, Map<String, Any>>? = null,

    /**
     * Values of attributes returned from the identity source
     */
    val returnedAttributes: Map<Attribute, String>? = null

)