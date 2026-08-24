package uk.gov.justice.digital.hmpps.nomisprisonerapi.staff

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/staff"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class StaffResource(private val staffService: StaffService) {

  @GetMapping("/id-ranges")
  @Operation(
    summary = "Gets every size staff ids for staff search.",
    description = """Returns a list of staff id ranges for staff. Each list item has a from and to 
      staff id, with the number of staff between the from and to equal to the specified size. 
      Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of staff id ranges"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAllStaffIdRanges(
    @RequestParam(value = "active", required = true)
    @Parameter(
      description = "When true only return active staff currently in prison else all staff are returned.",
    )
    active: Boolean,
    @RequestParam(value = "size", defaultValue = "1000")
    @Parameter(description = "Number of staff to get")
    pageSize: Int,
  ): List<StaffIdRange> = staffService.findStaffIdRanges(pageSize, active)

  @GetMapping("/ids")
  @Operation(
    summary = "Gets every staff id between range.",
    description = """Returns a list of staff ids for staff ids greater than the specified
      fromStaffId and less than or equal to the specified toStaffId.
      Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of staff ids"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAllStaffInRange(
    @RequestParam(value = "active", required = true)
    @Parameter(description = "When true only return active staff (those with status \"ACTIVE\") else all staff are returned.")
    active: Boolean,
    @RequestParam(required = true)
    @Parameter(description = "Return staff with staff id greater than this value.")
    fromStaffId: Long,
    @RequestParam(required = true)
    @Parameter(description = "Return staff with staff id less than or equal to this value.")
    toStaffId: Long,
  ): List<Long> = staffService.findStaffIdsInRange(fromStaffId, toStaffId, active)

  @GetMapping("/id/{staffId}")
  @Operation(
    summary = "Get staff details",
    description = "Gets staff details. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getStaffById(
    @Schema(description = "staff Id") @PathVariable staffId: Long,
    @RequestParam(name = "dpsRolesOnly")
    @Schema(description = "Only return dps roles for the staff", example = "true")
    dpsRolesOnly: Boolean = true,
  ) = staffService.getStaffDetails(staffId, dpsRolesOnly)

  @GetMapping("/username/{username}")
  @Operation(
    summary = "Get staff details by any of the staff's username",
    description = "Gets staff details. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getStaffByUsername(
    @Schema(description = "staff Id") @PathVariable username: String,
    @RequestParam(name = "dpsRolesOnly")
    @Schema(description = "Only return dps roles for the staff", example = "true")
    dpsRolesOnly: Boolean = true,
  ) = staffService.getStaffDetails(username, dpsRolesOnly)

  @GetMapping("/pagedIds")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all staff Ids - DEPRECATED - use /ids instead",
    description = "Typically for a migration. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of staff Ids",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getStaffIds(
    @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
  ): PagedModel<StaffIdResponse> = PagedModel(staffService.getStaffIds(pageRequest = pageRequest))

  @GetMapping("/ids/all-from-id")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all staff Ids",
    description = """
      Retrieves staff ids to be iterated over with optional starting from specific staff Id.
      Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of staff Ids",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getStaffIdsFromId(
    @Schema(description = "If supplied get staff ids starting after this id", required = false, example = "1555999")
    @RequestParam(value = "staffId", defaultValue = "0")
    staffId: Long,
    @Schema(description = "Number of ids to get", required = false, defaultValue = "20")
    @RequestParam(value = "size", defaultValue = "20")
    size: Int,
  ): StaffIdsPage = staffService.getStaffIdsFromId(
    staffId = staffId,
    pageSize = size,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Staff details")
data class StaffDetails(
  @Schema(description = "The unique staff id", example = "12345")
  val id: Long,
  @Schema(description = "List of email addresses for the staff user", example = "['fred@example.com','fred2.example.com']")
  val emailAddresses: List<StaffEmail>,
  @Schema(description = "Staff user's first name", example = "John")
  val firstName: String,
  @Schema(description = "Staff user's last name", example = "Smith")
  val lastName: String,
  @Schema(description = "Status of the staff user", example = "ACTIVE")
  val status: String,
  @Schema(description = "Accounts for the staff user")
  val accounts: List<StaffAccount>,
  @Schema(description = "Audit data associated with the staff user")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Staff email")
data class StaffEmail(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val email: String,
  @Schema(description = "Audit data associated with the staff email")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StaffAccount(
  @Schema(description = "The username associated with the account", example = "JOHNSMITH_GEN")
  val username: String,
  @Schema(description = "The type of account", example = "GENERAL")
  val typeCode: String,
  @Schema(description = "Status of the account", example = "OPEN")
  val status: String,
  @Schema(description = "How the account was created", example = "USER")
  val sourceCode: String,
  @Schema(description = "Date and time when the user last logged in", example = "2023-12-23T11:17:00")
  val lastLoggedIn: LocalDateTime? = null,
  @Schema(description = "The current active caseload on the account", example = "MDI")
  val activeCaseloadId: String? = null,
  @Schema(description = "Caseloads and roles associated with the user")
  val caseloads: List<CaseloadResponse>,
  @Schema(description = "Audit data associated with the account")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StaffIdsPage(
  @Schema(description = "Page of staff IDs")
  val ids: List<StaffIdResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseloadResponse(
  @Schema(description = "Caseload id", example = "MDI")
  val caseloadId: String,
  @Schema(description = "Roles associated with the user caseload")
  val roles: List<RoleResponse>,
  @Schema(description = "Audit data associated with the user caseload")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoleResponse(
  @Schema(description = "Role code", example = "ROLE_1")
  val code: String,
  @Schema(description = "Role Description", example = "Ability to access user details")
  val name: String,
  @Schema(description = "Audit data associated with the user role")
  val audit: NomisAudit,
)

@Schema(description = "Staff ID range.")
data class StaffIdRange(
  @Schema(description = "The lowest NOMIS staffId in the range", example = "1234567")
  val fromStaffId: Long,
  @Schema(description = "The highest NOMIS staffId in the range", example = "1234567")
  val toStaffId: Long,
)

fun List<Long>.toStaffIdRanges(): List<StaffIdRange> = mutableListOf(0L).apply {
  addAll(this@toStaffIdRanges)
  add(Long.MAX_VALUE)
}
  .zipWithNext()
  .map { StaffIdRange(it.first, it.second) }
