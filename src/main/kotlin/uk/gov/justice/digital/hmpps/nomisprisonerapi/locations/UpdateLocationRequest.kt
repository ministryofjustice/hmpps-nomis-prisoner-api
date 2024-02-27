package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location update request")
data class UpdateLocationRequest(

  @Schema(
    description = "Whether a CELL, VISIT room, Kitchen etc (Ref type ILOC_TYPE)",
    allowableValues = [
      "ADJU", "ADMI", "APP", "AREA", "ASSO", "BOOT", "BOX", "CELL",
      "CLAS", "EXER", "EXTE", "FAIT", "GROU", "HCEL", "HOLD", "IGRO",
      "INSI", "INTE", "LAND", "LOCA", "MEDI", "MOVE", "OFFI", "OUTS",
      "POSI", "RESI", "ROOM", "RTU", "SHEL", "SPOR", "SPUR", "STOR", "TABL",
      "TRAI", "TRRM", "VIDE", "VISIT", "WING", "WORK",
    ],
  )
  val locationType: String,

  @Schema(description = "Full code hierarchy", example = "MDI-C-3-015")
  @field:Size(max = 240, message = "description is too long (max allowed 240 characters)")
  val description: String,

  @Schema(description = "Description of location", example = "Some description")
  @field:Size(max = 40, message = "userDescription is too long (max allowed 40 characters)")
  val userDescription: String? = null,

  @Schema(
    description = "Usually a number for a cell, a letter for a wing or landing. Used to calculate description",
    example = "005",
  )
  val locationCode: String,

  @Schema(description = "Parent location if any, e.g. landing for a cell", example = "1234567")
  val parentLocationId: Long? = null,

  @Schema(
    description = "Housing Unit type, Reference code (HOU_UN_TYPE)",
    allowableValues = ["HC", "HOLC", "NA", "OU", "REC", "SEG", "SPLC"],
  )
  val unitType: String? = null,

  @Schema(description = "Defines the order within parent location", example = "Joe Bloggs")
  val listSequence: Int? = null,

  @Schema(description = "Comment", example = "Some comment")
  @field:Size(max = 240, message = "Comment is too long (max allowed 240 characters)")
  val comment: String? = null,

  @Schema(description = "Profiles")
  val profiles: List<ProfileRequest>? = null,

  @Schema(description = "Usages")
  val usages: List<UsageRequest>? = null,
)
