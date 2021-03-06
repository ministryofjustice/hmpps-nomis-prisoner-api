package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.util.Optional

@Repository
interface OffenderBookingRepository :
  PagingAndSortingRepository<OffenderBooking, Long>, JpaSpecificationExecutor<OffenderBooking> {

  fun findByOffenderNomsIdAndActive(nomsId: String, active: Boolean): Optional<OffenderBooking>
  fun findByOffenderNomsIdAndBookingSequence(nomsId: String, bookingSequence: Int): Optional<OffenderBooking>
}
