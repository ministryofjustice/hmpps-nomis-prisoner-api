package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Phone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class PersonPhoneDslMarker

@NomisDataDslMarker
interface PersonPhoneDsl

@Component
class PersonPhoneBuilderFactory(private val personPhoneBuilderRepository: PersonPhoneBuilderRepository) {
  fun builder() = PersonPhoneBuilder(repository = personPhoneBuilderRepository)
}

@Component
class PersonPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val phoneRepository: PhoneRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
  fun save(phone: PersonPhone): PersonPhone = phoneRepository.saveAndFlush(phone)
  fun updateCreateDatetime(phone: Phone, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update PHONES set CREATE_DATETIME = ? where PHONE_ID = ?", whenCreated, phone.phoneId)
  }
  fun updateCreateUsername(phone: Phone, whoCreated: String) {
    jdbcTemplate.update("update PHONES set CREATE_USER_ID = ? where PHONE_ID = ?", whoCreated, phone.phoneId)
  }
}

class PersonPhoneBuilder(private val repository: PersonPhoneBuilderRepository) : PersonPhoneDsl {

  fun build(
    person: Person,
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): PersonPhone = PersonPhone(
    person = person,
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
