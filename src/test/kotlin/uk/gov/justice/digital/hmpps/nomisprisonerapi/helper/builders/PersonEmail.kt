package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternetAddressRepository
import java.time.LocalDateTime

@DslMarker
annotation class PersonEmailDslMarker

@NomisDataDslMarker
interface PersonEmailDsl

@Component
class PersonEmailBuilderFactory(val repository: PersonEmailBuilderRepository) {
  fun builder() = PersonEmailBuilder(repository)
}

@Component
class PersonEmailBuilderRepository(
  private val internetAddressRepository: InternetAddressRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(email: PersonInternetAddress): PersonInternetAddress = internetAddressRepository.saveAndFlush(email)
  fun updateCreateDatetime(internetAddress: InternetAddress, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_DATETIME = ? where INTERNET_ADDRESS_ID = ?", whenCreated, internetAddress.internetAddressId)
  }
  fun updateCreateUsername(internetAddress: InternetAddress, whoCreated: String) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_USER_ID = ? where INTERNET_ADDRESS_ID = ?", whoCreated, internetAddress.internetAddressId)
  }
}

class PersonEmailBuilder(val repository: PersonEmailBuilderRepository) : PersonEmailDsl {

  fun build(
    person: Person,
    emailAddress: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): PersonInternetAddress = PersonInternetAddress(
    person = person,
    emailAddress = emailAddress,
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
