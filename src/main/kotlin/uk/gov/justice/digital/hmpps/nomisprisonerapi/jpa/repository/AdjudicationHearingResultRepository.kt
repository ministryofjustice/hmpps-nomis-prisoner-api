package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge

@Repository
interface AdjudicationHearingResultRepository : JpaRepository<AdjudicationHearingResult, AdjudicationHearingResultId> {
  fun findFirstOrNullByIncidentChargeOrderById_resultSequenceDesc(incidentCharge: AdjudicationIncidentCharge): AdjudicationHearingResult?

  fun findFirstOrNullById_OicHearingIdAndChargeSequence(hearingId: Long, chargeSequence: Int): AdjudicationHearingResult?
}
