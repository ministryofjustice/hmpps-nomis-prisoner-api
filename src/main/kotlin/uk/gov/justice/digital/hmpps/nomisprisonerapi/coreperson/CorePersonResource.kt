package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonResource(private val corePersonService: CorePersonService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_CORE_PERSON')")
  @GetMapping("/core-person/{prisonNumber}")
  @Operation(
    summary = "Get an offender by prison number",
    description = "Retrieves an offender. Requires ROLE_NOMIS_CORE_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core person information returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CorePerson::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CORE_PERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffender(
    @Schema(description = "Prison number aka noms id / offender id display", example = "A1234BC") @PathVariable prisonNumber: String,
  ): CorePerson = corePersonService.getOffender(prisonNumber)
}

@Schema(description = "The data held in NOMIS for an offender")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorePerson(
  @Schema(description = "The prison number")
  val prisonNumber: String,
  @Schema(description = "The offender id. This will change if a different alias is made current")
  val offenderId: Long,
  @Schema(description = "Title of the person")
  val title: CodeDescription?,
  @Schema(description = "First name of the person")
  val firstName: String,
  @Schema(description = "Middle name of the person")
  val middleName1: String?,
  @Schema(description = "Second middle name of the person")
  val middleName2: String?,
  @Schema(description = "Surname name of the person")
  val lastName: String,
  @Schema(description = "Date of birth of the person")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Birth place of the person")
  val birthPlace: String?,
  @Schema(description = "Race of the person")
  val race: CodeDescription?,
  @Schema(description = "Sex of the person")
  val sex: CodeDescription?,
)
