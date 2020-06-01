package org.openg2p.verificationservice.config

import no.priv.garshol.duke.Cleaner
import no.priv.garshol.duke.Comparator
import no.priv.garshol.duke.cleaners.PhoneNumberCleaner
import no.priv.garshol.duke.cleaners.PhoneNumberCleaner.CountryCode
import org.openg2p.verificationservice.models.Attribute
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.Valid
import javax.validation.constraints.NotNull
import kotlin.reflect.full.createInstance

@ConstructorBinding
@ConfigurationProperties(prefix = "verificationservice")
@Validated
data class Configurations(
    /**
     * Country code used to clear phone numbers
     */
    @Valid @NotNull val countryPhoneCode: String = "232",

    /**
     * Length of phone numbers in country without the code and the leading zero
     */
    @Valid @NotNull val countryPhoneLength: Int = 8,

    /**
     * Attributes, default or loaded from the configuration file. Note that if specified in config file
     * you will need to redeclare all the default attributes definition you still want
     */
    @Valid @NotNull val attributeDefinitions: List<AttributeDefinition> = defaultAttributeDefinition()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Stores supported attributes and their definition for rapid access
     * 1) Supported attributes - Map Key
     * 2) How we normalize and compare each attribute. Pair of:
     *      - List of cleaners/normalizers to pass the attribute through before it sent as lookup query or used for
     *        comparing. It also passes the data retrieved from source through this before it pass to comparators
     *      - List of comparators to used to compare PIIs fields. Any that match triggers a match
     * A map of each supported attribute
     */
    val attributeDefinitionMap:
        Map<Attribute, Pair<List<Cleaner>?, List<Pair<Comparator, Double>>?>?>

    /**
     * List of unique attributes in our definition
     */
    val uniqueAttributes: List<Attribute>

    /**
     * Attributes of person supported for querying and matching. Only unique attributes are allowed to query backend
     */
    data class AttributeDefinition(

        /**
         * Will be converted to org.openg2p.verificationservice.models.Attribute. see
         * org.openg2p.verificationservice.models.Attribute for valid values
         *
         */
        @Valid @NotNull val name: String,

        /**
         * Marks this attribute as unique in one of the supported backends.
         * Only unique attributes can be used to query an identity the backend
         */
        val unique: Boolean = false,

        /**
         * Normalizes/cleans attributes strings before query is sent and also before matched
         * Fullyqualified names of the cleaner classes. Must implement no.priv.garshol.duke.Cleaner
         * Only strings are normalized for now! Will skip if value is not a string
         */
        val cleaners: List<String>? = null,

        /**
         * List of map of comparators and their threshold to use in matching.
         * Any of these that is past the threshold for matching with report a match on that field
         *
         */
        val comparators: List<AttributeComparatorThreshold>? = null
    )

    data class AttributeComparatorThreshold(

        /**
         * Fully qualifed name of Comparator
         * Must implement no.priv.garshol.duke.Comparator
         */
        val comparator: String,

        /**
         * Match threshold. Between 0.5 to 1.0. Anything under 0.5 does not make much sense
         */
        val threshold: Double
    )

    init {
        /**
         * @TODO better errors e.g. if Attrubute enum not valid or comparator or cleaner...
         */
        attributeDefinitionMap = attributeDefinitions
            .map { attr ->
                Pair(
                    Attribute.valueOf(attr.name.toUpperCase()),
                    Pair(
                        attr.cleaners?.map { initNormalizer(it, this) },
                        attr.comparators?.map { initComparator(it, this) }
                    )
                )
            }.toMap()

        uniqueAttributes = attributeDefinitions
            .filter { it.unique }
            .map {
                Attribute.valueOf(it.name.toUpperCase())
            }

        if (uniqueAttributes.isNullOrEmpty()) 
            throw Exception("At least one of the declared attributes should be marked as unique")
        logger.info("Initiated attributeProcessorMap with attributes ${attributeDefinitionMap.keys.joinToString(", ")}")
    }
}

private fun initNormalizer(clsName: String, configuration: Configurations): Cleaner {
    val cleaner = if (clsName == "no.priv.garshol.duke.cleaners.PhoneNumberCleaner") {
        val countryDefinition = mapOf(
            configuration.countryPhoneCode to CountryCode(
                configuration.countryPhoneCode, configuration.countryPhoneLength
            )
        )
        PhoneNumberCleaner(countryDefinition)
    } else Class.forName(clsName).kotlin.createInstance()
    return cleaner as Cleaner
}

private fun initComparator(clsNameThreshold: Configurations.AttributeComparatorThreshold, configuration: Configurations)
    : Pair<Comparator, Double> {
    assert(clsNameThreshold.threshold in 0.5..1.0) { "Comparator threshold should be between 0.5 and 1.0 " }
    return Pair(
        Class.forName(clsNameThreshold.comparator).kotlin.createInstance() as Comparator,
        clsNameThreshold.threshold
    )
}

private fun defaultAttributeDefinition(): List<Configurations.AttributeDefinition> {
    return listOf(
        Configurations.AttributeDefinition(
            name = "ID",
            unique = true,
            cleaners = defaultTextCleaners,
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    comparator = "no.priv.garshol.duke.comparators.ExactComparator",
                    threshold = 1.0
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "PHONE_NUMBER",
            unique = true,
            cleaners = defaultTextCleaners + listOf("no.priv.garshol.duke.cleaners.PhoneNumberCleaner"),
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    comparator = "no.priv.garshol.duke.comparators.ExactComparator",
                    threshold = 1.0
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "FACIAL_PORTRAIT"
        ),
        Configurations.AttributeDefinition(
            name = "BIO_FINGERPRINT",
            unique = true
        ),
        Configurations.AttributeDefinition(
            name = "BIO_FACIAL",
            unique = true
        ),
        Configurations.AttributeDefinition(
            name = "FIRST_NAME",
            cleaners = defaultTextCleaners,
            comparators = nameComparators
        ),
        Configurations.AttributeDefinition(
            name = "LAST_NAME",
            cleaners = defaultTextCleaners,
            comparators = nameComparators
        ),
        Configurations.AttributeDefinition(
            name = "OTHER_NAMES"
        ),
        Configurations.AttributeDefinition(
            name = "ADDRESS",
            cleaners = defaultTextCleaners,
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.WeightedLevenshtein",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.QGramComparator",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.JaroWinkler",
                    threshold = 0.8
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "ADDRESS_2",
            cleaners = defaultTextCleaners,
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.JaroWinkler",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.Levenshtein",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.QGramComparator",
                    threshold = 0.8
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "ADDRESS_CITY",
            cleaners = defaultTextCleaners,
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.JaroWinkler",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.Levenshtein",
                    threshold = 0.8
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "ADDRESS_STATE",
            cleaners = listOf(
                "no.priv.garshol.duke.cleaners.TrimCleaner",
                "no.priv.garshol.duke.cleaners.StripNontextCharacters",
                "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
            ),
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.JaroWinkler",
                    threshold = 0.8
                ),
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.Levenshtein",
                    threshold = 0.8
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "DATE_OF_BIRTH",
            cleaners = listOf(
                "no.priv.garshol.duke.cleaners.TrimCleaner"
            ),
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.ExactComparator",
                    threshold = 1.0
                )
            )
        ),
        Configurations.AttributeDefinition(
            name = "GENDER",
            cleaners = listOf(
                "no.priv.garshol.duke.cleaners.TrimCleaner"
            ),
            comparators = listOf(
                Configurations.AttributeComparatorThreshold(
                    "no.priv.garshol.duke.comparators.ExactComparator",
                    threshold = 1.0
                )
            )
        )
    )
}

private val defaultTextCleaners = listOf(
    "no.priv.garshol.duke.cleaners.TrimCleaner",
    "no.priv.garshol.duke.cleaners.StripNontextCharacters",
    "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
)

private val nameComparators = listOf(
    Configurations.AttributeComparatorThreshold(
        "no.priv.garshol.duke.comparators.PersonNameComparator",
        threshold = 0.8
    ),
    Configurations.AttributeComparatorThreshold(
        "no.priv.garshol.duke.comparators.SoundexComparator",
        threshold = 0.9 // phonetics
    ),
    Configurations.AttributeComparatorThreshold(
        "no.priv.garshol.duke.comparators.MetaphoneComparator",
        threshold = 0.9 // phonetics
    ),
    Configurations.AttributeComparatorThreshold(
        "no.priv.garshol.duke.comparators.JaroWinkler",
        threshold = 0.8
    )
)