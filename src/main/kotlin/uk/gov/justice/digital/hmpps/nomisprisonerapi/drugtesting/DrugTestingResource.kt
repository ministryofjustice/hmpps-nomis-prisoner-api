package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/drug-testing", produces = [MediaType.APPLICATION_JSON_VALUE])
class DrugTestingResource(private val drugTestingService: DrugTestingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
  ): RandomTestingProgramResponse = drugTestingService.findRandomTestingProgramWithPrisoners(rtpId).let { program ->
    RandomTestingProgramResponse(
      rtpId = program.id,
      caseloadId = program.caseloadId,
      rtpDate = program.rtpDate,
      mainPercentage = program.mainPercentage,
      reservePercentage = program.reservePercentage,
      selectionsCount = program.selectionsCount,
      eligibleCount = program.eligibleCount,
      reserveCount = program.reserveCount,
      offenderTestSelection = program.offenderTestSelection.map {
        OffenderTestSelectionResponse(
          offenderBookId = it.id?.offenderBookId ?: 0L,
          testSelectionType = it.testSelectionType,
          testSelectionNo = it.testSelectionNo,
          testedFlag = it.testedFlag?.equals("Y"),
          reasonNotTested = it.reasonNotTested,
          notes = it.notes,
        )
      },
    )
  }
}

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

data class OffenderTestSelectionResponse(
  @Schema(description = "Offender booking id")
  val offenderBookId: Long,
//  @Schema(description = "Prisoner number")
//  val prisonNumber: String,
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
