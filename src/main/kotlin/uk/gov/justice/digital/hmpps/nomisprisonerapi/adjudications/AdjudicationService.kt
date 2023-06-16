package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentPartyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.suspect

@Service
@Transactional
class AdjudicationService(private val adjudicationIncidentPartyRepository: AdjudicationIncidentPartyRepository) {

  fun getAdjudication(adjudicationNumber: Long): AdjudicationResponse =
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.let {
      return mapAdjudication(it)
    }
      ?: throw NotFoundException("Adjudication not found")

  private fun mapAdjudication(adjudication: AdjudicationIncidentParty): AdjudicationResponse {
    return AdjudicationResponse(
      adjudicationNumber = adjudication.adjudicationNumber,
      adjudicationSequence = adjudication.id.partySequence,
      adjudicationIncidentId = adjudication.id.agencyIncidentId,
      offenderNo = adjudication.suspect().offender.nomsId,
      partyAddedDate = adjudication.partyAddedDate,
      comment = adjudication.comment,
      reportingStaffId = adjudication.incident.reportingStaff.id,
      incidentDate = adjudication.incident.incidentDate,
      incidentTime = adjudication.incident.incidentDateTime.toLocalTime(),
      reportedDate = adjudication.incident.reportedDate,
      reportedTime = adjudication.incident.reportedDateTime.toLocalTime(),
      internalLocationId = adjudication.incident.agencyInternalLocation.locationId,
      incidentType = adjudication.incident.incidentType.toCodeDescription(),
      incidentStatus = adjudication.incident.incidentStatus,
      prisonId = adjudication.incident.agencyInternalLocation.agencyId,
    )
  }
}
