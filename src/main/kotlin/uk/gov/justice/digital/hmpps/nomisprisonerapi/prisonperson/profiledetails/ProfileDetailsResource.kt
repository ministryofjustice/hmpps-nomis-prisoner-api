package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ProfileDetailsResource(private val service: ProfileDetailsService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISON_PERSON')")
  @GetMapping("/prisoners/{offenderNo}/profile-details")
  @Operation(
    summary = "Get profile details for a prisoner",
    description = "Retrieves profile details for a prisoner and all of their aliases and bookings. Requires ROLE_NOMIS_PRISON_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Profile Details Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerProfileDetailsResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISON_PERSON",
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
  fun getProfileDetails(
    @Schema(description = "Offender number", example = "A1234AA") @PathVariable offenderNo: String,
  ): PrisonerProfileDetailsResponse = service.getProfileDetails(offenderNo)

  @PutMapping("/prisoners/{offenderNo}/profile-details")
  @Operation(
    summary = "Upsert profile details for a prisoner",
    description = "Upserts profile details on the latest booking for a prisoner, if it exists. Requires ROLE_NOMIS_PRISON_PERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpsertProfileDetailsRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Physical Attributes Updated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = UpsertProfileDetailsResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISON_PERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist or has no bookings",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun upsertPhysicalAttributes(
    @Schema(description = "Offender number", example = "A1234AA") @PathVariable offenderNo: String,
    @RequestBody @Valid request: UpsertProfileDetailsRequest,
  ): UpsertProfileDetailsResponse = service.upsertProfileDetails(offenderNo, request)
}
