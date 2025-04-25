package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch.Companion.SERVICE_ALL_PRISONS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitchId

interface ServiceAgencySwitchesRepository : CrudRepository<ServiceAgencySwitch, ServiceAgencySwitchId> {
  /** should only be called internally by checkServicePrison as need to pass in *ALL* too */
  fun existsByIdExternalServiceServiceNameAndIdAgencyLocationIdIn(serviceName: String, agencyLocationId: Collection<String>): Boolean

  fun checkServicePrisonAndAll(serviceName: String, agencyLocationId: String): Boolean = existsByIdExternalServiceServiceNameAndIdAgencyLocationIdIn(serviceName, setOf(agencyLocationId, SERVICE_ALL_PRISONS))
}
