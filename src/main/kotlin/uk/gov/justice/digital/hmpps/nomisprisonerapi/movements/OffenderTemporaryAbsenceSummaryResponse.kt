package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Offender temporary absence counts for an offender")
data class OffenderTemporaryAbsenceSummaryResponse(
  @Schema(description = "The application counts")
  val applications: Applications,
  @Schema(description = "The scheduled outbound movement counts")
  val scheduledOutMovements: ScheduledOut,
  @Schema(description = "The actual movement counts")
  val movements: Movements,
)

@Schema(description = "Offender temporary absence application counts")
data class Applications(
  @Schema(description = "The number of applications")
  val count: Long,
)

@Schema(description = "Offender temporary absence scheduled OUT counts")
data class ScheduledOut(
  @Schema(description = "The number of scheduled OUT movements")
  val count: Long,
)

@Schema(description = "Offender temporary absence movement counts")
data class Movements(
  @Schema(description = "The number of actual movements")
  val count: Long,
  @Schema(description = "The number of scheduled movements by direction")
  val scheduled: MovementsByDirection,
  @Schema(description = "The number of unscheduled movements by direction")
  val unscheduled: MovementsByDirection,
)

@Schema(description = "Offender temporary absence scheduled movement counts")
data class MovementsByDirection(
  @Schema(description = "The number of actual OUT movements")
  val outCount: Long,
  @Schema(description = "The number of actual IN movements")
  val inCount: Long,
)
