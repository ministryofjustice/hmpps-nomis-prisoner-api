package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository

@DslMarker
annotation class IncidentRequirementDslMarker

@NomisDataDslMarker
interface IncidentRequirementDsl

@Component
class IncidentRequirementBuilderFactory(
  private val repository: IncidentRequirementBuilderRepository,
) {

  fun builder() =
    IncidentRequirementBuilder(repository)
}

@Component
class IncidentRequirementBuilderRepository(
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

class IncidentRequirementBuilder(
  private val repository: IncidentRequirementBuilderRepository,
) :
  IncidentRequirementDsl {
  private lateinit var incidentRequirement: IncidentRequirement

  fun build(
    incident: Incident,
    comment: String?,
    recordingStaff: Staff,
    prisonId: String,
    requirementSequence: Int,
  ): IncidentRequirement = IncidentRequirement(
    id = IncidentRequirementId(incident.id, requirementSequence),
    incident = incident,
    comment = comment,
    recordingStaff = recordingStaff,
    location = repository.lookupAgency(prisonId),

  )
    .also { incidentRequirement = it }
}
