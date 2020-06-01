package org.openg2p.verificationservice.services

import org.openg2p.verificationservice.dto.BackendQuery
import org.openg2p.verificationservice.dto.BackendResponse
import org.openg2p.verificationservice.services.backends.Backend
import reactor.core.publisher.Mono

/**
 * Pretty much calls on backends that actually does the hard work
 */
interface VerificationEngine {

    /**
     * Get a list of active backends
     */
    val backends: List<Backend>

    /**
     * Has active backends do the heavy lifting and return matches y
     * @return a map of query result per backend
     */
    fun verify(query: Map<String, BackendQuery>): Mono<Map<String, BackendResponse>>

}
