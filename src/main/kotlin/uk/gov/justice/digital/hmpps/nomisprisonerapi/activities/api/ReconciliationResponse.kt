package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allocation reconciliation check response")
data class AllocationReconciliationResponse(
  @Schema(description = "The prison we checked the allocations for", example = "BXI")
  @NotNull
  val prisonId: String,
  @Schema(description = "All active bookings and their allocation count", example = "[ { bookingId: 1234567, count: 2 } ]")
  @NotNull
  val bookings: List<BookingCount>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Attendance reconciliation check response")
data class AttendanceReconciliationResponse(
  @Schema(description = "The prison we checked the attendance for", example = "BXI")
  @NotNull
  val prisonId: String,
  @Schema(description = "Date of the attendance check", example = "2021-01-01")
  @NotNull
  val date: LocalDate,
  @Schema(description = "All active bookings and their attendance count", example = "[ { bookingId: 1234567, count: 2 } ]")
  @NotNull
  val bookings: List<BookingCountWithPay>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A count for an offender booking")
data class BookingCount(
  @Schema(description = "The offender booking id", example = "1234567")
  @NotNull
  val bookingId: Long,
  @Schema(description = "The count for the offender booking", example = "2")
  @NotNull
  val count: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A count and sum of pay for an offender booking")
data class BookingCountWithPay(
  @Schema(description = "The offender booking id", example = "1234567")
  @NotNull
  val bookingId: Long,
  @Schema(description = "The count for the offender booking", example = "2")
  @NotNull
  val count: Long,
  @Schema(description = "The total paid for all of the attendances", example = "2.3")
  @NotNull
  val totalPay: BigDecimal,
)
