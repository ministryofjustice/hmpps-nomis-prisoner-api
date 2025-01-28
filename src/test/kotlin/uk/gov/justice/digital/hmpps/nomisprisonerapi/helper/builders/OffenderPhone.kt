package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Phone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class OffenderPhoneDslMarker

@NomisDataDslMarker
interface OffenderPhoneDsl

@Component
class OffenderPhoneBuilderFactory(private val offenderPhoneBuilderRepository: OffenderPhoneBuilderRepository) {
  fun builder() = OffenderPhoneBuilder(repository = offenderPhoneBuilderRepository)
}

@Component
class OffenderPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val phoneRepository: PhoneRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
  fun save(phone: OffenderPhone): OffenderPhone = phoneRepository.saveAndFlush(phone)
  fun updateCreateDatetime(phone: Phone, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update PHONES set CREATE_DATETIME = ? where PHONE_ID = ?", whenCreated, phone.phoneId)
  }
  fun updateCreateUsername(phone: Phone, whoCreated: String) {
    jdbcTemplate.update("update PHONES set CREATE_USER_ID = ? where PHONE_ID = ?", whoCreated, phone.phoneId)
  }
}

class OffenderPhoneBuilder(private val repository: OffenderPhoneBuilderRepository) : OffenderPhoneDsl {

  fun build(
    offender: Offender,
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): OffenderPhone = OffenderPhone(
    offender = offender,
    phoneType = repository.phoneUsageOf(phoneType),
    phoneNo = phoneNo,
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
