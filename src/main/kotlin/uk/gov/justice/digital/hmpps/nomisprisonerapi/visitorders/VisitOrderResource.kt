package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitorders

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitOrderBalanceResource(
  private val visitOrderService: VisitOrderService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_VISIT_ORDERS')")
  @GetMapping("/prisoners/{offenderNo}/visit-orders/balance/to-migrate")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get visit order balance data for a prisoner",
    description = "Retrieves visit order balance details for the last month for a prisoner. Requires ROLE_NOMIS_VISIT_ORDERS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit Orders Returned",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_VISIT_ORDERS",
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
  fun getVisitOrderBalanceToMigrate(
    @Schema(description = "Offender No AKA prisoner number", example = "A1234AK")
    @PathVariable
    offenderNo: String,
  ): PrisonerVisitOrderBalanceResponse = visitOrderService.getVisitOrderBalance(offenderNo)
}

@Schema(description = "The list of visit orders held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerVisitOrderBalanceResponse(

  @Schema(description = "Total number of unallocated (remaining) visit orders")
  val remainingVisitOrders: Int = 0,
  @Schema(description = "Total number of unallocated (remaining) privileged visit orders")
  val remainingPrivilegedVisitOrders: Int = 0,

  @Schema(description = "Balance adjustments for this prisoner over the last 28 days")
  val visitOrderBalanceAdjustments: List<VisitOrderBalanceAdjResponse>,
)

@Schema(description = "The visit orders balance changes held against a booking for a  prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitOrderBalanceAdjResponse(
  @Schema(description = "Number of visit orders affected by the adjustment")
  val remainingVisitOrders: Int? = null,
  @Schema(description = "Previous number of visit orders before the adjustment")
  val previousRemainingVisitOrders: Int? = null,
  @Schema(description = "Number of privileged visit orders affected by the adjustment")
  val remainingPrivilegedVisitOrders: Int? = null,
  @Schema(description = "Previous number of privileged visit orders before the adjustment")
  val previousRemainingPrivilegedVisitOrders: Int? = null,
  @Schema(description = "Adjustment reason")
  val adjustmentReason: CodeDescription? = null,
  @Schema(description = "Date the adjust was made")
  val adjustmentDate: LocalDate? = null,
  @Schema(description = "Comment text")
  val comment: String? = null,
  @Schema(description = "Expiry balance")
  val expiryBalance: Int? = null,
  @Schema(description = "Expiry date")
  val expiryDate: LocalDate? = null,
  @Schema(description = "Expiry status")
  val expiryStatus: String? = null,
  @Schema(description = "Which staff member endorsed the adjustment aka Entered by")
  val endorsedStaffId: Long? = null,
  @Schema(description = "Which staff member authorised the adjustment")
  val authorisedStaffId: Long? = null,
  // TODO add in - Not normally needed but included to compare against wrongly entered adjust_date
  // @Schema(description = "Date/time the adjustment was created")
  // val createDateTime: LocalDateTime
)

fun OffenderVisitBalanceAdjustment.toVisitOrderBalanceAdjResponse(): VisitOrderBalanceAdjResponse = VisitOrderBalanceAdjResponse(
  remainingVisitOrders = remainingVisitOrders,
  previousRemainingVisitOrders = previousRemainingVisitOrders,
  remainingPrivilegedVisitOrders = remainingPrivilegedVisitOrders,
  previousRemainingPrivilegedVisitOrders = previousRemainingPrivilegedVisitOrders,
  adjustmentReason = adjustReasonCode?.toCodeDescription(),
  authorisedStaffId = authorisedStaffId,
  endorsedStaffId = endorsedStaffId,
  adjustmentDate = adjustDate,
  comment = commentText,
  expiryBalance = expiryBalance,
  expiryDate = expiryDate,
  expiryStatus = expiryStatus,
)
