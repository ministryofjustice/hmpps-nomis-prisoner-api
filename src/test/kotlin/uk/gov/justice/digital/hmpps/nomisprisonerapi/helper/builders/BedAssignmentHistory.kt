package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistoryId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.BedAssignmentHistoryRepository
import java.time.LocalDateTime

@DslMarker
annotation class BedAssignmentHistoryDslMarker

@BedAssignmentHistoryDslMarker
interface BedAssignmentHistoryDsl

@Component
class BedAssignmentHistoryBuilderRepository(
  private val bedAssignmentHistoryRepository: BedAssignmentHistoryRepository,
) {
  fun save(bedAssignmentHistory: BedAssignmentHistory): BedAssignmentHistory = bedAssignmentHistoryRepository.save(bedAssignmentHistory)
}

@Component
class BedAssignmentHistoryBuilderFactory(private val repository: BedAssignmentHistoryBuilderRepository) {
  fun builder() = BedAssignmentHistoryBuilder(repository)
}

class BedAssignmentHistoryBuilder(private val repository: BedAssignmentHistoryBuilderRepository) : BedAssignmentHistoryDsl {
  fun build(
    offenderBooking: OffenderBooking,
    sequence: Int,
    livingUnitId: Long,
    assignmentDateTime: LocalDateTime,
    assignmentReasonCode: String?,
    assignmentEndDateTime: LocalDateTime?,
  ): BedAssignmentHistory = BedAssignmentHistory(
    id = BedAssignmentHistoryId(offenderBooking = offenderBooking, sequence = sequence),
    livingUnitId = livingUnitId,
    assignmentDate = assignmentDateTime.toLocalDate(),
    assignmentTime = assignmentDateTime,
    assignmentReasonCode = assignmentReasonCode,
    assignmentEndDate = assignmentEndDateTime?.toLocalDate(),
    assignmentEndTime = assignmentEndDateTime,
  )
    .let { repository.save(it) }
}
