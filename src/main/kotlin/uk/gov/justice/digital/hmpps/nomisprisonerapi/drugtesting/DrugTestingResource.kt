package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RequestMapping("/drug-testing", produces = [MediaType.APPLICATION_JSON_VALUE])
class DrugTestingResource(private val drugTestingService: DrugTestingService) {

  @GetMapping("/{rtpId}")
  @Operation(
    summary = "Get a random testing program with its offender selections",
    responses = [
      ApiResponse(responseCode = "200", description = "Random testing program returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Random testing program not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getRandomTestingProgramWithOffenders(
    @Schema(description = "Random testing program id", example = "12345", required = true)
    @PathVariable
    rtpId: Long,
  ): RandomTestingProgramResponse {
    val programRows = drugTestingService.findRandomTestingProgramWithPrisoners(rtpId)
    val program = programRows.first()

    return RandomTestingProgramResponse(
      rtpId = program.rtpId,
      caseloadId = program.caseloadId,
      rtpDate = program.rtpDate,
      mainPercentage = program.mainPercentage,
      reservePercentage = program.reservePercentage,
      selectionsCount = program.selectionsCount,
      eligibleCount = program.eligibleCount,
      reserveCount = program.reserveCount,
      offenderTestSelection = programRows.filter { it.offenderBookId != null }.map {
        OffenderTestSelectionResponse(
          offenderBookId = it.offenderBookId!!,
          prisonNumber = it.prisonNumber!!,
          testSelectionType = it.testSelectionType!!,
          testSelectionNo = it.testSelectionNo!!,
          testedFlag = it.testedFlag?.equals("Y"),
          reasonNotTested = it.reasonNotTested,
          notes = it.notes,
        )
      },
    )
  }

  @GetMapping("/id-ranges")
  @Operation(
    summary = "Get random testing program ids by page size",
    description = "Retrieves a list of random testing program ids, spaced by the page size. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(responseCode = "200", description = "List of ids are returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDrugTestingIdRanges(
    @RequestParam(value = "pageSize", defaultValue = "1000")
    @Parameter(description = "Range size of ids to get")
    @Min(1)
    pageSize: Int,
    @RequestParam(value = "includedPrisonIds", required = false)
    @Parameter(description = "Filter results by included prison ids", example = "['MDI','LEI']")
    includedPrisonIds: Set<String>?,
    @RequestParam(value = "excludedPrisonIds", required = false)
    @Parameter(description = "Filter results by excluded prison ids", example = "['MDI','LEI']")
    excludedPrisonIds: Set<String>?,
  ): List<Long> = drugTestingService.findIdRanges(
    pageSize,
    DrugTestingFilter(
      includedPrisonIds = includedPrisonIds,
      excludedPrisonIds = excludedPrisonIds,
    ),
  )

  @GetMapping("/ids-in-range")
  @Operation(
    summary = "Get every random testing program id in range",
    description = """Returns a list of random testing program ids greater than the specified fromId and less than or equal to the
      specified toId. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.""",
    responses = [
      ApiResponse(responseCode = "200", description = "List of random testing program ids"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDrugTestingIdsInRange(
    @RequestParam(value = "fromId")
    @Schema(description = "Return ids greater than this value.")
    fromId: Long,
    @RequestParam(value = "toId")
    @Schema(description = "Return ids less than or equal to this value.")
    toId: Long,
    @RequestParam(value = "includedPrisonIds", required = false)
    @Parameter(description = "Filter results by included prison ids", example = "['MDI','LEI']")
    includedPrisonIds: Set<String>?,
    @RequestParam(value = "excludedPrisonIds", required = false)
    @Parameter(description = "Filter results by excluded prison ids", example = "['MDI','LEI']")
    excludedPrisonIds: Set<String>?,
  ): List<Long> = drugTestingService.findAllIdsBetweenIds(
    fromId,
    toId,
    DrugTestingFilter(
      includedPrisonIds = includedPrisonIds,
      excludedPrisonIds = excludedPrisonIds,
    ),
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RandomTestingProgramResponse(
  @Schema(description = "Random testing program id", example = "12345")
  val rtpId: Long,
  @Schema(description = "Caseload id", example = "MDI")
  val caseloadId: String,
  @Schema(description = "Random testing program date", example = "2024-01-01")
  val rtpDate: LocalDate,
  @Schema(description = "Main percentage")
  val mainPercentage: Short,
  @Schema(description = "Reserve percentage")
  val reservePercentage: Short,
  @Schema(description = "Selections count")
  val selectionsCount: Int?,
  @Schema(description = "Eligible count")
  val eligibleCount: Int?,
  @Schema(description = "Reserve count")
  val reserveCount: Int?,
  @Schema(description = "List of offender selections for the random testing program")
  val offenderTestSelection: List<OffenderTestSelectionResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderTestSelectionResponse(
  @Schema(description = "Offender booking id")
  val offenderBookId: Long,
  @Schema(description = "Prisoner number")
  val prisonNumber: String,
  @Schema(description = "Test selection type")
  val testSelectionType: String,
  @Schema(description = "Test selection number")
  val testSelectionNo: Int,
  @Schema(description = "Tested flag")
  val testedFlag: Boolean?,
  @Schema(description = "Reason not tested")
  val reasonNotTested: String?,
  @Schema(description = "Notes")
  val notes: String?,
)
