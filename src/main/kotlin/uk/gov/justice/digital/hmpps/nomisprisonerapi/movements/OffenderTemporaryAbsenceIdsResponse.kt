package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Offender temporary absence ids by booking, including applications and scheduled absences")
data class OffenderTemporaryAbsenceIdsResponse(
  @Schema(description = "List of TAP application IDs")
  val applicationIds: List<Long>,

  @Schema(description = "List of TAP scheduled movement IDs")
  val scheduleIds: List<Long>,

  @Schema(description = "List of TAP scheduled movement OUT IDs")
  val scheduledMovementOutIds: List<OffenderTemporaryAbsenceId>,

  @Schema(description = "List of TAP scheduled movement IN IDs")
  val scheduledMovementInIds: List<OffenderTemporaryAbsenceId>,

  @Schema(description = "List of TAP unscheduled movement OUT IDs")
  val unscheduledMovementOutIds: List<OffenderTemporaryAbsenceId>,

  @Schema(description = "List of TAP unscheduled movement IN IDs")
  val unscheduledMovementInIds: List<OffenderTemporaryAbsenceId>,
)

@Schema(description = "The ID of a single movement")
data class OffenderTemporaryAbsenceId(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val sequence: Int,
)
