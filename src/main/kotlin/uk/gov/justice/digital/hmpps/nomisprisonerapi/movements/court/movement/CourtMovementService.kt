package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.movement

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CourtMovementService {

  // TODO implement this method
  fun getCourtMovementOut(offenderNo: String, bookingId: Long, sequence: Int) = CourtMovementOut(
    bookingId = bookingId,
    sequence = sequence,
    courtScheduleOutId = null,
    movementDate = LocalDate.now(),
    movementTime = LocalDateTime.now(),
    movementReason = "COURT",
    fromPrison = "LEI",
    toCourt = "LEEDMC",
    commentText = "Court movement",
    audit = NomisAudit(createUsername = "USER", createDatetime = LocalDateTime.now()),
  )

  // TODO implement this method
  fun getCourtMovementIn(offenderNo: String, bookingId: Long, sequence: Int) = CourtMovementIn(
    bookingId = bookingId,
    sequence = sequence,
    courtScheduleOutId = null,
    movementDate = LocalDate.now(),
    movementTime = LocalDateTime.now(),
    movementReason = "COURT",
    fromCourt = "LEEDMC",
    toPrison = "LEI",
    commentText = "Court movement",
    audit = NomisAudit(createUsername = "USER", createDatetime = LocalDateTime.now()),
  )
}
