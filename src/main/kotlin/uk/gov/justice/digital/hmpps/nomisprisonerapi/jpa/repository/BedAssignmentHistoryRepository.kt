package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistoryId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface BedAssignmentHistoryRepository : CrudRepository<BedAssignmentHistory, BedAssignmentHistoryId> {
  fun findAllByIdOffenderBookingOrderByIdSequence(offenderBooking: OffenderBooking): List<BedAssignmentHistory>
}
