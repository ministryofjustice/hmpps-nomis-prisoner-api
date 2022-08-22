package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.IncentiveIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.IncentiveResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.IncentiveFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.service.IncentivesService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class IncentivesResource(private val incentivesService: IncentivesService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/ids")
  @Operation(
    summary = "get incentives (a.k.a IEP) by filter",
    description = "Retrieves a paged list of incentive composite ids by filter. Requires ROLE_NOMIS_INCENTIVES.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of composite ids are returned"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun getIncentivesByFilter(
    @PageableDefault(sort = ["iepDate", "iepTime"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by incentives that were assigned on or after the given date",
      example = "2021-11-03"
    ) fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by incentives that were assigned on or before the given date",
      example = "2021-11-03"
    ) toDate: LocalDate?,
    @RequestParam(value = "latestOnly", required = false)
    @Parameter(
      description = "if true only retrieve latest incentive for each prisoner",
      example = "true"
    ) latestOnly: Boolean? = false
  ): Page<IncentiveIdResponse> =
    incentivesService.findIncentiveIdsByFilter(
      pageRequest = pageRequest,
      IncentiveFilter(
        toDate = toDate,
        fromDate = fromDate,
        latestOnly = latestOnly ?: false
      )
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/incentives/booking-id/{bookingId}/incentive-sequence/{incentiveSequence}")
  @Operation(
    summary = "get a prisoner's incentive level (a.k.a IEP) by id (bookingId and incentiveId)",
    description = "Retrieves a created incentive level for a prisoner. Requires ROLE_NOMIS_INCENTIVES.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the incentive level details"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun getIncentive(
    @Schema(description = "NOMIS booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "NOMIS Incentive sequence ", example = "1", required = true)
    @PathVariable
    incentiveSequence: Long
  ): IncentiveResponse =
    incentivesService.getIncentive(
      bookingId = bookingId,
      incentiveSequence = incentiveSequence
    )
}
