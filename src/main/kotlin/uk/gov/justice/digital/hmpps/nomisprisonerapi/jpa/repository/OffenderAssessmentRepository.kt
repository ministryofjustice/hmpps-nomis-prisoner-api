package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface OffenderAssessmentRepository : JpaRepository<OffenderAssessment, OffenderAssessmentId> {
  fun findByIdAndAssessmentTypeId(offenderAssessmentId: OffenderAssessmentId, type: Long): OffenderAssessment?

  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderAssessment where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Int
}
