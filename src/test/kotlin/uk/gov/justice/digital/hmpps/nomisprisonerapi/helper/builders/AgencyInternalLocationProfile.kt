package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationProfileId

@DslMarker
annotation class AgencyInternalLocationProfileDslMarker

@NomisDataDslMarker
interface AgencyInternalLocationProfileDsl

@Component
class AgencyInternalLocationProfileBuilderFactory() {
  fun builder() = AgencyInternalLocationProfileBuilder()
}

class AgencyInternalLocationProfileBuilder() : AgencyInternalLocationProfileDsl {

  fun build(
    profileType: String,
    profileCode: String,
    agencyInternalLocation: AgencyInternalLocation,
  ): AgencyInternalLocationProfile = AgencyInternalLocationProfile(
    id = AgencyInternalLocationProfileId(
      locationId = agencyInternalLocation.locationId,
      profileType = profileType,
      profileCode = profileCode,
    ),
    agencyInternalLocation = agencyInternalLocation,
  ).also {
    agencyInternalLocation.profiles.add(it)
  }
}
