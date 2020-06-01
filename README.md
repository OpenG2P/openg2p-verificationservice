# OpenG2P Verification Service

The “OpenG2P” is a set of digital building blocks opensourced to support large scale cash
transfer programs digitize key cogs in their delivery chain: 1) beneficiary targeting
and enrollment, 2) beneficiary list management, 3) payment digitization, and 4)
recourse.

## Background

Large-scale cash transfers have the requirement of verifying the identity of beneficiaries enrolling into their programs.
Current options either skip the verification step or employ manual methods that significantly increases processing time,
is resource intensive and error-prone.

This project lowers the barrier to digitally integrating with identity sources, that is any repository that issues
credentials accepted by a program, e.g. civil registries and mobile network providers for the purposes of:

1) Validating a person (more accurately an ID) is enrolled in that identity source. 
    Examples person with national ID 123445678 enrolled in the civil registry, person fingerprint matches 
    an enrollment in the civil registry, etc
2) Optionally, if supported by the source, comparing information provided to program by person against that curated in the identity source
3) Optionally, if supported by the source, Retrieving certain attributes of person from the identity source, e.g facial portrait for  visual verification. 

> **_NOTE:_**  
> This project is a technical in nature and does nothing on its own. 
> For it to be useful, adopters will have to implement backends that connects with the identity source and performs lookup 

> **_WARNING:_**  
> Do not expose to the internet or untrusted networks in production!
> We are still working on an authentication layer


**A Use Case**

A program as part of their enrollment workflow, via the verification service, checks prospective beneficiary's ID 
or fingerprint against the civil registry to verify identity, accuracy of data provided, and deduplicate against
the civil registry. Program also, via the verification service, asserts that the provided mobile number is registered  
to a Network provider and considered active.

Can be used as a standalone component or integrated with the [OpenG2P ERP](https://github.com/openg2p/erp)

## Getting Started

### Via  Docker Compose

@TODO

### Manually

@TODO

## Concepts

### Attributes

Attributes, `org.openg2p.verificationservice.models.Attribute`, are personal identifiable information about a person.

**The list of attributes supported and can be extended:**

| Attribute       | Description                                                                                                                                                                                                                                                                                                      |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ID              | Translation of this is dependent on the identity source and is usually the unique ID of this person in the identity source. For a civil registry that might be the national ID while for a mobile network operator this can be the person's phone number. It is usually what is sent to the identity source as lookup |
| PHONE_NUMBER    | Phone number of a person. If this phone number is what uniquely identifies the person in the source, e.g. source is a mobile network operator, it is better to pass this in `ID` and for `Backend` implementations to use that attribute for lookup requests                                                                          |
| FACIAL_PORTRAIT | Picture of the person. For visual and not facial recognition purposes. Values of the attributes are passed around base64 encoded                                                                                                                                                                                                                                         |
| BIO_FINGERPRINT | Fingerprint data, image or template, that can be used for biometric analysis. A JSON sting with position and base64 encoding of image or template at that position                                                                                                                                                                                                                                    |
| BIO_FACIAL      | Facial data, image or template, that can be used for biometric analysis. Different from FACIAL_PORTRAIT which is typically your normal picture. A JSON sting with position and base64 encoding of image or template at that position                                                                                                                                                                  |
| FIRST_NAME      | First Name of person                                                                                                                                                                                                                                                                                             |
| LAST_NAME       | Last Name of person                                                                                                                                                                                                                                                                                              |
| OTHER_NAMES     | Others names of person                                                                                                                                                                                                                                                                                           |
| ADDRESS         | First address line                                                                                                                                                                                                                                                                                               |
| ADDRESS_2       | Second address line                                                                                                                                                                                                                                                                                              |
| ADDRESS_CITY    | City                                                                                                                                                                                                                                                                                                             |
| ADDRESS_STATE   | State                                                                                                                                                                                                                                                                                                            |
| DATE_OF_BIRTH   | Date of Birth                                                                                                                                                                                                                                                                                                    |
| GENDER          | Gender                                                                                                                                                                                                                                                                                                           |

Any of these attributes can be used to query an identity source, considering the source supports this, or retrieved from a source,
or used to compare your data against that curated by the source.

### A BackendQuery 
`org.openg2p.verificationservice.dto.BackendQuery` encapsulates question we want the verification service to
attempt to answer about a person through a single backend. 

Request to the verification service are sent a map of the `Backend.name` to `BackendQuery`. Example:

```json
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
     }
  },
  "a_mobile_operator": {
    "lookupAttributes": {
       "ID": "+23277772772"
     }
  }
}
```

The verification engine parses the request and routes these to the individual backends that do the actual lookup.

**The anatomy of a BackendQuery**

1) `BackendQuery.lookupAttributes`

This property is required and is a map of attribute (listed above) to the lookup values of those attributes. This map is 
passed to `Backend` implementation who it turn would pass it in lookup requests to identity sources.

Examples:

Lookup person by their national ID from the civil registry

```json
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
     }
  }
}
```

Lookup person by their phone number from a mobile network operator

```json
{
  "a_mobile_operator": {
    "lookupAttributes": {
       "ID": "1234567889"
     }
  }
}
```

Identity source requires composite key

```json
{
  "a_bank": {
    "lookupAttributes": {
       "ID": "1234567889",
       "DATE_OF_BIRTH": "1990-08-19"
     }
  }
}
```

> **_NOTE:_**  
> This is required

2) `BackendQuery.matchAttributes`

For identity source that allow PII retrievals, we can attempt to ask questions about PII provided to us about a person
against that held by an identity source seen as a source of truth. Example, a program aimed at providing assistance to 
females aged between 20 and 35 might want to verify the age and gender of applicants against the civil registry to assert
eligibility. This is done by pass a map of attributes we wish to verify their known values in `BackendQuery.matchAttributes`.

Examples of above are:


```json
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
    },
    "matchAttributes": {
       "GENDER": "FEMALE",
       "DATE_OF_BIRTH": "1990-09-15"
    }
  }
}
```

Currently, we assume that the ID attribute is trustworthy enough to indicate with a high degree of probability that 
the person with that ID our end is the same as that registered to the ID in the identity source. As such the basic question
being answered by `matchAttributes` is not that of if the persons are the same but rather if the data we hold is similar 
enough to that curated by the identity source for it be regarded as probabilistically the same.

How this works and is configured is covered later.

> **_NOTE:_**  
> This is optional

3) `BackendQuery.returnAttributes`

For identity source that allow PII retrievals, we can attempt to have the verification service return attributes to the
caller e.g. facial portrait of person for visual verification, and address information that caller might not have. 

This is done by passing a map of attributes we wish to returned in `BackendQuery.returnAttributes`.

Example:

```json
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
    },
    "returnAttributes": ["FACIAL_PORTRAIT", "ADDRESS"]
  }
}
```

> **_NOTE:_**  
> This is optional


4) `BackendQuery.secretTokens`

For identity sources requiring tokens, e.g. fingerprints, or OTPs to verify consent of data owner, these can be provided
here. The key and value is totally dependent on the backend's interpretation.

Example: Backend requires the fingerprint of the person

```json
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
    },
    "secretToken": {
        "fingerprint_position_1": "base64encoding....."
    }
  }
}
```

> **_NOTE:_**  
> This is optional


## A BackendResponse

`org.openg2p.verificationservice.dto.BackendResponse` encapsulates the answers to questions asked via the verification 
service of identity sources. Result are sent back to caller as a map of these keyed by the backend.

**The anatomy of a BackendResponse**

1) `BackendResponse.lookupMatched`

Boolean indicating if a match was found in the identity soruce for the `BackendQuery.lookupAttributes` supplied.

Examples:

Lookup person by their national ID from the civil registry

```json
{
  "civil_registry": {
    "lookupMatched": true
  }
}
```

2) `BackendResponse.matchedAttributes`

Result of `BackendQuery.matchAttributes` keyed by each attribute

```json
{
  "civil_registry": {
    "matchedAttributes": {
      "FIRST_NAME": {
          "matched": true,
          "ours": "Salton",
          "theirs": "Salton"
      }
    } 
  }
}
```

In the map of the result for each attribute the following properties represent:

| Property | Description                                                                                                    |
|----------|----------------------------------------------------------------------------------------------------------------|
| matched  | true or false, depending if the value we supplied for that attribute matches that retrieved for that attribute |
| ours     | value we supplied                                                                                              |
| theirs   | value retrieved from identity store                                                                            |


3) `BackendResponse.returnedAttributes`

Values for attributes of a person we asked to return in `BackendQuery.returnAttributes` keyed by each attribute 

```json
{
  "civil_registry": {
    "returnedAttributes": {
      "OTHER_NAMES": "Arthur"
    } 
  }
}
```

## Verifying a Person

Bringing the previous section together, a call to the verification endpoint `http:base_url\verify` might look as such:

```bash
POST /verify
{
  "civil_registry": {
    "lookupAttributes": {
       "ID": "1234567889"
    },
    "matchAttributes": {
       "GENDER": "MALE",
       "FIRST_NAME": "Salton",
       "LAST_NAME": "Massally"
    },
    "returnAttributes": ["FACIAL_PORTRAIT", "ADDRESS", "ADDRESS_2", "ADDRESS_CITY", "ADDRESS_STATE"],
    "secretToken": {
        "fingerprint_position_1": "base64encoding....."
    }
  }
}
```

```bash
RESPONSE: HTTP 200 
{
  "civil_registry": {
    "lookupMatched": true
    "matchedAttributes": {
      "FIRST_NAME": {
          "matched": true,
          "ours": "Salton",
          "theirs": "Salton"
      },
      "LAST_NAME": {
          "matched": true,
          "ours": "Massally",
          "theirs": "Massally"
      }
    },
    "returnedAttributes": {
      "FACIAL_PORTRAIT": "base64encoding.....",
      "ADDRESS": "5 Foday Drive",
      "ADDRESS_2": "Hill Station",   
      "ADDRESS_CITY": "Freetown",
      "ADDRESS_STATE": "Western Urban",
    }  
  }
}
```


## Connecting to an Identity Source

An identity source is any repository that stores identity and demographic information on persons against which programs
wishes to identify, verify, or retrieve KYC/PII information on persons enrolling or enrolled to their programs. The 
Verification Service lowers the barrier of integrating with and performing the aforementioned actions against multiple
identity sources.

Integrations implement `org.openg2p.verificationservice.services.backends.Backend`. See example below:

```kotlin
package org.openg2p.verificationservice.services.backends

import org.openg2p.verificationservice.dto.ResponseDTO
import org.openg2p.verificationservice.models.Attribute
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.text.SimpleDateFormat
import org.springframework.stereotype.Service

@Service
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
```

> **_NOTE:_**  
> For a backend implementation to be picked up by the framework, it should be annotated with `org.springframework.stereotype.Service`


## Configuration

Configurations determine what attributes are supported in API calls by the verification service and how matching occurs.
It also controls sanitation and transformation of values before they are used in lookups and comparisons.

We provide a set of defaults declared via code in `org.openg2p.verificationservice.config.Configurations`, however 
deployments can override this in their `application.yaml` file. 

> **_NOTE:_**  
> You will need to fully redeclare all the attributes you wish to support as it replaces rather than merges with
> the Configuration object `org.openg2p.verificationservice.config.Configurations`

A simple configuration declaring ID, PHONE_NUMBER, FIRST_NAME, and LAST_NAME attributes.

```yaml
verificationservice:
  country_phone_code: 232
  country_phone_length: 8

  attribute_definitions:
    -
      name: ID
      unique: true
      cleaners:
        - no.priv.garshol.duke.cleaners.TrimCleaner
        - no.priv.garshol.duke.cleaners.StripNontextCharacters
        - no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner
      comparators:
        -
          comparator: no.priv.garshol.duke.comparators.ExactComparator
          threshold: 1.0
    -
      name: PHONE_NUMBER
      unique: true
      cleaners:
        - no.priv.garshol.duke.cleaners.TrimCleaner
        - no.priv.garshol.duke.cleaners.StripNontextCharacters
        - no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner
        - no.priv.garshol.duke.cleaners.PhoneNumberCleaner
      comparators:
        -
          comparator: no.priv.garshol.duke.comparators.ExactComparator
          threshold: 1.0
    -
      name: FIRST_NAME
      cleaners:
        - no.priv.garshol.duke.cleaners.TrimCleaner
        - no.priv.garshol.duke.cleaners.StripNontextCharacters
        - no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner
      comparators:
        -
          comparator: no.priv.garshol.duke.comparators.PersonNameComparator
          threshold: 0.8
        -
          comparator: no.priv.garshol.duke.comparators.SoundexComparator
          threshold: 0.9
        -
          comparator: no.priv.garshol.duke.comparators.MetaphoneComparator
          threshold: 0.9
        -
          comparator: no.priv.garshol.duke.comparators.JaroWinkler
          threshold: 0.8
    -
      name: LAST_NAME
      cleaners:
        - no.priv.garshol.duke.cleaners.TrimCleaner
        - no.priv.garshol.duke.cleaners.StripNontextCharacters
        - no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner
      comparators:
        -
          comparator: no.priv.garshol.duke.comparators.PersonNameComparator
          threshold: 0.8
        -
          comparator: no.priv.garshol.duke.comparators.SoundexComparator
          threshold: 0.9
        -
          comparator: no.priv.garshol.duke.comparators.MetaphoneComparator
          threshold: 0.9
        -
          comparator: no.priv.garshol.duke.comparators.JaroWinkler
          threshold: 0.8
```

`verificationservice.country_phone_code` and `verificationservice.country_phone_length` are self-explanatory with the later
being the length of phone numbers without the country code or the leading zero. 

`verificationservice.attribute_definitions` defines what attributes can be used in the API and how they are matched. See
details in the table below:

| Property    | Required | Type    | Description                                                                                                                                                                                                                                                                                                                                                                                                                              |
|-------------|----------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name        | true     | String  | Name of the attribute. Allowed values are the name of enums in `org.openg2p.verificationservice.models.Attribute`. Deployments can support new attributes by adding to that enum and then having an entry for that here                                                                                                                                                                                                                  |
| unique      | false    | Boolean | Value is unique and can be used in lookups. (do we really need this?)                                                                                                                                                                                                                                                                                                                                                                    |
| cleaners    | false    | List    | A list of cleaners that should be applied to the this attributes. The attribute is sanitized with these before sent for lookup (if part of the lookupAttribute) and both the value on this end and that from the identity source are passed through these before compared (if attribute is in matchAttribute). A list of string of fully qualified name of classes implementing `no.priv.garshol.duke.Cleaner`                             |
| comparators | false    | List    | A list of comparators and matching threshold. Comparators are strategies for comparing attributes and return some similarity score between 0.0 and 1.0; thresholds is a double e.g 0.8, at which point we can say it is highly probable that two values are the same. Only one entry from the list of comparator-threshold pair needs to be true for us to denote that an attribute value we supply matches that of the identity source. Comparators must implement `no.priv.garshol.duke.Comparator`   |

We provide a redeclaration of the configuration as yaml in [application.sample.yaml](src/main/resources/application.sample.yaml) to get you started with customization. Rename that to application.yaml and
make your changes. Remember that attributes you define here should be part of the `org.openg2p.verificationservice.models.Attribute` enum.

### Cleaners

Cleaners are sanitizers and transformers that attribute values are passed to sanitize and normalize attribute values before they are:
- used in lookups
- used in comparisons.

A cleaner's job is to make comparison easier by removing from data values all variations that are not likely to indicate genuine differences.
For example, a cleaner might strip everything except digits from a zip code. Or, it might normalize and lowercase addresses. 
Or translate dates into a common format.

A number of cleaners are provided by [Duke](https://github.com/larsga/Duke) , but it's trivial to implement your own by implementing the `no.priv.garshol.duke.Cleaner interfac`e. If you want to use regular expressions to clean data you can subclass the `AbstractRuleBasedCleaner`.

They are based of Lars Marius Garshol in [Duke](https://github.com/larsga/Duke) and a documentation of these can be found
[here](https://github.com/larsga/Duke/wiki/Cleaner).

### Comparators 

A comparator can compare two string values and produce a similarity measure between 0.0 (meaning completely different) 
and 1.0 (meaning exactly equal). These are used because we need something better than simply knowing whether two values are the same or not. 
Also, different kinds of values must be compared differently, and comparison of complex strings like names and addresses 
is a whole discipline in itself.

A number of comparators are provided by [Duke](https://github.com/larsga/Duke), but it's trivial to implement your own by implementing the 'no.priv.garshol.duke.Comparator' interface.

They are based of Lars Marius Garshol in [Duke](https://github.com/larsga/Duke) and a documentation of these can be found
[here](https://github.com/larsga/Duke/wiki/Comparator)

## Development

### Dockerizing
Creating a docker image

```bash
./gradlew clean
./gradlew build
docker build --build-arg JAR_FILE=build/libs/*.jar -t openg2p/verificationservice .
```

### Reference Documentation
For further reference, please consider the following sections:


### Additional Links
These additional references should also help you:
