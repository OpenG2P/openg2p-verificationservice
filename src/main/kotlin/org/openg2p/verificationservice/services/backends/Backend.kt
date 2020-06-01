package org.openg2p.verificationservice.services.backends

import org.openg2p.verificationservice.dto.ResponseDTO
import org.openg2p.verificationservice.models.Attribute
import reactor.core.publisher.Mono

/**
 * Interface for the verification source. Queries are routed to this by the engine
 */
interface Backend {

    /**
     * slug-like name of the backend. Will be used in queries
     */
    val  name: String

    /**
     * Query capabilities supported by this backend
     * @see Capability
     */
    val capabilities: List<Capability>

    /**
     * @param lookup: attributes to lookup by and the value
     * @param returnAttributes: attributes backend should return for either matching or returning to client
     */
    fun lookup(lookup: Map<Attribute, String>, returnAttributes: List<Attribute>?, secretToken: Map<String, String>?):
        Mono<ResponseDTO>

    /**
     * Enums representing capabilities that a backend support
     */
    enum class Capability {

        /**
         * Backend supports simple unique ID verification. Example a mobile money operator verifying that the mobile
         * number +23277777777 is valid and registered to their network. It does not return any information or verifies
         * that it is the same person
         */
        ID_VERIFICATION,

        /**
         * Supports retrieval of KYC/PII if it matches the ID. PII can then be used to compare against what was supplied in
         * our query
         */
        KYC_LOOKUP
    }
}
