package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.HousingUnitType

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location creation request")
data class CreateLocationRequest(

  @Schema(description = "Whether certified for use", example = "true", defaultValue = "false")
  val certified: Boolean = false,

  @Schema(
    description = "Whether a CELL, VISIT room, Kitchen etc (Ref type ILOC_TYPE)",
    allowableValues = [
      "ADJU", "ADMI", "APP", "AREA", "ASSO", "BOOT", "BOX", "CELL",
      "CLAS", "EXER", "EXTE", "FAIT", "GROU", "HCEL", "HOLD", "IGRO",
      "INSI", "INTE", "LAND", "LOCA", "MEDI", "MOVE", "OFFI", "OUTS",
      "POSI", "RESI", "ROOM", "RTU", "SHEL", "SPOR", "SPUR", "STOR", "TABL",
      "TRAI", "TRRM", "VIDE", "VISIT", "WING", "WORK",
    ],
    // TODO it would be nice to define these values in one place, possibly something like this:
    // ref = "#/components/schemas/LocationUsageRequest.usageLocationType",
  )
  val locationType: String,

  @Schema(description = "Prison code of the location", example = "LEI")
  val prisonId: String,

  @Schema(description = "The containing location id", example = "1234567")
  val parentLocationId: Long? = null,

  @Schema(description = "Max capacity subject to resources", example = "43")
  val operationalCapacity: Int? = null,

  @Schema(description = "Certified Normal Accommodation capacity", example = "44")
  val cnaCapacity: Int? = null,

  @Schema(description = "Description of location", example = "Some description")
  @field:Size(max = 40, message = "userDescription is too long (max allowed 40 characters)")
  val userDescription: String? = null,

  @Schema(
    description = "Usually a number for a cell, a letter for a wing or landing. Used to calculate description",
    example = "005",
  )
  val locationCode: String,

  @Schema(description = "Full code hierarchy", example = "MDI-C-3-015")
  @field:Size(max = 240, message = "description is too long (max allowed 240 characters)")
  val description: String,

  @Schema(
    description = "Housing Unit type, Reference code (HOU_UN_TYPE)",
    allowableValues = ["HC", "HOLC", "NA", "OU", "REC", "SEG", "SPLC"],
  )
  val unitType: String? = null,

  @Schema(description = "Physical maximum capacity", example = "45")
  val capacity: Int? = null,

  @Schema(description = "Defines the order within parent location", example = "Joe Bloggs")
  val listSequence: Int? = null,

  @Schema(description = "Comment", example = "Some comment")
  @field:Size(max = 240, message = "Comment is too long (max allowed 240 characters)")
  val comment: String? = null,

  @Schema(description = "Whether internal transfers are tracked")
  val tracking: Boolean? = null,

  @Schema(description = "Whether location is activated")
  val active: Boolean? = null,

  @Schema(description = "Profiles")
  val profiles: List<ProfileRequest>? = null,

  @Schema(description = "Usages")
  val usages: List<UsageRequest>? = null,
) {
  fun toAgencyInternalLocation(
    locationType: String,
    housingUnitType: HousingUnitType?,
    agency: AgencyLocation,
    parent: AgencyInternalLocation?,
  ): AgencyInternalLocation = AgencyInternalLocation(
    active = active ?: true,
    certified = certified,
    tracking = tracking ?: true,
    locationCode = locationCode,
    locationType = locationType,
    description = description,
    unitType = housingUnitType,
    agency = agency,
    parentLocation = parent,
    currentOccupancy = 0,
    operationalCapacity = operationalCapacity,
    userDescription = userDescription,
    capacity = capacity,
    listSequence = listSequence,
    cnaCapacity = cnaCapacity,
    comment = comment,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location profile or attribute")
data class ProfileRequest(
  @Schema(
    description = "Reference Domain for the attribute",
    allowableValues = ["HOU_SANI_FIT", "HOU_UNIT_ATT", "HOU_USED_FOR", "SUP_LVL_TYPE", "NON_ASSO_TYP"],
    examples = ["Housing Unit Fittings", "Housing Unit Attribute", "Housing Unit Usage", "Supervision Level"],
  )
  val profileType: String,

  @Schema(description = "Reference Code within the domain for the attribute")
  val profileCode: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location usage")
data class UsageRequest(
  @Schema(
    description = "Types of location that the usage applies to",
    allowableValues = ["APP", "MOVEMENT", "OCCUR", "OIC", "OTHER", "OTH", "PROG", "PROP", "VISIT"],
    examples = [
      "Appointment Location", "Prisoner Movement Location", "Occurrence Location", "Adjudication Hearing Location",
      "Other Internal Location", "Programmes & Activities Location", "Property Location", "Visit Location",
    ],
  )
  val internalLocationUsageType: String,

  val capacity: Int? = null,

  val sequence: Int? = null,
)
