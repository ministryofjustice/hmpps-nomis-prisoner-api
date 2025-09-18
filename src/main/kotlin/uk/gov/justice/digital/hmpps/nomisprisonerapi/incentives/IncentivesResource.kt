package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import io.swagger.v3.oas.annotations.Hidden
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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class IncentivesResource(private val incentivesService: IncentivesService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
  ): CreateIncentiveResponse = incentivesService.createIncentive(bookingId, createIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/incentives/ids")
  @Operation(
    summary = "get incentives (a.k.a IEP) by filter",
    description = "Retrieves a paged list of incentive composite ids by filter. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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
  ): Page<IncentiveIdResponse> = incentivesService.findIncentiveIdsByFilter(
    pageRequest = pageRequest,
    IncentiveFilter(
      toDate = toDate,
      fromDate = fromDate,
      latestOnly = latestOnly ?: false,
    ),
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/incentives/booking-id/{bookingId}/incentive-sequence/{incentiveSequence}")
  @Operation(
    summary = "get a prisoner's incentive level (a.k.a IEP) by id (bookingId and incentiveId)",
    description = "Retrieves a created incentive level for a prisoner. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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
  ): IncentiveResponse = incentivesService.getIncentive(
    bookingId = bookingId,
    incentiveSequence = incentiveSequence,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/incentives/booking-id/{bookingId}/current")
  @Operation(
    summary = "get a prisoner's current incentive level (a.k.a IEP) for a booking",
    description = "Retrieves the current incentive level (by booking) for a prisoner. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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
  ): IncentiveResponse = incentivesService.getCurrentIncentive(
    bookingId = bookingId,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/booking-id/{bookingId}/incentives/reorder")
  @Operation(
    summary = "Reorder a existing incentives to match time order",
    description = "Reorder a series of IEPs so the sequence number matches the IEP date time. Latest time gets the higher sequence so the current IEP is the latest. This is required to correct DPS incentives that are created out of order",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Incentives successfully reordered",
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
        description = "Access this endpoint forbidden, incorrect role. Must have NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun reorderCurrentIncentives(
    @Schema(description = "Offender Booking Id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
  ): Unit = incentivesService.reorderCurrentIncentives(bookingId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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
    createIncentiveRequest: CreateGlobalIncentiveRequest,
  ): ReferenceCode = incentivesService.createGlobalIncentiveLevel(createIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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
  ): ReferenceCode = incentivesService.updateGlobalIncentiveLevel(code, updateIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
  ): ReferenceCode = incentivesService.getGlobalIncentiveLevel(code)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/incentives/reference-codes/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Hidden
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/incentives/prison/{prison}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Prison Incentive level data",
    description = "Creates incentive level data associated with a Prison",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreatePrisonIncentiveRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Prison Incentive level data created",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createPrisonIncentiveLevelData(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    prison: String,
    @RequestBody @Valid
    createIncentiveRequest: CreatePrisonIncentiveRequest,
  ): PrisonIncentiveLevelDataResponse = incentivesService.createPrisonIncentiveLevelData(prison, createIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/incentives/prison/{prison}/code/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Prison Incentive level data",
    description = "Creates incentive level data associated with a Prison",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreatePrisonIncentiveRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prison Incentive level data updated",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePrisonIncentiveLevelData(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    prison: String,
    @Schema(description = "Incentive level code", example = "STD", required = true)
    @PathVariable
    code: String,
    @RequestBody @Valid
    updateIncentiveRequest: UpdatePrisonIncentiveRequest,
  ): PrisonIncentiveLevelDataResponse = incentivesService.updatePrisonIncentiveLevelData(prison, code, updateIncentiveRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/incentives/prison/{prison}/code/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets the prison incentive level",
    description = "Gets prison incentive level data by provided code and prison",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "return the Prison Incentive level",
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
        description = "Prison Incentive Level does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonIncentiveLevel(
    @Schema(description = "Prison id", example = "MDI", required = true)
    @PathVariable
    prison: String,
    @Schema(description = "Incentive level code", example = "STD", required = true)
    @PathVariable
    code: String,
  ): PrisonIncentiveLevelDataResponse = incentivesService.getPrisonIncentiveLevel(prison, code)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/incentives/prison/{prison}/code/{code}")
  @ResponseStatus(HttpStatus.OK)
  @Hidden
  @Operation(
    summary = "FOR TESTING ONLY - Deletes an existing global incentive level",
    description = "FOR TESTING ONLY - Deletes existing prison incentive level data, if level doesn't exist request is ignored",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deletePrisonIncentiveLevel(
    @Schema(description = "Prison id", example = "MDI", required = true)
    @PathVariable
    prison: String,
    @Schema(description = "Incentive level code", example = "STD", required = true)
    @PathVariable
    code: String,
  ) {
    incentivesService.deletePrisonIncentiveLevelData(prison, code)
  }
}
