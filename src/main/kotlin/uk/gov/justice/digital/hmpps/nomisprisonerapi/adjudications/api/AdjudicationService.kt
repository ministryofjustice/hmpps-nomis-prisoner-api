package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications.api

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository

@Service
@Transactional
class AdjudicationService(private val adjudicationIncidentRepository: AdjudicationIncidentRepository) {

  fun getAdjudication(eventId: Long): AdjudicationResponse =
    adjudicationIncidentRepository.findByIdOrNull(eventId)?.let {
      return mapAdjudication(it)
    }
      ?: throw NotFoundException("Adjudication not found")

  private fun mapAdjudication(adjudicationIncident: AdjudicationIncident): AdjudicationResponse {
    return AdjudicationResponse(

    )
  }

}

}