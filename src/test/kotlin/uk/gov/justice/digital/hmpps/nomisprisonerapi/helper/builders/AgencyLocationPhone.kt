package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class AgencyLocationPhoneDslMarker

@AgencyLocationPhoneDslMarker
interface AgencyLocationPhoneDsl

@Component
class AgencyLocationPhoneBuilderFactory(private val agencyLocationPhoneBuilderRepository: AgencyLocationPhoneBuilderRepository) {
  fun builder() = AgencyLocationPhoneBuilderRepositoryBuilder(repository = agencyLocationPhoneBuilderRepository)
}

@Component
class AgencyLocationPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val phoneRepository: PhoneRepository,
) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
  fun save(phone: AgencyLocationPhone): AgencyLocationPhone = phoneRepository.saveAndFlush(phone)
}

class AgencyLocationPhoneBuilderRepositoryBuilder(private val repository: AgencyLocationPhoneBuilderRepository) : AgencyLocationPhoneDsl {
  fun build(
    agencyLocation: AgencyLocation,
    phoneType: String,
    phoneNo: String,
    extNo: String?,
  ): AgencyLocationPhone = AgencyLocationPhone(
    agencyLocation = agencyLocation,
    phoneNo = phoneNo,
    phoneType = repository.phoneUsageOf(phoneType),
    extNo = extNo,
  ).let { repository.save(it) }
}
