package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation

@Repository
interface OffenderSentenceCalculationRepository : JpaRepository<OffenderSentenceCalculation, Long> {
  @Query("select c from OffenderSentenceCalculation c where c.offenderBooking.offender.nomsId = :offenderNo order by c.id")
  fun findAllByOffenderNo(offenderNo: String): List<OffenderSentenceCalculation>
}
