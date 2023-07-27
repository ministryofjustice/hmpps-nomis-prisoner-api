package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.OFFENDER_NO_PATTERN
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AdjudicationResource(
  private val adjudicationService: AdjudicationService,
) {
  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/adjudications/charges/ids")
  @Operation(
    summary = "get adjudication charge IDs by filter",
    description = "Retrieves a paged list of adjudication charge ids by filter. Requires ROLE_NOMIS_ADJUDICATIONS.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids are returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_ADJUDICATIONS not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAdjudicationChargeIdsByFilter(
    @PageableDefault(sort = ["whenCreated"], direction = Sort.Direction.ASC, size = 20)
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by adjudication charges that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by adjudication charges that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(
      description = "Filter results by adjudication charges that were created in one of the given prisons",
      example = "MDI",
    )
    prisonIds: List<String>?,
  ): Page<AdjudicationChargeIdResponse> =
    adjudicationService.findAdjudicationChargeIdsByFilter(
      pageRequest = pageRequest,
      AdjudicationFilter(
        toDate = toDate,
        fromDate = fromDate,
        prisonIds = prisonIds ?: listOf(),
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/adjudications/adjudication-number/{adjudicationNumber}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get adjudication by adjudication number",
    description = "Retrieves an adjudication by the adjudication number. Requires ROLE_NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjudication Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AdjudicationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_ADJUDICATIONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjudication does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAdjudication(
    @Schema(description = "Adjudication number", example = "12345", required = true)
    @PathVariable
    adjudicationNumber: Long,
  ): AdjudicationResponse =
    adjudicationService.getAdjudication(adjudicationNumber)

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/adjudications/adjudication-number/{adjudicationNumber}/charge-sequence/{chargeSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get adjudication by adjudication number",
    description = "Retrieves an adjudication charge by the adjudication number and charge sequence. Requires ROLE_NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjudication Charge information returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AdjudicationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_ADJUDICATIONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjudication charge does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAdjudicationCharge(
    @Schema(description = "Adjudication number", example = "12345", required = true)
    @PathVariable
    adjudicationNumber: Long,
    @Schema(description = "Charge sequence", example = "1", required = true)
    @PathVariable
    chargeSequence: Int,
  ): AdjudicationChargeResponse =
    adjudicationService.getAdjudicationCharge(adjudicationNumber, chargeSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @PostMapping("/prisoners/{offenderNo}/adjudications")
  @Operation(
    summary = "creates an adjudication on the latest booking of a prisoner",
    description = "Creates an adjudication. Requires ROLE_NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Adjudication Created Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AdjudicationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_ADJUDICATIONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Adjudication already exists",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createAdjudication(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable("offenderNo")
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @RequestBody @Valid
    request: CreateAdjudicationRequest,
  ): AdjudicationResponse? = adjudicationService.createAdjudication(offenderNo, request)
}

@Schema(description = "adjudication id")
data class AdjudicationChargeIdResponse(
  @Schema(description = "The adjudication number", required = true)
  val adjudicationNumber: Long,
  @Schema(description = "The adjudication charge sequence", required = true)
  val chargeSequence: Int,
  @Schema(description = "The prisoner number", required = true)
  val offenderNo: String,
)
