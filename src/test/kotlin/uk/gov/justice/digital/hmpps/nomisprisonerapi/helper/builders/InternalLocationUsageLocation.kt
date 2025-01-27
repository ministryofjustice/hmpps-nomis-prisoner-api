package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationUsageLocation

@DslMarker
annotation class InternalLocationUsageLocationDslMarker

@NomisDataDslMarker
interface InternalLocationUsageLocationDsl

@Component
class InternalLocationUsageLocationBuilderFactory {
  fun builder() = InternalLocationUsageLocationBuilder()
}

class InternalLocationUsageLocationBuilder : InternalLocationUsageLocationDsl {

  fun build(
    internalLocationUsage: InternalLocationUsage,
    agencyInternalLocation: AgencyInternalLocation,
    capacity: Int?,
    usageLocationType: InternalLocationType?,
    listSequence: Int?,
    parentUsage: InternalLocationUsageLocation?,
  ): InternalLocationUsageLocation = InternalLocationUsageLocation(
    internalLocationUsage = internalLocationUsage,
    agencyInternalLocation = agencyInternalLocation,
    capacity = capacity,
    usageLocationType = usageLocationType,
    listSequence = listSequence,
    parentUsage = parentUsage,
  ).also {
    agencyInternalLocation.usages.add(it)
  }
}
