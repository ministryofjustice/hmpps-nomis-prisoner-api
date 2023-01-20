package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class VisitFilter(
  @Schema(description = "List of prison Ids (AKA Agency Ids) to migrate visits from", example = "MDI")
  val prisonIds: List<String>,
  @Schema(
    description = "Visit types to include",
    example = "SCON",
    allowableValues = ["SCON", "OFFI"],
  )
  val visitTypes: List<String>,
  @Schema(
    description = "Only include visits created before this date. NB this is creation date not the actual visit date",
    example = "2020-03-24T12:00:00",
  )
  val toDateTime: LocalDateTime?,
  @Schema(
    description = "Only include visits created after this date. NB this is creation date not the actual visit date",
    example = "2020-03-23T12:00:00",
  )
  val fromDateTime: LocalDateTime?,

  @Schema(
    description = "if true only include visits that are after today",
    example = "true",
  )
  val futureVisits: Boolean? = false,

  @Schema(
    description = "if true exclude erroneous visits ( determined by visit date being more than 1 year in the future )",
    example = "true",
  )
  val excludeExtremeFutureDates: Boolean? = false,

)
