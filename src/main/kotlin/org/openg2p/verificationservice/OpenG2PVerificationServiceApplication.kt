package org.openg2p.verificationservice

import org.openg2p.verificationservice.config.Configurations
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
@EnableConfigurationProperties(Configurations::class)
@EnableWebFlux
@Configuration
class OpenG2PVerificationServiceApplication

fun main(args: Array<String>) {
	runApplication<OpenG2PVerificationServiceApplication>(*args)
}
