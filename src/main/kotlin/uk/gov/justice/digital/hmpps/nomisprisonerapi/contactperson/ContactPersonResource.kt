package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

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

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactPersonResource(private val contactPersonService: ContactPersonService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @GetMapping("/persons/{personId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a person by person Id",
    description = "Retrieves a person and related contacts. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactPerson::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPerson(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
  ): ContactPerson = contactPersonService.getPerson(personId)
}

@Schema(description = "The data held in NOMIS about a person who is a contact for a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactPerson(
  @Schema(description = "The person id")
  val personId: Long,
  @Schema(description = "First name of the person")
  val firstName: String,
  @Schema(description = "Surname name of the person")
  val lastName: String,
  @Schema(description = "Middle name of the person")
  val middleName: String?,
  @Schema(description = "Date of birth of the person")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Gender of the person")
  val gender: CodeDescription?,
  @Schema(description = "Title of the person")
  val title: CodeDescription?,
)
