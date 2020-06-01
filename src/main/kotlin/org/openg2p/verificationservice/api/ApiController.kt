package org.openg2p.verificationservice.api

import org.openg2p.verificationservice.dto.BackendQuery
import org.openg2p.verificationservice.dto.BackendResponse
import org.openg2p.verificationservice.services.VerificationEngine
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import javax.validation.Valid
import javax.validation.ValidationException

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
class ApiController constructor(private val verificationEngine: VerificationEngine) {

    /**
     * Endpoint to ensure service is up and running.
     * @return
     */
    @GetMapping("/health")
    fun healthCheck(): Mono<ResponseEntity<String>> {
        return ResponseEntity.ok("OK").toMono()
    }

    @GetMapping("/backends")
    fun backends(payload: Map<String, Any>): Mono<ResponseEntity<List<String>>> {
        return ResponseEntity.ok(verificationEngine.backends.map { it.name }).toMono()
    }

    /**
     * calls on the verification engine.
     * @return Map of response from each backend in the query keyed by the backend
     */
    @PostMapping("/verify", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun verify(@Valid @RequestBody query: Map<String, BackendQuery>): Mono<ResponseEntity<Map<String, BackendResponse>>> {
        if (verificationEngine.backends.map { it.name }.let {  query.keys - it.toSet() }.isNotEmpty())
            throw ValidationException("Unsupported backends in the query")
        return verificationEngine.verify(query).flatMap { ResponseEntity.ok(it).toMono() }
    }
}
