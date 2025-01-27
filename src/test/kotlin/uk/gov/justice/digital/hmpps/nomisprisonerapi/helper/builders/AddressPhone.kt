package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Phone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class AddressPhoneDslMarker

@NomisDataDslMarker
interface AddressPhoneDsl

@Component
class AddressPhoneBuilderFactory(private val addressPhoneBuilderRepository: AddressPhoneBuilderRepository) {
  fun builder() = AddressPhoneBuilderRepositoryBuilder(repository = addressPhoneBuilderRepository)
}

@Component
class AddressPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val phoneRepository: PhoneRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
  fun save(phone: AddressPhone): AddressPhone = phoneRepository.saveAndFlush(phone)
  fun updateCreateDatetime(phone: Phone, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update PHONES set CREATE_DATETIME = ? where PHONE_ID = ?", whenCreated, phone.phoneId)
  }
  fun updateCreateUsername(phone: Phone, whoCreated: String) {
    jdbcTemplate.update("update PHONES set CREATE_USER_ID = ? where PHONE_ID = ?", whoCreated, phone.phoneId)
  }
}

class AddressPhoneBuilderRepositoryBuilder(private val repository: AddressPhoneBuilderRepository) : AddressPhoneDsl {
  fun build(
    address: PersonAddress,
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): AddressPhone = AddressPhone(
    address = address,
    phoneNo = phoneNo,
    phoneType = repository.phoneUsageOf(phoneType),
    extNo = extNo,
  ).let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }
  fun build(
    address: OffenderAddress,
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): AddressPhone = AddressPhone(
    address = address,
    phoneNo = phoneNo,
    phoneType = repository.phoneUsageOf(phoneType),
    extNo = extNo,
  ).let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }

  fun build(
    address: CorporateAddress,
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): AddressPhone = AddressPhone(
    address = address,
    phoneNo = phoneNo,
    phoneType = repository.phoneUsageOf(phoneType),
    extNo = extNo,
  ).let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }
}
