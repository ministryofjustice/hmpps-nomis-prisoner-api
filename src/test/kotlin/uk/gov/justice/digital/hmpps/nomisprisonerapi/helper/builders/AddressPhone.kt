package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class AddressPhoneDslMarker

@NomisDataDslMarker
interface AddressPhoneDsl

@Component
class AddressPhoneBuilderFactory(private val addressPhoneBuilderRepository: AddressPhoneBuilderRepository) {
  fun builder() = AddressPhoneBuilderRepositoryBuilder(addressPhoneBuilderRepository = addressPhoneBuilderRepository)
}

@Component
class AddressPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,

) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
}

class AddressPhoneBuilderRepositoryBuilder(private val addressPhoneBuilderRepository: AddressPhoneBuilderRepository) : AddressPhoneDsl {
  fun build(
    address: PersonAddress,
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
  ): AddressPhone =
    AddressPhone(
      address = address,
      phoneNo = phoneNo,
      phoneType = addressPhoneBuilderRepository.phoneUsageOf(phoneType),
      extNo = extNo,
    )
}
