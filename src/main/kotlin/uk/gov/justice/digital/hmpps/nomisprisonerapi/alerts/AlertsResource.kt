package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

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
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsResource(
  private val alertsService: AlertsService,
) {
  @PreAuthorize("hasRole('ROLE_NOMIS_ALERTS')")
  @GetMapping("/prisoner/booking-id/{bookingId}/alerts/{alertSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get an alert by bookingId and alert sequence",
    description = "Retrieves an prisoner alert. Requires ROLE_NOMIS_ALERTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alert Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AlertResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_ALERTS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Alert does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlert(
    @Schema(description = "Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
    @Schema(description = "Alert sequence", example = "3")
    @PathVariable
    alertSequence: Long,
  ): AlertResponse = alertsService.getAlert(bookingId, alertSequence)
}

@Schema(description = "The data held in NOMIS about an alert associated with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AlertResponse(
  @Schema(description = "The prisoner's bookingId related to this alert")
  val bookingId: Long,
  @Schema(description = "The sequence primary key within this booking")
  val alertSequence: Long,
  @Schema(description = "The alert code")
  val alertCode: CodeDescription,
  @Schema(description = "The alert type")
  val type: CodeDescription,
  @Schema(description = "Date alert started")
  val date: LocalDate,
  @Schema(description = "Date alert expired")
  val expiryDate: LocalDate? = null,
  @Schema(description = "true if alert is active and has not expired")
  val isActive: Boolean = true,
  @Schema(description = "true if alert has been verified by another member of staff")
  val isVerified: Boolean = false,
  @Schema(description = "Free format text of person or department that authorised the alert", example = "security")
  val authorisedBy: String? = null,
  @Schema(description = "Free format comment")
  val comment: String? = null,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS the person or system that created this record")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisAudit(
  @Schema(description = "Date time record was created")
  val createDatetime: LocalDateTime,
  @Schema(description = "Username of person that created the record (might also be a system) ")
  val createUsername: String,
  @Schema(description = "Username of person that last modified the record (might also be a system)")
  val modifyUserId: String? = null,
  @Schema(description = "Date time record was last modified")
  val modifyDatetime: LocalDateTime? = null,
  @Schema(description = "Audit Date time")
  val auditTimestamp: LocalDateTime? = null,
  @Schema(description = "Audit username")
  val auditUserId: String? = null,
  @Schema(description = "NOMIS or DPS module that created the record")
  val auditModuleName: String? = null,
  @Schema(description = "Client userid")
  val auditClientUserId: String? = null,
  @Schema(description = "IP Address where request originated from")
  val auditClientIpAddress: String? = null,
  @Schema(description = "Machine name where request originated from")
  val auditClientWorkstationName: String? = null,
  @Schema(description = "Additional information that is audited")
  val auditAdditionalInfo: String? = null,
)
