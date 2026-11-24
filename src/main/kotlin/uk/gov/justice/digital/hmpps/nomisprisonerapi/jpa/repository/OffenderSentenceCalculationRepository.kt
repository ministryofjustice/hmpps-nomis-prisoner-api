package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation

@Repository
interface OffenderSentenceCalculationRepository : JpaRepository<OffenderSentenceCalculation, Long> {
  fun findByOffenderBookingBookingId(bookingId: Long): List<OffenderSentenceCalculation>
}
