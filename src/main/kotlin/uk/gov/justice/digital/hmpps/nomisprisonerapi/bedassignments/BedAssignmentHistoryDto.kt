package uk.gov.justice.digital.hmpps.nomisprisonerapi.bedassignments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDateTime

@Schema(description = "A period a prisoner was assigned to a cell (living unit) in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BedAssignmentHistoryResponse(
  @Schema(description = "The booking id", example = "2345678")
  val bookingId: Long,

  @Schema(description = "The sequence of the assignment within the booking", example = "3")
  val sequence: Int,

  @Schema(description = "The NOMIS internal location id of the living unit, returned even when the location no longer exists", example = "-3009")
  val livingUnitId: Long,

  @Schema(description = "The description of the living unit, null when the location no longer exists", example = "BXI-A-1-016")
  val livingUnitDescription: String?,

  @Schema(description = "The prison the living unit belongs to, null when the location no longer exists", example = "BXI")
  val prisonId: String?,

  @Schema(description = "When the prisoner was assigned to the living unit")
  val assignmentDateTime: LocalDateTime,

  @Schema(description = "When the assignment ended, null when it is the current assignment")
  val assignmentEndDateTime: LocalDateTime?,

  @Schema(description = "The reason code (domain CHG_HOUS_RSN), returned even when not in reference data", example = "ADM")
  val assignmentReasonCode: String?,

  @Schema(description = "The reason description, null when the code is not in reference data", example = "Administrative")
  val assignmentReasonDescription: String?,

  @Schema(description = "Audit data associated with the record")
  val audit: NomisAudit,
)

@Schema(description = "The bed assignment history held against a prisoner, across all bookings")
data class PrisonerBedAssignmentHistoryResponse(
  @Schema(description = "Bed assignments ordered by booking id then sequence")
  val bedAssignments: List<BedAssignmentHistoryResponse>,
)
