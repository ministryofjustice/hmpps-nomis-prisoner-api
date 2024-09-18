package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

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
  private val addressPhoneBuilderRepository: PersonAddressBuilderRepository,

) {
  fun builder() = PersonAddressBuilder(
    addressPhoneBuilderFactory = addressPhoneBuilderFactory,
    addressPhoneBuilderRepository = addressPhoneBuilderRepository,
  )
}

@Component
class PersonAddressBuilderRepository(
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,

) {
  fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) }
}

class PersonAddressBuilder(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val addressPhoneBuilderRepository: PersonAddressBuilderRepository,
) : PersonAddressDsl {

  private lateinit var address: PersonAddress

  fun build(
    type: String?,
    person: Person,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
  ): PersonAddress =
    PersonAddress(
      addressType = addressPhoneBuilderRepository.addressTypeOf(type),
      person = person,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postalCode = postcode,
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
