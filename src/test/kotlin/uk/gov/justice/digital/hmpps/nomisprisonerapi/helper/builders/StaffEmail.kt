package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternetAddressRepository

@DslMarker
annotation class StaffEmailDslMarker

@NomisDataDslMarker
interface StaffEmailDsl

@Component
class StaffEmailBuilderFactory(val repository: StaffEmailBuilderRepository) {
  fun builder() = StaffEmailBuilder(repository)
}

@Component
class StaffEmailBuilderRepository(
  private val internetAddressRepository: InternetAddressRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(email: StaffInternetAddress): StaffInternetAddress = internetAddressRepository.saveAndFlush(email)
}

class StaffEmailBuilder(val repository: StaffEmailBuilderRepository) : StaffEmailDsl {

  fun build(
    staff: Staff,
    emailAddress: String,
  ): StaffInternetAddress = StaffInternetAddress(
    staff = staff,
    emailAddress = emailAddress,
  ).let { repository.save(it) }
}
