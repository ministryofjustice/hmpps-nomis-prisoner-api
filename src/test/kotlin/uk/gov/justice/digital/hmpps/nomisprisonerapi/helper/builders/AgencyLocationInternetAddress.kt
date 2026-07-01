package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternetAddressRepository

@DslMarker
annotation class AgencyLocationInternetAddressDslMarker

@AgencyLocationInternetAddressDslMarker
interface AgencyLocationInternetAddressDsl

@Component
class AgencyLocationInternetAddressBuilderFactory(val repository: AgencyLocationInternetAddressBuilderRepository) {
  fun builder() = AgencyLocationInternetAddressBuilder(repository)
}

@Component
class AgencyLocationInternetAddressBuilderRepository(
  private val internetAddressRepository: InternetAddressRepository,
) {
  fun save(email: AgencyLocationInternetAddress): AgencyLocationInternetAddress = internetAddressRepository.saveAndFlush(email)
}

class AgencyLocationInternetAddressBuilder(val repository: AgencyLocationInternetAddressBuilderRepository) : AgencyLocationInternetAddressDsl {

  fun build(
    agencyLocation: AgencyLocation,
    internetAddress: String,
  ): AgencyLocationInternetAddress = AgencyLocationInternetAddress(
    agencyLocation = agencyLocation,
    internetAddress = internetAddress,
    internetAddressClass = "EMAIL",
  ).let { repository.save(it) }
}
