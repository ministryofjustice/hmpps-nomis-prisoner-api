package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitchId

interface ServiceAgencySwitchesRepository : CrudRepository<ServiceAgencySwitch, ServiceAgencySwitchId>
