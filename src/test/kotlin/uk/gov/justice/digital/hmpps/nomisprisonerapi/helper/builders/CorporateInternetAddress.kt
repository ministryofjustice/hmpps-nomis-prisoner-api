package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternetAddressRepository
import java.time.LocalDateTime

@DslMarker
annotation class CorporateInternetAddressDslMarker

@NomisDataDslMarker
interface CorporateInternetAddressDsl {
  companion object {
    const val EMAIL = "EMAIL"
    const val WEB = "WEB"
  }
}

@Component
class CorporateInternetAddressBuilderFactory(val repository: CorporateInternetAddressBuilderRepository) {
  fun builder() = CorporateInternetAddressBuilder(repository)
}

@Component
class CorporateInternetAddressBuilderRepository(
  private val internetAddressRepository: InternetAddressRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(email: CorporateInternetAddress): CorporateInternetAddress = internetAddressRepository.saveAndFlush(email)
  fun updateCreateDatetime(internetAddress: InternetAddress, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_DATETIME = ? where INTERNET_ADDRESS_ID = ?", whenCreated, internetAddress.internetAddressId)
  }
  fun updateCreateUsername(internetAddress: InternetAddress, whoCreated: String) {
    jdbcTemplate.update("update INTERNET_ADDRESSES set CREATE_USER_ID = ? where INTERNET_ADDRESS_ID = ?", whoCreated, internetAddress.internetAddressId)
  }
}

class CorporateInternetAddressBuilder(val repository: CorporateInternetAddressBuilderRepository) : CorporateInternetAddressDsl {

  fun build(
    corporate: Corporate,
    internetAddress: String,
    internetAddressClass: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): CorporateInternetAddress =
    CorporateInternetAddress(
      corporate = corporate,
      internetAddress = internetAddress,
      internetAddressClass = internetAddressClass,
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
