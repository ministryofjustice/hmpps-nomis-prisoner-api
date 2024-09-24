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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts.NomisAudit
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
  @Schema(description = "Language of the person")
  val language: CodeDescription?,
  @Schema(description = "True if the person requires an interpreter")
  val interpreterRequired: Boolean,
  @Schema(description = "Domestic aka marital status of the person")
  val domesticStatus: CodeDescription?,
  @Schema(description = "Date the person dies")
  val deceasedDate: LocalDate?,
  @Schema(description = "True if a staff member")
  val isStaff: Boolean?,
  @Schema(description = "No longer used in NOMIS since 2018")
  val isRemitter: Boolean?,
  @Schema(description = "No longer used in NOMIS since 2019")
  val keepBiometrics: Boolean,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
  @Schema(description = "List of phone numbers for the person")
  val phoneNumbers: List<PhoneNumber>,
  @Schema(description = "List of addresses for the person")
  val addresses: List<Address>,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val phoneId: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
)

@Schema(description = "The data held in NOMIS about a address number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Address(
  @Schema(description = "Unique NOMIS Id of number")
  val addressId: Long,
  @Schema(description = "Address type")
  val type: CodeDescription?,
  @Schema(description = "Flat name or number", example = "Apartment 3")
  val flat: String?,
  @Schema(description = "Premise", example = "22")
  val premise: String?,
  @Schema(description = "Street", example = "West Street")
  val street: String?,
  @Schema(description = "Locality", example = "Keighley")
  val locality: String?,
  @Schema(description = "Post code", example = "MK15 2ST")
  val postcode: String?,
  @Schema(description = "City")
  val city: CodeDescription?,
  @Schema(description = "County")
  val county: CodeDescription?,
  @Schema(description = "Country")
  val country: CodeDescription?,
  @Schema(description = "List of phone numbers for the address")
  val phoneNumbers: List<PhoneNumber>,
)
