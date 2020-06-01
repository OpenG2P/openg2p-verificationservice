package org.openg2p.verificationservice.services.backends

import org.openg2p.verificationservice.dto.ResponseDTO
import org.openg2p.verificationservice.models.Attribute
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.text.SimpleDateFormat

class ExampleBackend : Backend {
    override val name = "example"
    override val capabilities = listOf(Backend.Capability.ID_VERIFICATION, Backend.Capability.KYC_LOOKUP)

    /**
     * Let's connect to our identity source and get KYC info
     */
    override fun lookup(
        lookup: Map<Attribute, String>,
        returnAttributes: List<Attribute>?,
        secretToken: Map<String, String>?
    ): Mono<ResponseDTO> {
        // should be wrapped in some reactive calls but left out for illustrative purposes
        // @see https://projectreactor.io/
        val data = someRemoteDataSource.get(lookup[Attribute.ID])
        if (data != null) {
            // let's transform our data into what is expected in our respose
            val kyc = mutableMapOf<Attribute, String>()
            data.keys.forEach {
                when (it) {
                    "firstname" -> kyc[Attribute.FIRST_NAME] = data[it]!!
                    "middlename" -> kyc[Attribute.OTHER_NAMES] = data[it]!!
                    "lastname" -> kyc[Attribute.LAST_NAME] = data[it]!!
                    "birthday" -> {
                        // we need to first transform date to format supported by our side
                        val remoteFormat = SimpleDateFormat("dd/MM/yyyy")
                        val ourFormat = SimpleDateFormat(Attribute.DATE_OF_BIRTH.format)
                        val remoteDate = remoteFormat.parse(data[it]!!)
                        kyc[Attribute.DATE_OF_BIRTH] = ourFormat.format(remoteDate)
                    }
                    "gender" -> kyc[Attribute.GENDER] = data[it]!!.toUpperCase()
                }
            }
            return ResponseDTO(matched = true, kycData = kyc).toMono()
        } else {
            return ResponseDTO(matched = false).toMono()
        }
    }

    private val someRemoteDataSource = mapOf(
        "123456789" to mapOf(
            "firstname" to "Salton",
            "middlename" to "Arthur",
            "lastname" to "Massally",
            "birthday" to "11/04/1911",
            "gender" to "male"
        )
    )
}