package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Limit
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

  @Suppress("ktlint:standard:function-naming")
  @Query(
    """
    select oa from OffenderAssessment oa
      where oa.id.offenderBooking.offender.nomsId = :offenderNo
        and oa.assessment.isCellSharing = true
        and not (
          (oa.calculatedLevel is null or oa.calculatedLevel = 'PEND') and
          (oa.overrideLevel is null or oa.overrideLevel = 'PEND') and
          (oa.reviewLevel is null or oa.reviewLevel = 'PEND')
        )
    order by oa.assessmentDate desc, oa.id.sequence desc
  """,
  )
  fun findValidCsras(offenderNo: String, limit: Limit): List<OffenderAssessment>
  fun findCurrent(offenderNo: String): OffenderAssessment? = findValidCsras(offenderNo, Limit.of(1)).firstOrNull()
}
