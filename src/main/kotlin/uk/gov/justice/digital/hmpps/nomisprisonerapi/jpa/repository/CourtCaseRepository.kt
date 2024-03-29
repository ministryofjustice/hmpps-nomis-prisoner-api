package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface CourtCaseRepository : JpaRepository<CourtCase, Long> {
  fun findByOffenderBookingOffenderNomsIdOrderByCreateDatetimeDesc(
    nomsId: String,
  ): List<CourtCase>

  fun findByOffenderBookingOrderByCreateDatetimeDesc(
    offenderBooking: OffenderBooking,
  ): List<CourtCase>

  @Query("select coalesce(max(caseSequence), 0) + 1 from CourtCase where offenderBooking = :offenderBooking")
  fun getNextCaseSequence(offenderBooking: OffenderBooking): Int
}
