package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAwardId

@Repository
interface AdjudicationHearingResultAwardRepository : JpaRepository<AdjudicationHearingResultAward, AdjudicationHearingResultAwardId> {
  @Query(value = "SELECT NVL(MAX(SANCTION_SEQ)+1, 1) FROM OFFENDER_OIC_SANCTIONS oos WHERE OFFENDER_BOOK_ID = :offenderBookId", nativeQuery = true)
  fun getNextSanctionSequence(offenderBookId: Long): Int

  fun findFirstOrNullByIncidentPartyAdjudicationNumberAndSanctionCodeAndHearingResultChargeSequence(
    adjudicationNumber: Long,
    sanctionCode: String,
    chargeSequence: Int,
  ): AdjudicationHearingResultAward?

  fun findByIncidentPartyAdjudicationNumberAndHearingResultChargeSequenceOrderByIdSanctionSequence(
    adjudicationNumber: Long,
    chargeSequence: Int,
  ): List<AdjudicationHearingResultAward>

  fun findByIdOffenderBookIdAndSanctionCodeOrderByIdSanctionSequenceAsc(
    offenderBookId: Long,
    sanctionCode: String,
  ): List<AdjudicationHearingResultAward>
}
