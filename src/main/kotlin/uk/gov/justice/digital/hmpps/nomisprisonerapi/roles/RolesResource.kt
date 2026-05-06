package uk.gov.justice.digital.hmpps.nomisprisonerapi.roles

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(value = ["/roles"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class RolesResource(
  private val rolesService: RolesService,
) {
  @GetMapping()
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all roles",
    description = "Retrieves all roles. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Roles Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RoleDetail::class),
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
  fun getAllRoles(
    @Schema(description = "Get all roles, which includes both DPS and NOMIS roles", example = "true")
    @RequestParam(value = "all-roles", defaultValue = "false")
    allRoles: Boolean = false,
  ): List<RoleDetail> = if (allRoles) {
    rolesService.getAllRoles()
  } else {
    rolesService.getAllDpsRoles()
  }

  @GetMapping("/{roleCode}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get details for a role",
    description = "Retrieves details for a Role by its code. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role returned",
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
      ApiResponse(
        responseCode = "404",
        description = "Role does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getRoleDetails(
    @Schema(description = "Role Code", example = "INCIDENT_REPORTS__RO")
    @PathVariable
    roleCode: String,
  ): RoleDetail = rolesService.findRoleByCode(roleCode)
}

@Schema(description = "The data held in NOMIS about a role")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoleDetail(

  @Schema(description = "Role Code", example = "GLOBAL_SEARCH")
  val code: String,

  @Schema(description = "Role Name", example = "Global Search Role")
  val name: String,

  @Schema(description = "Role Type ", example = "APP")
  val type: String? = null,

  @Schema(description = "If the role is for admin users only", example = "true", defaultValue = "false")
  val adminRoleOnly: Boolean = false,
)
