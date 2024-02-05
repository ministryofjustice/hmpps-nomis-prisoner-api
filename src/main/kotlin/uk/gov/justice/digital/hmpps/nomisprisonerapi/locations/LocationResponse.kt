package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location request returned data")
data class LocationResponse(

  @Schema(description = "The location id", required = true, example = "1234567")
  val locationId: Long,

  @Schema(description = "Whether certified for use", required = false, example = "true", defaultValue = "false")
  val certified: Boolean = false,

  @Schema(description = "Whether a CELL, VISIT room, Kitchen etc (Ref type ILOC_TYPE)", required = true, example = "LAND")
  val locationType: String,

  @Schema(description = "Prison code of the location", required = true, example = "LEI")
  val prisonId: String,

  @Schema(description = "The containing location id", required = true, example = "1234567")
  val parentLocationId: Long? = null,

  @Schema(description = "Max capacity subject to resources", example = "43")
  val operationalCapacity: Int? = null,

  @Schema(description = "Certified Normal Accommodation capacity", example = "44")
  val cnaCapacity: Int? = null,

  @Schema(description = "Description of location", example = "Some description")
  val userDescription: String? = null,

  @Schema(description = "Constructed full code of location", example = "WWI-B-2-004")
  val description: String,

  @Schema(description = "Usually a number for a cell, a letter for a wing or landing. Used to calculate description", required = true, example = "xxxx")
  val locationCode: String,

  @Schema(description = "Physical maximum capacity", example = "45")
  val capacity: Int? = null,

  @Schema(description = "Defines the order within parent location", example = "Joe Bloggs")
  val listSequence: Int? = null,

  @Schema(description = "Comment", example = "Some comment")
  val comment: String? = null,
) {
  fun toAgencyInternalLocation(locationType: InternalLocationType, agency: AgencyLocation, parent: AgencyInternalLocation?): AgencyInternalLocation =
    AgencyInternalLocation(
      active = true,
      certified = certified,
      tracking = true,
      locationType = locationType,
      agency = agency,
      parentLocation = parent,
      currentOccupancy = 0,
      operationalCapacity = operationalCapacity,
      userDescription = userDescription,
      locationCode = locationCode,
      capacity = capacity,
      listSequence = listSequence,
      cnaCapacity = cnaCapacity,
    )
}
