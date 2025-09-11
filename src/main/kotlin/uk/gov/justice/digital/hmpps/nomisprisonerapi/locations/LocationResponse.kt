package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location request returned data")
data class LocationResponse(

  @Schema(description = "The location id", example = "1234567")
  val locationId: Long,

  @Schema(description = "Whether certified for use", example = "true", defaultValue = "false")
  val certified: Boolean = false,

  @Schema(description = "Whether a CELL, VISIT room, Kitchen etc (Ref type ILOC_TYPE)", example = "LAND")
  val locationType: String,

  @Schema(description = "Prison code of the location", example = "LEI")
  val prisonId: String,

  @Schema(description = "The containing location id", example = "1234567")
  val parentLocationId: Long? = null,

  @Schema(description = "The containing location id's key (Nomis description field)", example = "WWI-B-2")
  val parentKey: String? = null,

  @Schema(description = "Max capacity subject to resources", example = "43")
  val operationalCapacity: Int? = null,

  @Schema(description = "Certified Normal Accommodation capacity", example = "44")
  val cnaCapacity: Int? = null,

  @Schema(description = "Description of location", example = "Some description")
  val userDescription: String? = null,

  @Schema(description = "Constructed full code of location", example = "WWI-B-2-004")
  val description: String,

  @Schema(
    description = "Usually a number for a cell, a letter for a wing or landing. Used to calculate description",
  )
  val locationCode: String,

  @Schema(description = "Physical maximum capacity", example = "45")
  val capacity: Int? = null,

  @Schema(description = "Defines the order within parent location", example = "Joe Bloggs")
  val listSequence: Int? = null,

  @Schema(description = "Comment", example = "Some comment")
  val comment: String? = null,

  @Schema(
    description = "Housing unit type",
    example = "NA",
    allowableValues = ["HC", "HOLC", "NA", "OU", "REC", "SEG", "SPLC"],
  )
  val unitType: String? = null,

  @Schema(description = "Whether the location is active or has been deactivated", example = "true")
  val active: Boolean,

  @Schema(description = "The deactivation date, defaults to today", example = "2024-12-31")
  val deactivateDate: LocalDate? = null,

  @Schema(
    description = "The reason code for deactivation, reference data 'LIV_UN_RSN'",
    allowableValues = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"],
  )
  val reasonCode: String? = null,

  @Schema(description = "The expected reactivation date if any", example = "2024-12-31")
  val reactivateDate: LocalDate? = null,

  @Schema(description = "Whether internal transfers are tracked")
  val tracking: Boolean,

  @Schema(description = "Profiles")
  val profiles: List<ProfileRequest>? = null,

  @Schema(description = "Usages")
  val usages: List<UsageRequest>? = null,

  @Schema(description = "History")
  val amendments: List<AmendmentResponse>? = null,

  @Schema(description = "Record created date")
  val createDatetime: LocalDateTime,

  @Schema(description = "Record created by")
  val createUsername: String,

  @Schema(description = "Record modified by")
  val modifyUsername: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AmendmentResponse(

  @Schema(description = "Amended timestamp", example = "2024-12-31T23:59:59")
  val amendDateTime: LocalDateTime,

  @Schema(description = "Which value was changed", example = "Sequence")
  val columnName: String? = null,

  @Schema(description = "Original value")
  val oldValue: String? = null,

  @Schema(description = "New value")
  val newValue: String? = null,

  @Schema(description = "Username of the person who made the change", example = "NQP44X")
  val amendedBy: String,
)
