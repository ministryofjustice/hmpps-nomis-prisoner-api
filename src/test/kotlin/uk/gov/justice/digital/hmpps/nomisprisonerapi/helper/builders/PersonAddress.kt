package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress

@DslMarker
annotation class PersonAddressDslMarker

@NomisDataDslMarker
interface PersonAddressDsl {
  @AddressPhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    dsl: AddressPhoneDsl.() -> Unit = {},
  ): AddressPhone
}

@Component
class PersonAddressBuilderFactory(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,

) {
  fun builder() = PersonAddressBuilder(addressPhoneBuilderFactory)
}

class PersonAddressBuilder(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
) : PersonAddressDsl {

  private lateinit var address: PersonAddress

  fun build(
    person: Person,
    premise: String,
    street: String,
    locality: String,
  ): PersonAddress =
    PersonAddress(
      person = person,
      premise = premise,
      street = street,
      locality = locality,
    )
      .also { address = it }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    dsl: AddressPhoneDsl.() -> Unit,
  ): AddressPhone =
    addressPhoneBuilderFactory.builder().let { builder ->
      builder.build(
        address = address,
        phoneType = phoneType,
        extNo = extNo,
        phoneNo = phoneNo,
      )
        .also { address.phones += it }
        .also { builder.apply(dsl) }
    }
}
