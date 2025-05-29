package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import java.time.LocalDate

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_VISIT_BALANCE')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitBalanceResource(
  private val visitBalanceService: VisitBalanceService,
) {

  @GetMapping("/visit-balances/ids")
  @Operation(
    summary = "Find paged visit balance ids",
    description = """
      Returns the visit balance ids (which are booking ids) for the latest booking for offenders with balance entries.
      Requires role NOMIS_VISIT_BALANCE""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_VISIT_BALANCE",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun findVisitBalanceIds(
    @PageableDefault(sort = ["offenderBookingId"], direction = Sort.Direction.ASC) pageRequest: Pageable,
    @Schema(description = "Prison id") @RequestParam prisonId: String?,
  ): Page<VisitBalanceIdResponse> = visitBalanceService.findAllIds(prisonId, pageRequest)

  @GetMapping("/visit-balances/{visitBalanceId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get visit balance data for a booking",
    description = "Retrieves visit order balance for a booking . Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit balance returned",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit Balance does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getVisitBalanceByIdToMigrate(
    @Schema(description = "Visit balance (offender booking) id.", example = "12345")
    @PathVariable
    visitBalanceId: Long,
  ): VisitBalanceDetailResponse = visitBalanceService.getVisitBalanceDetailsById(visitBalanceId)

  @GetMapping("/prisoners/{prisonNumber}/visit-balance/details")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get visit balance details for a prisoner",
    description = "Retrieves visit balance details including last IEP allocation date for a prisoner. Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit balance details returned",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
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
    ],
  )
  fun getVisitBalanceDetailsForPrisoner(
    @Schema(description = "Prison number aka Offender No.", example = "A1234AK")
    @PathVariable
    prisonNumber: String,
  ): VisitBalanceDetailResponse = visitBalanceService.getVisitBalanceDetailsForPrisoner(prisonNumber)

  @GetMapping("/prisoners/{prisonNumber}/visit-balance")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get visit order balance data for a prisoner",
    description = "Retrieves visit order balance details for a prisoner. Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit balance returned or null if no visit balance exists",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
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
    ],
  )
  fun getVisitBalanceForPrisoner(
    @Schema(description = "Prison number aka Offender No.", example = "A1234AK")
    @PathVariable
    prisonNumber: String,
  ): VisitBalanceResponse? = visitBalanceService.getVisitBalanceForPrisoner(prisonNumber)

  @GetMapping("/visit-balances/visit-balance-adjustment/{visitBalanceAdjustmentId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get specific offender visit balance adjustment",
    description = "Retrieves offender visit balance adjustment. Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit balance adjustment returned",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjustment does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getVisitBalanceAdjustment(
    @Schema(description = "Visit balance adjustment id", example = "5")
    @PathVariable
    visitBalanceAdjustmentId: Long,
  ): VisitBalanceAdjustmentResponse = visitBalanceService.getVisitBalanceAdjustment(visitBalanceAdjustmentId)

  @PostMapping("/prisoners/{prisonNumber}/visit-balance-adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Inserts a visit balance adjustment for an offender",
    description = "Creates a visit balance adjustment on the prisoner's latest booking. Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit balance adjustment created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
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
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
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
    ],
  )
  fun createVisitBalanceAdjustment(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    prisonNumber: String,
    @RequestBody @Valid
    request: CreateVisitBalanceAdjustmentRequest,
  ): CreateVisitBalanceAdjustmentResponse = visitBalanceService.createVisitBalanceAdjustment(prisonNumber, request)

  @PutMapping("/prisoners/{prisonNumber}/visit-balance")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Updates a visit order balance for an offender",
    description = "Updates a visit order balance on the prisoner's latest booking or creates one if it doesn't already exist. Requires ROLE_NOMIS_VISIT_BALANCE",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Visit balance updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
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
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_BALANCE",
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
    ],
  )
  fun updateVisitBalance(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    prisonNumber: String,
    @Valid @RequestBody
    request: UpdateVisitBalanceRequest,
  ) = visitBalanceService.upsertVisitBalance(prisonNumber, request)
}

@Schema(description = "Visit Balance update request")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateVisitBalanceRequest(
  @Schema(description = "Total number of unallocated (remaining) visit orders", required = false)
  @field:Min(0)
  val remainingVisitOrders: Int?,
  @Schema(description = "Total number of unallocated (remaining) privileged visit orders", required = false)
  @field:Min(0)
  val remainingPrivilegedVisitOrders: Int?,
)

data class CreateVisitBalanceAdjustmentRequest(
  @Schema(description = "Number of visit orders affected by the adjustment")
  val visitOrderChange: Int? = null,
  @Schema(description = "Previous number of visit orders before the adjustment")
  val previousVisitOrderCount: Int? = null,
  @Schema(description = "Number of privileged visit orders affected by the adjustment")
  val privilegedVisitOrderChange: Int? = null,
  @Schema(description = "Previous number of privileged visit orders before the adjustment")
  val previousPrivilegedVisitOrderCount: Int? = null,
  @Schema(description = "Date the adjust was made")
  val adjustmentDate: LocalDate,
  @Schema(description = "Comment text")
  val comment: String? = null,
  @Schema(description = "Which user authorised the adjustment. Will be null for a system initiated change")
  val authorisedUsername: String? = null,
)

@Schema(description = "A response after a visit balance adjustment is created in NOMIS")
data class CreateVisitBalanceAdjustmentResponse(
  @Schema(description = "The id of the visit balance adjustment")
  val visitBalanceAdjustmentId: Long,
)

@Schema(description = "The visit balance held against a prisoner's latest booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitBalanceResponse(
  @Schema(description = "Total number of unallocated (remaining) visit orders")
  val remainingVisitOrders: Int,
  @Schema(description = "Total number of unallocated (remaining) privileged visit orders")
  val remainingPrivilegedVisitOrders: Int,
)

@Schema(description = "The visit balance held against a prisoner's latest booking including the last IEP Allocation date")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitBalanceDetailResponse(
  @Schema(description = "Prison number aka noms id / offender id display", example = "A1234BC")
  val prisonNumber: String,
  @Schema(description = "Total number of unallocated (remaining) visit orders")
  val remainingVisitOrders: Int,
  @Schema(description = "Total number of unallocated (remaining) privileged visit orders")
  val remainingPrivilegedVisitOrders: Int,
  @Schema(description = "The date of the last IEP Allocation date via the batch process, if it exists")
  val lastIEPAllocationDate: LocalDate? = null,
)

@Schema(description = "The visit order balance changes held against a booking for a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitBalanceAdjustmentResponse(
  @Schema(description = "Number of visit orders affected by the adjustment")
  val visitOrderChange: Int? = null,
  @Schema(description = "Previous number of visit orders before the adjustment")
  val previousVisitOrderCount: Int? = null,
  @Schema(description = "Number of privileged visit orders affected by the adjustment")
  val privilegedVisitOrderChange: Int? = null,
  @Schema(description = "Previous number of privileged visit orders before the adjustment")
  val previousPrivilegedVisitOrderCount: Int? = null,
  @Schema(description = "Adjustment reason")
  val adjustmentReason: CodeDescription,
  @Schema(description = "Date the adjust was made")
  val adjustmentDate: LocalDate,
  @Schema(description = "Comment text")
  val comment: String? = null,
  @Schema(description = "Expiry balance")
  val expiryBalance: Int? = null,
  @Schema(description = "Expiry date")
  val expiryDate: LocalDate? = null,
  @Schema(description = "Which staff member endorsed the adjustment aka Entered by")
  val endorsedStaffId: Long? = null,
  @Schema(description = "Which staff member authorised the adjustment")
  val authorisedStaffId: Long? = null,
  @Schema(description = "Who created the adjustment")
  val createUsername: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "visit balance id")
data class VisitBalanceIdResponse(
  @Schema(description = "The visit balance (booking) id")
  val visitBalanceId: Long,
)

fun OffenderVisitBalanceAdjustment.toVisitBalanceAdjustmentResponse(): VisitBalanceAdjustmentResponse = VisitBalanceAdjustmentResponse(
  visitOrderChange = remainingVisitOrders,
  previousVisitOrderCount = previousRemainingVisitOrders,
  privilegedVisitOrderChange = remainingPrivilegedVisitOrders,
  previousPrivilegedVisitOrderCount = previousRemainingPrivilegedVisitOrders,
  adjustmentReason = adjustReasonCode.toCodeDescription(),
  authorisedStaffId = authorisedStaffId,
  endorsedStaffId = endorsedStaffId,
  adjustmentDate = adjustDate,
  comment = commentText,
  expiryBalance = expiryBalance,
  expiryDate = expiryDate,
  createUsername = createUsername,
)
