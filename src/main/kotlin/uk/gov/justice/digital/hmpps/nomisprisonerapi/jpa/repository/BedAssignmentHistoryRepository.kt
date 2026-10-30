package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistoryId

@Repository
interface BedAssignmentHistoryRepository : CrudRepository<BedAssignmentHistory, BedAssignmentHistoryId> {
  @Query("from BedAssignmentHistory b where b.id.offenderBooking.offender.nomsId = :offenderNo order by b.id.offenderBooking.bookingId, b.id.sequence")
  fun findAllForPrisoner(offenderNo: String): List<BedAssignmentHistory>
}
