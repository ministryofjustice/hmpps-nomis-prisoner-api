package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface OffenderAssessmentRepository : JpaRepository<OffenderAssessment, OffenderAssessmentId> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderAssessment where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Int

  @Suppress("ktlint:standard:function-naming")
  @EntityGraph(type = EntityGraphType.FETCH, value = "offender-csra")
  fun findById_OffenderBooking_Offender_NomsIdAndAssessment_AssessmentIdIn(offenderNo: String, assessmentIds: List<Long>): List<OffenderAssessment>
}
