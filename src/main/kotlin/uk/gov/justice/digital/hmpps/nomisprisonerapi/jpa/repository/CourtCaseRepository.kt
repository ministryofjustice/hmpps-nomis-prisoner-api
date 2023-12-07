package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface CourtCaseRepository : JpaRepository<CourtCase, Long> {
  fun findByOffenderBooking_offender_nomsIdOrderByCreateDatetimeDesc(
    nomsId: String,
  ): List<CourtCase>

  fun findByOffenderBookingOrderByCreateDatetimeDesc(
    offenderBooking: OffenderBooking,
  ): List<CourtCase>
}
