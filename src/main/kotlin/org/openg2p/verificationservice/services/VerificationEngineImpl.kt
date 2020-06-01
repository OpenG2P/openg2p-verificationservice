package org.openg2p.verificationservice.services

import org.openg2p.verificationservice.dto.BackendQuery
import org.openg2p.verificationservice.dto.BackendResponse
import org.openg2p.verificationservice.config.Configurations
import org.openg2p.verificationservice.dto.ResponseDTO
import org.openg2p.verificationservice.models.Attribute
import org.openg2p.verificationservice.services.backends.Backend
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Service
class VerificationEngineImpl(
    override val backends: List<Backend>,
    private val configurations: Configurations
) : VerificationEngine {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info("Loaded ${backends.count()} backends")
    }

    override fun verify(query: Map<String, BackendQuery>): Mono<Map<String, BackendResponse>> {
        return backends.filter { it.name in query.keys }
            .toFlux()
            .parallel()
            .flatMap { backend -> // let's have our backends do the heavy lifting
                val attributes = mutableListOf<Attribute>()
                val backendQuery = query[backend.name]!!

                // let's sanitize our lookup
                val sanitizedLookupMap = backendQuery.lookupAttributes.map {
                    Pair(it.key, sanitizeValue(it.key, it.value))
                }.toMap()

                backendQuery.returnAttributes?.let { attributes.addAll(it) }
                backendQuery.matchAttributes?.let { attributes.addAll(it.keys) }
                backend.lookup(sanitizedLookupMap, attributes, backendQuery.secretTokens)
                    .map {
                        // now let's run our declared comparators and return the first positive match we get
                        val attributesNotReported = attributes.toSet() -  it.kycData?.keys as Set<Attribute>
                        if (attributesNotReported.isNotEmpty())
                            throw Exception("Backend ${backend.name} does not report a values for " +
                                "${attributesNotReported.joinToString(", ")}")
                        processBackendResponse(backendQuery, backend, it)
                    }
            }
            .doOnNext { logger.debug("Lookup Result -  $it") }
            .sequential()
            .collectMap({it.first}, {it.second})
    }

    private fun processBackendResponse(backendQuery: BackendQuery, backend: Backend, res: ResponseDTO): Pair<String, BackendResponse> {
        return if (res.matched) {
            val matchedAttributes: MutableMap<Attribute, Map<String, Any>> = mutableMapOf()
            val returnedAttributes: MutableMap<Attribute, String> = mutableMapOf()
            backendQuery.matchAttributes?.map {
                val theirs = res.kycData!![it.key]!!
                matchedAttributes[it.key] = mapOf(
                    "matched" to compareValue(
                        attribute = it.key,
                        ourValue = sanitizeValue(it.key, it.value),
                        theirValue = sanitizeValue(it.key, theirs)
                    ),
                    "ours" to it.value,
                    "theirs" to theirs
                )
            }
            backendQuery.returnAttributes?.map {
                // now let's run our declared comparators and return the first positive match we get
                returnedAttributes[it] = res.kycData!![it]!!
            }
            BackendResponse(
                lookupMatched = true,
                matchedAttributes = matchedAttributes,
                returnedAttributes = returnedAttributes
            )
        } else {
            BackendResponse(lookupMatched = false)
        }.let { Pair(backend.name, it) }
    }

    /**
     * helper fun that returns sanitized value based on the configuration
     * @todo support types other than string
     */
    private fun sanitizeValue(attribute: Attribute, value: String): String {
        val normalizers = configurations.attributeDefinitionMap[attribute]!!.first // we have already validated the attr key :)
        var cleanedValue = value
        normalizers?.forEach {
            cleanedValue = it.clean(cleanedValue)
        }
        return cleanedValue
    }

    /**
     * helper func to person our comparison.
     * Now is supports only string but we are working on changing that
     * @todo support types other than string
     */
    private fun compareValue(attribute: Attribute, ourValue: String, theirValue: String): Boolean {
        val comparators = configurations.attributeDefinitionMap[attribute]!!.second // we have already validated the attr key :)
        comparators?.forEach {
            if (it.first.compare(ourValue, theirValue) >= it.second) return true
        }
        return false
    }


}
