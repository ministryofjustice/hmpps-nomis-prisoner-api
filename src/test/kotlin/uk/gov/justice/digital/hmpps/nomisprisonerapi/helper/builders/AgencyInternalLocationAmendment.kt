package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationAmendment
import java.time.LocalDateTime

@DslMarker
annotation class AgencyInternalLocationAmendmentDslMarker

@NomisDataDslMarker
interface AgencyInternalLocationAmendmentDsl

@Component
class AgencyInternalLocationAmendmentBuilderFactory {
  fun builder() = AgencyInternalLocationAmendmentBuilder()
}

class AgencyInternalLocationAmendmentBuilder : AgencyInternalLocationAmendmentDsl {

  fun build(
    agencyInternalLocation: AgencyInternalLocation,
    amendDateTime: LocalDateTime,
    columnName: String?,
    oldValue: String?,
    newValue: String?,
    amendUserId: String,
  ): AgencyInternalLocationAmendment = AgencyInternalLocationAmendment(
    agencyInternalLocation = agencyInternalLocation,
    amendDateTime = amendDateTime,
    columnName = columnName,
    oldValue = oldValue,
    newValue = newValue,
    amendUserId = amendUserId,
  ).also {
    agencyInternalLocation.amendments.add(it)
  }
}
