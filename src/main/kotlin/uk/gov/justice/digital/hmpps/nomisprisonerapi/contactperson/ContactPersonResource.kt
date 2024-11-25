package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
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

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @GetMapping("/persons/ids")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all Ids",
    description = "Retrieves all person Ids - typically for a migration. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of person Ids",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPersonIds(
    @PageableDefault(size = 20, sort = ["personId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "fromDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by persons that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by persons that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<PersonIdResponse> = contactPersonService.findPersonIdsByFilter(
    pageRequest = pageRequest,
    PersonFilter(
      toDate = toDate,
      fromDate = fromDate,
    ),
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person",
    description = "Creates a person, typically a person who will become a contact of a prisoners. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonResponse::class),
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
        responseCode = "409",
        description = "Person already exists with the same ID",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createPerson(
    @RequestBody @Valid
    request: CreatePersonRequest,
  ): CreatePersonResponse = contactPersonService.createPerson(request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/contact")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person contact",
    description = "Creates a person contact; the relationship between a prisoner and a person. Typically a prospective visitor. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Contact ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonContactResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad for example prisoner does not exist or contact / relationship types do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
      ApiResponse(
        responseCode = "409",
        description = "Contact with the specified relationship and type already exists for this prisoner's latest booking",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createPersonContact(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonContactRequest,
  ): CreatePersonContactResponse = contactPersonService.createPersonContact(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person address",
    description = "Creates a person address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Address ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonAddressResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad for example type code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun createPersonAddress(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonAddressRequest,
  ): CreatePersonAddressResponse = contactPersonService.createPersonAddress(personId, request)
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
  @Schema(description = "Set to true when person created via finance remitter page")
  val isRemitter: Boolean?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
  @Schema(description = "List of phone numbers for the person")
  val phoneNumbers: List<PersonPhoneNumber>,
  @Schema(description = "List of addresses for the person")
  val addresses: List<PersonAddress>,
  @Schema(description = "List of email addresses for the person")
  val emailAddresses: List<PersonEmailAddress>,
  @Schema(description = "List of employments for the person")
  val employments: List<PersonEmployment>,
  @Schema(description = "List of identifiers for the person")
  val identifiers: List<PersonIdentifier>,
  @Schema(description = "List of prisoner contacts this person is related to")
  val contacts: List<PersonContact>,
  @Schema(description = "List of restrictions between all prisoners and this person")
  val restrictions: List<ContactRestriction>,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonPhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val phoneId: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a address number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonAddress(
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
  val phoneNumbers: List<PersonPhoneNumber>,
  @Schema(description = "true if address validated by Post Office Address file??")
  val validatedPAF: Boolean,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean?,
  @Schema(description = "true if this is the person's primary address")
  val primaryAddress: Boolean,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean,
  @Schema(description = "Free format comment about the address")
  val comment: String?,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate?,
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a email address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonEmailAddress(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val email: String,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a person's employment")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonEmployment(
  @Schema(description = "Unique NOMIS sequence for this employment for this person")
  val sequence: Long,
  @Schema(description = "The entity the person is employed by")
  val corporate: PersonEmploymentCorporate,
  @Schema(description = "True is employment is active")
  val active: Boolean,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a corporate entity")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonEmploymentCorporate(
  @Schema(description = "Unique NOMIS Id of corporate address")
  val id: Long,
  @Schema(description = "The corporate name", example = "Police")
  val name: String,
)

@Schema(description = "The data held in NOMIS about a person's identifiers")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonIdentifier(
  @Schema(description = "Unique NOMIS sequence for this identifier for this person")
  val sequence: Long,
  @Schema(description = "The identifier type")
  val type: CodeDescription,
  @Schema(description = "The identifier value", example = "NE121212T")
  val identifier: String,
  @Schema(description = "The issued authority", example = "Police")
  val issuedAuthority: String?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a person's contact with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonContact(
  @Schema(description = "Unique NOMIS sequence for this identifier for this contact")
  val id: Long,
  @Schema(description = "The contact type")
  val contactType: CodeDescription,
  @Schema(description = "The relationship type")
  val relationshipType: CodeDescription,
  @Schema(description = "True if active")
  val active: Boolean,
  @Schema(description = "Date contact is no longer active")
  val expiryDate: LocalDate?,
  @Schema(description = "True if approved to visit the prisoner")
  val approvedVisitor: Boolean,
  @Schema(description = "True if next of kin to the prisoner")
  val nextOfKin: Boolean,
  @Schema(description = "True if emergency contact for the prisoner")
  val emergencyContact: Boolean,
  @Schema(description = "Free format comment text")
  val comment: String?,
  @Schema(description = "The prisoner this person is a contact for")
  val prisoner: ContactForPrisoner,
  @Schema(description = "List of restrictions specifically between the prisoner and this contact")
  val restrictions: List<ContactRestriction>,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a person's contact with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactForPrisoner(
  @Schema(description = "Unique NOMIS Id of booking associated with the prisoner")
  val bookingId: Long,
  @Schema(description = "Booking sequence this contact is related to. WHen 1 this indicates contact is for current term")
  val bookingSequence: Long,
  @Schema(description = "Offender no aka prisoner number", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "Last name of the prisoner", example = "Smith")
  val lastName: String,
  @Schema(description = "First name of the prisoner", example = "John")
  val firstName: String,
)

@Schema(description = "The data held in NOMIS about a person's restriction with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactRestriction(
  @Schema(description = "Unique NOMIS Id of the restriction")
  val id: Long,
  @Schema(description = "Restriction type")
  val type: CodeDescription,
  @Schema(description = "Free format comment text")
  val comment: String?,
  @Schema(description = "Date restriction became active")
  val effectiveDate: LocalDate,
  @Schema(description = "Date restriction is no longer active")
  val expiryDate: LocalDate?,
  @Schema(description = "Staff member who created the restriction")
  val enteredStaff: ContactRestrictionEnteredStaff,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactRestrictionEnteredStaff(
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "username for staff member. For staff with multiple accounts this will be the general account username.")
  val username: String,
)

data class PersonIdResponse(
  @Schema(description = "The person Id")
  val personId: Long,
)

@Schema(description = "Request to create an person (aka DPS contact) in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreatePersonRequest(
  @Schema(description = "The person id. When non-zero this will be used rather than the auto generated id", example = "12345")
  val personId: Long? = null,
  @Schema(description = "First name of the person", example = "Ashantee")
  val firstName: String,
  @Schema(description = "Surname name of the person", example = "Addo")
  val lastName: String,
  @Schema(description = "Middle name of the person", example = "Ashwin")
  val middleName: String? = null,
  @Schema(description = "Date of birth of the person")
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "Gender code of the person", example = "F")
  val genderCode: String? = null,
  @Schema(description = "Title code of the person", example = "DR")
  val titleCode: String? = null,
  @Schema(description = "Language code of the person", example = "FRE-FRA")
  val languageCode: String? = null,
  @Schema(description = "True if the person requires an interpreter")
  val interpreterRequired: Boolean = false,
  @Schema(description = "Domestic status code aka marital status of the person", example = "S")
  val domesticStatusCode: String? = null,
  @Schema(description = "True if a staff member")
  val isStaff: Boolean? = null,
)

data class CreatePersonResponse(
  @Schema(description = "The person Id")
  val personId: Long,
)

@Schema(description = "Request to create a contact (aka DPS prisoner contact) in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreatePersonContactRequest(
  @Schema(description = "Offender no aka prisoner number. Contact will be added to latest booking", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "The contact type", example = "S")
  val contactTypeCode: String,
  @Schema(description = "The relationship type", example = "BRO")
  val relationshipTypeCode: String,
  @Schema(description = "True if active")
  val active: Boolean,
  @Schema(description = "Date contact is no longer active")
  val expiryDate: LocalDate?,
  @Schema(description = "True if approved to visit the prisoner")
  val approvedVisitor: Boolean,
  @Schema(description = "True if next of kin to the prisoner")
  val nextOfKin: Boolean,
  @Schema(description = "True if emergency contact for the prisoner")
  val emergencyContact: Boolean,
  @Schema(description = "Free format comment text")
  val comment: String?,
)

data class CreatePersonContactResponse(
  @Schema(description = "The contact Id")
  val personContactId: Long,
)

data class CreatePersonAddressRequest(
  @Schema(description = "Address reference code", example = "HOME")
  val typeCode: String? = null,
  @Schema(description = "Flat name or number", example = "Apartment 3")
  val flat: String? = null,
  @Schema(description = "Premise", example = "22")
  val premise: String? = null,
  @Schema(description = "Street", example = "West Street")
  val street: String? = null,
  @Schema(description = "Locality", example = "Keighley")
  val locality: String? = null,
  @Schema(description = "Post code", example = "MK15 2ST")
  val postcode: String? = null,
  @Schema(description = "City reference code", example = "25343")
  val cityCode: String? = null,
  @Schema(description = "County reference code", example = "S.YORKSHIRE")
  val countyCode: String? = null,
  @Schema(description = "Country reference code", example = "ENG")
  val countryCode: String? = null,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean? = null,
  @Schema(description = "true if this is the person's primary address")
  val primaryAddress: Boolean,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean,
  @Schema(description = "Free format comment about the address")
  val comment: String? = null,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate? = null,
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate? = null,
)

data class CreatePersonAddressResponse(
  @Schema(description = "The address Id")
  val personAddressId: Long,
)
