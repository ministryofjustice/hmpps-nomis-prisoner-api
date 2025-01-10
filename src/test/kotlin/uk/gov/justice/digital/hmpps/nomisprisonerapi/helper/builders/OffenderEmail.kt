package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternetAddressRepository
import java.time.LocalDateTime

@DslMarker
annotation class OffenderEmailDslMarker

@NomisDataDslMarker
interface OffenderEmailDsl

@Component
class OffenderEmailBuilderFactory(val repository: OffenderEmailBuilderRepository) {
  fun builder() = OffenderEmailBuilder(repository)
}

@Component
class OffenderEmailBuilderRepository(
  private val internetAddressRepository: InternetAddressRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(email: OffenderInternetAddress): OffenderInternetAddress = internetAddressRepository.saveAndFlush(email)
  fun updateCreateDatetime(internetAddress: InternetAddress, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_DATETIME = ? where INTERNET_ADDRESS_ID = ?", whenCreated, internetAddress.internetAddressId)
  }
  fun updateCreateUsername(internetAddress: InternetAddress, whoCreated: String) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_USER_ID = ? where INTERNET_ADDRESS_ID = ?", whoCreated, internetAddress.internetAddressId)
  }
}

class OffenderEmailBuilder(val repository: OffenderEmailBuilderRepository) : OffenderEmailDsl {

  fun build(
    offender: Offender,
    emailAddress: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): OffenderInternetAddress =
    OffenderInternetAddress(
      offender = offender,
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
