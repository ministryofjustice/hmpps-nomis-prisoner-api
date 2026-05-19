package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/users"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class UserResource(private val userService: UserService) {

  @GetMapping("/{userId}")
  @Operation(
    summary = "Get staff user details",
    description = "Gets staff user details. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getUser(
    @Schema(description = "user Id") @PathVariable userId: Long,
  ) = userService.getUserDetails(userId)
}

data class UserDetails(
  @Schema(description = "The staff user id")
  val id: Long,
  @Schema(description = "Primary email address of the user")
  val email: String? = null,
  @Schema(description = "User's first name")
  val firstName: String,
  @Schema(description = "User's last name")
  val lastName: String,
  @Schema(description = "Status of the user")
  val statusCode: String,
  @Schema(description = "Accounts for the user")
  val accounts: List<UserAccount>,
  @Schema(description = "Audit data associated with the staff user")
  val audit: NomisAudit,
)

data class UserAccount(
  val username: String,
  @Schema(description = "The type of account", example = "GENERAL")
  val typeCode: String,
  @Schema(description = "Status of the user account", example = "ACTIVE")
  val statusCode: String,
  @Schema(description = "How the account was created", example = "USER")
  val sourceCode: String,
  @Schema(description = "Date and time when the user last logged in", example = "2023-12-23T11:17:00")
  val lastLoggedIn: LocalDateTime? = null,
  @Schema(description = "The current active caseload on the account")
  val activeCaseloadId: String? = null,
  @Schema(description = "Caseloads associated with the user")
  val caseloads: List<String>,
  @Schema(description = "Audit data associated with the account")
  val audit: NomisAudit,
)
