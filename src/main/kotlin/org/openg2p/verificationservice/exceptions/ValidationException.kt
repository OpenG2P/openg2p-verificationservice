package org.openg2p.verificationservice.exceptions;

import org.springframework.http.HttpStatus

open class ValidationException(reason: String) : ApiException(HttpStatus.BAD_REQUEST, reason)