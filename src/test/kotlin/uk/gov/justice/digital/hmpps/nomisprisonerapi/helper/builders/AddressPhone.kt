package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress

@DslMarker
annotation class AddressPhoneDslMarker

@NomisDataDslMarker
interface AddressPhoneDsl

@Component
class AddressPhoneBuilderFactory {
  fun builder() = AddressPhoneBuilderRepositoryBuilder()
}

class AddressPhoneBuilderRepositoryBuilder : AddressPhoneDsl {
  fun build(
    address: PersonAddress,
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
  ): AddressPhone =
    AddressPhone(
      address = address,
      phoneNo = phoneNo,
      phoneType = phoneType,
      extNo = extNo,
    )
}
