package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/csip"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPResource(private val csipService: CSIPService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/ids")
  @Operation(
    summary = "get csip IDs by filter",
    description = "Retrieves a paged list of csip ids by filter. Requires ROLE_NOMIS_CSIP.",
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
        description = "Forbidden to access this endpoint when role NOMIS_CSIP not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdsByFilter(
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<CSIPIdResponse> =
    csipService.findIdsByFilter(
      pageRequest = pageRequest,
      CSIPFilter(
        toDate = toDate,
        fromDate = fromDate,
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/{id}")
  @Operation(
    summary = "Get CSIP details",
    description = "Gets csip details. Requires role NOMIS_CSIP",
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
        description = "Forbidden, requires role NOMIS_CSIP",
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
  fun getCSIP(
    @Schema(description = "CSIP id") @PathVariable id: Long,
  ) = csipService.getCSIP(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/count")
  @Operation(
    summary = "Get csip count",
    description = "Gets a count of all csips. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
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
        description = "Forbidden, requires role NOMIS_CSIP",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getCSIPCount() = csipService.getCSIPCount()
}

@Schema(description = "CSIP Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CSIPResponse(
  @Schema(description = "The csip id")
  val id: Long,
  @Schema(description = "The offender")
  val offender: Offender,
  @Schema(description = "The booking id associated with the CSIP")
  val bookingId: Long?,

  @Schema(description = "Log number")
  val logNumber: String?,

  @Schema(description = "Date/Time incident occurred")
  val incidentDateTime: LocalDateTime,
  @Schema(description = "Type of incident")
  val type: CodeDescription,
  @Schema(description = "Location of the incident")
  val location: CodeDescription,

  @Schema(description = "The Area of work, aka function")
  val areaOfWork: CodeDescription,
  @Schema(description = "The Staff reporting the incident")
  val reportedBy: Staff?,
  @Schema(description = "Date reported")
  val reportedDate: LocalDate,

  @Schema(description = "CSIP Plans")
  val plans: List<Plan>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Plan(
  @Schema(description = "Plan Id")
  val id: Long,
  @Schema(description = "Details of the need")
  val identifiedNeed: String,
  @Schema(description = "Intervention plan")
  val intervention: String,
  @Schema(description = "The Staff reporting ")
  val referredBy: Staff?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Staff(
  @Schema(description = "Username of first account related to staff")
  val username: String,
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Offender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of staff member")
  val firstName: String?,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)
