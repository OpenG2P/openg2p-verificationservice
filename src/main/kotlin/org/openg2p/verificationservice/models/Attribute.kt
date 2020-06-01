package org.openg2p.verificationservice.models

/**
 * Attributes that a person might have.
 * @todo implement data types
 * @todo move this to configuration
 */
enum class Attribute(
    val type: Type = Type.STRING,
    val format: String? = null,
    val  allowedValues: List<String>? = null
) {
    ID,
    PHONE_NUMBER,
    FACIAL_PORTRAIT, // picture of subject. Different from BIO_FACIAL which can be an image or a template usable for matching
    BIO_FINGERPRINT,
    BIO_FACIAL,
    FIRST_NAME,
    LAST_NAME,
    OTHER_NAMES,
    ADDRESS,
    ADDRESS_2,
    ADDRESS_CITY,
    ADDRESS_STATE,
    DATE_OF_BIRTH(type = Type.DATE, format = "yyyy-MM-dd"),
    GENDER(allowedValues = Gender.values().map { it.name });

    enum class Type {
        STRING,
        DATE
    }
}