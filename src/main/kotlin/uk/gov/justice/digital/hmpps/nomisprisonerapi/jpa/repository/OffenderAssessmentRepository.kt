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

  @Query(
    """
    select oa from OffenderAssessment oa
      join oa.id.offenderBooking ob
      where ob.offender.nomsId = :offenderNo
        and oa.assessment.isCellSharing = true
      order by oa.assessmentDate asc, ob.bookingSequence desc, oa.id.sequence asc
  """,
  )
  @EntityGraph(type = EntityGraphType.FETCH, value = "offender-csra")
  fun findCsras(offenderNo: String): List<OffenderAssessment>

  @Query(
    """
    select oa from OffenderAssessment oa
      join oa.id.offenderBooking ob
      where ob.offender.nomsId = :offenderNo
        and oa.assessment.isCellSharing = true
        and not (
          (oa.calculatedLevel is null or oa.calculatedLevel = 'PEND') and
          (oa.overrideLevel is null or oa.overrideLevel = 'PEND') and
          (oa.reviewLevel is null or oa.reviewLevel = 'PEND')
        )
      order by oa.assessmentDate desc, ob.bookingSequence asc, oa.id.sequence desc
  """,
  )
  fun findValidCsras(offenderNo: String, limit: Limit): List<OffenderAssessment>
  fun findCurrent(offenderNo: String): OffenderAssessment? = findValidCsras(offenderNo, Limit.of(1)).firstOrNull()
}
