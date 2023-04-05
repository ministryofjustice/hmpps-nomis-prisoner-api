package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class IncentivesResource(private val incentivesService: IncentivesService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @PostMapping("/prisoners/booking-id/{bookingId}/incentives")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new incentive",
    description = "Creates a new incentive using next sequence no.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateIncentiveRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Incentive information with created sequence",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison or iep value do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        responseCode = "404",
        description = "booking does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createIncentive(
    @Schema(description = "Offender Booking Id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
    @RequestBody @Valid
    createIncentiveRequest: CreateIncentiveRequest,
  ): CreateIncentiveResponse =
    incentivesService.createIncentive(bookingId, createIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/ids")
  @Operation(
    summary = "get incentives (a.k.a IEP) by filter",
    description = "Retrieves a paged list of incentive composite ids by filter. Requires ROLE_NOMIS_INCENTIVES.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of composite ids are returned",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIncentivesByFilter(
    @PageableDefault(sort = ["whenCreated", "id.offenderBooking", "id.sequence"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by incentives that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by incentives that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
    @RequestParam(value = "latestOnly", required = false)
    @Parameter(
      description = "if true only retrieve latest incentive for each prisoner",
      example = "true",
    )
    latestOnly: Boolean? = false,
  ): Page<IncentiveIdResponse> =
    incentivesService.findIncentiveIdsByFilter(
      pageRequest = pageRequest,
      IncentiveFilter(
        toDate = toDate,
        fromDate = fromDate,
        latestOnly = latestOnly ?: false,
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/booking-id/{bookingId}/incentive-sequence/{incentiveSequence}")
  @Operation(
    summary = "get a prisoner's incentive level (a.k.a IEP) by id (bookingId and incentiveId)",
    description = "Retrieves a created incentive level for a prisoner. Requires ROLE_NOMIS_INCENTIVES.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the incentive level details",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIncentive(
    @Schema(description = "NOMIS booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "NOMIS Incentive sequence ", example = "1", required = true)
    @PathVariable
    incentiveSequence: Long,
  ): IncentiveResponse =
    incentivesService.getIncentive(
      bookingId = bookingId,
      incentiveSequence = incentiveSequence,
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/booking-id/{bookingId}/current")
  @Operation(
    summary = "get a prisoner's current incentive level (a.k.a IEP) for a booking",
    description = "Retrieves the current incentive level (by booking) for a prisoner. Requires ROLE_NOMIS_INCENTIVES.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the incentive level details",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCurrentIncentive(
    @Schema(description = "NOMIS booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
  ): IncentiveResponse =
    incentivesService.getCurrentIncentive(
      bookingId = bookingId,
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @PostMapping("/incentives/reference-codes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new global incentive level",
    description = "Creates a new global incentive level",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateIncentiveRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Global Incentive level",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createGlobalIncentiveLevel(
    @RequestBody @Valid
    createIncentiveRequest: ReferenceCode,
  ): ReferenceCode =
    incentivesService.createGlobalIncentiveLevel(createIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @PutMapping("/incentives/reference-codes/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates an existing global incentive level",
    description = "Updates an existing global incentive level, updateable fields are description and active",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateIncentiveRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Updated Global Incentive level",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Global incentive level not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateGlobalIncentiveLevel(
    @Schema(description = "Incentive reference code", example = "STD", required = true)
    @PathVariable
    code: String,
    @RequestBody @Valid
    updateIncentiveRequest: UpdateGlobalIncentiveRequest,
  ): ReferenceCode =
    incentivesService.updateGlobalIncentiveLevel(code, updateIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/reference-codes/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets the global incentive level by code",
    description = "Gets a global incentive level by provided code and domain of IEP_LEVEL",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "return the Global Incentive level",
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
        responseCode = "404",
        description = "Global Incentive Level does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getGlobalIncentiveLevel(
    @Schema(description = "Incentive reference code", example = "STD", required = true)
    @PathVariable
    code: String,
  ): IEPLevel =
    incentivesService.getGlobalIncentiveLevel(code)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @PostMapping("/incentives/reference-codes/reorder")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "reorders all global incentive levels",
    description = "reorders all global incentive levels using provided list of Incentive codes, including inactive. 1-based index",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ReorderRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Reorder successful",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun reorderGlobalIncentiveLevels(
    @RequestBody
    request: ReorderRequest,
  ) {
    incentivesService.reorderGlobalIncentiveLevels(request.codeList)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @DeleteMapping("/incentives/reference-codes/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes an existing global incentive level",
    description = "Deletes an existing global incentive level, if level doesn't exist request is ignored",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Deletion successful (or ignored)",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteGlobalIncentiveLevel(
    @Schema(description = "Incentive reference code", example = "STD", required = true)
    @PathVariable
    code: String,
  ) {
    incentivesService.deleteGlobalIncentiveLevel(code)
  }
}
