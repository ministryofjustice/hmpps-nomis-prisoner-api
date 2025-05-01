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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
  @GetMapping("/contact/{contactId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a contact by ID",
    description = "Gets a single contact by ID",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contact Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonContact::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getContact(
    @Schema(description = "Contact Id", example = "75675")
    @PathVariable
    contactId: Long,
  ): PersonContact = contactPersonService.getContact(contactId)

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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  @GetMapping("/prisoners/{offenderNo}/contacts")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets a prisoner's contacts",
    description = "Retrieves all contacts across all bookings for a prisoner. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contacts Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerWithContacts::class),
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
  fun getPrisonerWithContacts(
    @Schema(description = "Offender No aka prisoner number", example = "A1234KT")
    @PathVariable
    offenderNo: String,
    @RequestParam(value = "active-only", required = false, defaultValue = "true")
    @Parameter(
      description = "When true only return contacts that are active",
      example = "false",
    )
    activeOnly: Boolean,
    @RequestParam(value = "latest-booking-only", required = false, defaultValue = "true")
    @Parameter(
      description = "When true only return contacts that related to latest booking",
      example = "false",
    )
    latestBookingOnly: Boolean,
  ): PrisonerWithContacts = contactPersonService.getPrisonerWithContacts(offenderNo, activeOnly, latestBookingOnly)

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

  @PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/persons/ids/all-from-id")
  @Operation(
    summary = "Gets the identifier for all persons.",
    description = """Gets the specified number of persons starting after the given id number.
      Clients can iterate through all persons by calling this endpoint using the id from the last call (omit for first call).
      Iteration ends when the returned prisonerIds list has size less than the requested page size.
      Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of person ids"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getPersonIdsFromId(
    @Schema(description = "If supplied get person starting after this id", required = false, example = "1555999")
    @RequestParam(value = "personId", defaultValue = "0")
    personId: Long,
    @Schema(description = "Number of persons to get", required = false, defaultValue = "10")
    @RequestParam(value = "pageSize", defaultValue = "10")
    pageSize: Int,
  ): PersonIdsWithLast = contactPersonService.findPersonIdsFromId(personId, pageSize)

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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  @DeleteMapping("/persons/{personId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a person",
    description = "Deletes a person and any associated data e.g contact relationships, addresses. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Deleted",
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
  fun deletePerson(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
  ) = contactPersonService.deletePerson(personId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}")
  @Operation(
    summary = "Updates a person",
    description = "Updates core person data but leaves any associated data e.g contact relationships, addresses. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Updated",
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
  fun updatePerson(
    @RequestBody @Valid
    request: UpdatePersonRequest,
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
  ) = contactPersonService.updatePerson(personId, request)

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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  @PutMapping("/persons/{personId}/contact/{contactId}")
  @Operation(
    summary = "Updates a person contact",
    description = "Updates a person contact; the relationship between a prisoner and a person. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Contact Updated",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or contact does not exist",
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
  fun updatePersonContact(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Contact Id", example = "75675")
    @PathVariable
    contactId: Long,
    @RequestBody @Valid
    request: UpdatePersonContactRequest,
  ) = contactPersonService.updatePersonContact(personId, contactId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/persons/{personId}/contact/{contactId}")
  @Operation(
    summary = "Deletes a person contact",
    description = "Deletes a person contact; the relationship between a prisoner and a person. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Contact Updated, returned if in contact does not exist",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Contact does belong to person",
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
  fun deletePersonContact(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Contact Id", example = "75675")
    @PathVariable
    contactId: Long,
  ) = contactPersonService.deletePersonContact(personId, contactId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/contact/{contactId}/restriction")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person contact restriction for a specific relationship",
    description = "Creates a person contact restriction; the restriction is for a specific relationship between a prisoner and a person. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Contact ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateContactPersonRestrictionResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example restriction type does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or contact does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createPersonContactRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Contact Id", example = "67899")
    @PathVariable
    contactId: Long,
    @RequestBody @Valid
    request: CreateContactPersonRestrictionRequest,
  ): CreateContactPersonRestrictionResponse = contactPersonService.createPersonContactRestriction(personId, contactId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/contact/{contactId}/restriction/{contactRestrictionId}")
  @Operation(
    summary = "Updates a person contact restriction for a specific relationship",
    description = "Updates a person contact restriction; the restriction is for a specific relationship between a prisoner and a person. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example restriction type does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or contact or restriction does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonContactRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Contact Id", example = "67899")
    @PathVariable
    contactId: Long,
    @Schema(description = "Restriction Id", example = "38383")
    @PathVariable
    contactRestrictionId: Long,
    @RequestBody @Valid
    request: UpdateContactPersonRestrictionRequest,
  ) = contactPersonService.updatePersonContactRestriction(personId, contactId, contactRestrictionId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/persons/{personId}/contact/{contactId}/restriction/{contactRestrictionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a person contact restriction for a specific relationship",
    description = "Deletes a person contact restriction; the restriction is for a specific relationship between a prisoner and a person. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The personId, ContactId or restrictionId exist but on other relationships",
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
  fun deletePersonContactRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Contact Id", example = "67899")
    @PathVariable
    contactId: Long,
    @Schema(description = "Restriction Id", example = "38383")
    @PathVariable
    contactRestrictionId: Long,
  ) = contactPersonService.deletePersonContactRestriction(personId, contactId, contactRestrictionId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/restriction")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a global person restriction",
    description = "Creates a person restriction; the restriction is estate wide. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Contact ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateContactPersonRestrictionResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example restriction type does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  fun createPersonRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreateContactPersonRestrictionRequest,
  ): CreateContactPersonRestrictionResponse = contactPersonService.createPersonRestriction(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/restriction/{personRestrictionId}")
  @Operation(
    summary = "Updates a global person restriction",
    description = "Updates a person restriction; the restriction is estate wide. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example restriction type does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Restriction or Person does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Person restrictions Id", example = "12345")
    @PathVariable
    personRestrictionId: Long,
    @RequestBody @Valid
    request: UpdateContactPersonRestrictionRequest,
  ) = contactPersonService.updatePersonRestriction(personId, personRestrictionId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/persons/{personId}/restriction/{personRestrictionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a global person restriction",
    description = "Deletes a person restriction; the restriction is estate wide. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Restrictions exists but not for this person",
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
  fun deletePersonRestriction(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Person restrictions Id", example = "12345")
    @PathVariable
    personRestrictionId: Long,
  ) = contactPersonService.deletePersonRestriction(personId, personRestrictionId)

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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/address/{addressId}")
  @Operation(
    summary = "Updates a person address",
    description = "Updates a person address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Address Updated",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonAddress(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Address Id", example = "47474")
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: UpdatePersonAddressRequest,
  ) = contactPersonService.updatePersonAddress(personId = personId, addressId = addressId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/persons/{personId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a person address",
    description = "Deletes a person address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "202",
        description = "Person Address Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The address exist but not for this person",
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
  fun deletePersonAddress(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Address Id", example = "47474")
    @PathVariable
    addressId: Long,
  ) = contactPersonService.deletePersonAddress(personId = personId, addressId = addressId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/email")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person email",
    description = "Creates a person email in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Email ID aka InternetAddressId Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonEmailResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  fun createPersonEmail(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonEmailRequest,
  ): CreatePersonEmailResponse = contactPersonService.createPersonEmail(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/email/{emailAddressId}")
  @Operation(
    summary = "Updates a person email",
    description = "Updates a person email in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Email ID aka InternetAddressId Update",
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
      ApiResponse(
        responseCode = "404",
        description = "Person or email address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonEmail(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Email address Id", example = "76554")
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    request: UpdatePersonEmailRequest,
  ) = contactPersonService.updatePersonEmail(personId = personId, emailAddressId = emailAddressId, request = request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/persons/{personId}/email/{emailAddressId}")
  @Operation(
    summary = "Deletes a person email",
    description = "Deletes a person email in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Email ID aka InternetAddressId Delete",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The email exist but not for this person",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or email address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deletePersonEmail(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Email address Id", example = "76554")
    @PathVariable
    emailAddressId: Long,
  ) = contactPersonService.deletePersonEmail(personId = personId, emailAddressId = emailAddressId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person global phone",
    description = "Creates a person global phone in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Phone ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonPhoneResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  fun createPersonPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonPhoneRequest,
  ): CreatePersonPhoneResponse = contactPersonService.createPersonPhone(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/phone/{phoneId}")
  @Operation(
    summary = "Updated a person global phone",
    description = "Updates a person global phone in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Phone ID Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or phone does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Phone Id", example = "35355")
    @PathVariable
    phoneId: Long,
    @RequestBody @Valid
    request: UpdatePersonPhoneRequest,
  ) = contactPersonService.updatePersonPhone(personId = personId, phoneId = phoneId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/persons/{personId}/phone/{phoneId}")
  @Operation(
    summary = "Deleted a person global phone",
    description = "Deletes a person global phone in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Phone ID Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Phone exists but not for this person",
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
  fun deletePersonPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Phone Id", example = "35355")
    @PathVariable
    phoneId: Long,
  ) = contactPersonService.deletePersonPhone(personId = personId, phoneId = phoneId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/address/{addressId}/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person address phone",
    description = "Creates a person phone associated with an address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Phone ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonPhoneResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createPersonAddressPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: CreatePersonPhoneRequest,
  ): CreatePersonPhoneResponse = contactPersonService.createPersonAddressPhone(
    personId = personId,
    addressId = addressId,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/address/{addressId}/phone/{phoneId}")
  @Operation(
    summary = "Updates a person address phone",
    description = "Updates a person phone associated with an address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Phone ID Updates",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatedPersonAddressPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @Schema(description = "Phone Id", example = "585850")
    @PathVariable
    phoneId: Long,
    @RequestBody @Valid
    request: UpdatePersonPhoneRequest,
  ) = contactPersonService.updatePersonAddressPhone(
    personId = personId,
    addressId = addressId,
    phoneId = phoneId,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/persons/{personId}/address/{addressId}/phone/{phoneId}")
  @Operation(
    summary = "Deletes a person address phone",
    description = "Deletes a person phone associated with an address in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Phone ID Deletes",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Phone exists but not for this address or address exists but not for this person",
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
  fun deletedPersonAddressPhone(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @Schema(description = "Phone Id", example = "585850")
    @PathVariable
    phoneId: Long,
  ) = contactPersonService.deletePersonAddressPhone(
    personId = personId,
    addressId = addressId,
    phoneId = phoneId,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/identifier")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person identifier",
    description = "Creates a person identifier in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Identifier sequence returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonIdentifierResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  fun createPersonIdentifier(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonIdentifierRequest,
  ): CreatePersonIdentifierResponse = contactPersonService.createPersonIdentifier(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/identifier/{sequence}")
  @Operation(
    summary = "Updates a person identifier",
    description = "Updates a person identifier in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Identifier updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or identifier does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonIdentifier(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Identifier sequence", example = "4")
    @PathVariable
    sequence: Long,
    @RequestBody @Valid
    request: UpdatePersonIdentifierRequest,
  ) = contactPersonService.updatePersonIdentifier(personId, sequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/persons/{personId}/identifier/{sequence}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a person identifier",
    description = "Deletes a person identifier in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Identifier deleted",
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
  fun deletePersonIdentifier(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Identifier sequence", example = "4")
    @PathVariable
    sequence: Long,
  ) = contactPersonService.deletePersonIdentifier(personId, sequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/persons/{personId}/employment")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a person employment",
    description = "Creates a person employment in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Person Employment sequence returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreatePersonEmploymentResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g corporate is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
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
  fun createPersonEmployment(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @RequestBody @Valid
    request: CreatePersonEmploymentRequest,
  ): CreatePersonEmploymentResponse = contactPersonService.createPersonEmployment(personId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/persons/{personId}/employment/{sequence}")
  @Operation(
    summary = "Updates a person employment",
    description = "Updates a person employment in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Employment updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request data, e.g type is not valid",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Person or employment does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updatePersonEmployment(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Employment sequence", example = "4")
    @PathVariable
    sequence: Long,
    @RequestBody @Valid
    request: UpdatePersonEmploymentRequest,
  ) = contactPersonService.updatePersonEmployment(personId, sequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/persons/{personId}/employment/{sequence}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a person employment",
    description = "Deletes a person employment in NOMIS. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Person Employment deleted",
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
  fun deletePersonEmployment(
    @Schema(description = "Person Id", example = "12345")
    @PathVariable
    personId: Long,
    @Schema(description = "Employment sequence", example = "4")
    @PathVariable
    sequence: Long,
  ) = contactPersonService.deletePersonEmployment(personId, sequence)
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
  @Schema(description = "Unique NOMIS sequence for the identifier for this contact")
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

data class PrisonerWithContacts(
  @Schema(description = "List of contacts for this prisoner")
  val contacts: List<PrisonerContact>,
)

@Schema(description = "The data held in NOMIS about a person's contact with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactForPerson(
  @Schema(description = "Unique NOMIS Id of person associated with the prisoner")
  val personId: Long,
  @Schema(description = "Last name of the person", example = "Smith")
  val lastName: String,
  @Schema(description = "First name of the person", example = "John")
  val firstName: String,
)

@Schema(description = "The data held in NOMIS about a prisoner's contact with a person")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerContact(
  @Schema(description = "Unique NOMIS sequence for the identifier for this contact")
  val id: Long,
  @Schema(description = "Unique NOMIS Id of booking associated with the contact")
  val bookingId: Long,
  @Schema(description = "Booking sequence this contact is related to. When 1 this indicates contact is for current term")
  val bookingSequence: Long,
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
  @Schema(description = "The person this prisoner is a contact for")
  val person: ContactForPerson,
  @Schema(description = "List of restrictions specifically between the prisoner and this contact")
  val restrictions: List<ContactRestriction>,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
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

@Schema(description = "Request to update an person (aka DPS contact) in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdatePersonRequest(
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
  @Schema(description = "Date the person dies")
  val deceasedDate: LocalDate? = null,
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

@Schema(description = "Request to update a contact (aka DPS prisoner contact) in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdatePersonContactRequest(
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

data class UpdatePersonAddressRequest(
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
  @Schema(description = "true if address validated by PAF")
  val validatedPAF: Boolean? = null,
)

data class CreatePersonAddressResponse(
  @Schema(description = "The address Id")
  val personAddressId: Long,
)

data class CreatePersonEmailRequest(
  @Schema(description = "Email address", example = "test@test.justice.gov.uk")
  val email: String,
)

data class UpdatePersonEmailRequest(
  @Schema(description = "Email address", example = "test@test.justice.gov.uk")
  val email: String,
)

data class CreatePersonEmailResponse(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
)

data class CreatePersonPhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class UpdatePersonPhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class CreatePersonPhoneResponse(
  @Schema(description = "Unique NOMIS Id of phone")
  val phoneId: Long,
)

data class CreatePersonIdentifierRequest(
  @Schema(description = "The identifier type code")
  val typeCode: String,
  @Schema(description = "The identifier value", example = "NE121212T")
  val identifier: String,
  @Schema(description = "The issued authority", example = "Police")
  val issuedAuthority: String? = null,
)

data class UpdatePersonIdentifierRequest(
  @Schema(description = "The identifier type code")
  val typeCode: String,
  @Schema(description = "The identifier value", example = "NE121212T")
  val identifier: String,
  @Schema(description = "The issued authority", example = "Police")
  val issuedAuthority: String? = null,
)

data class CreatePersonIdentifierResponse(
  @Schema(description = "Unique NOMIS sequence for this identifier for this person")
  val sequence: Long,
)

data class UpdatePersonEmploymentRequest(
  @Schema(description = "The id of the corporate organisation this employment is at")
  val corporateId: Long,
  @Schema(description = "True is employment is active")
  val active: Boolean,
)

data class CreatePersonEmploymentRequest(
  @Schema(description = "The id of the corporate organisation this employment is at")
  val corporateId: Long,
  @Schema(description = "True is employment is active")
  val active: Boolean,
)

data class CreatePersonEmploymentResponse(
  @Schema(description = "Unique NOMIS sequence for this employment for this person")
  val sequence: Long,
)

@Schema(description = "Request to create a contact restriction in NOMIS for either global or against a specific relationship")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateContactPersonRestrictionRequest(
  @Schema(description = "Restriction type")
  val typeCode: String,
  @Schema(description = "Free format comment text")
  val comment: String?,
  @Schema(description = "Date restriction became active")
  val effectiveDate: LocalDate,
  @Schema(description = "Date restriction is no longer active")
  val expiryDate: LocalDate?,
  @Schema(description = "Username Staff member who created the restriction")
  val enteredStaffUsername: String,
)

data class CreateContactPersonRestrictionResponse(
  @Schema(description = "Unique NOMIS Id of the restriction")
  val id: Long,
)

@Schema(description = "Request to update a contact restriction in NOMIS for either global or against a specific relationship")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateContactPersonRestrictionRequest(
  @Schema(description = "Restriction type")
  val typeCode: String,
  @Schema(description = "Free format comment text")
  val comment: String?,
  @Schema(description = "Date restriction became active")
  val effectiveDate: LocalDate,
  @Schema(description = "Date restriction is no longer active")
  val expiryDate: LocalDate?,
  @Schema(description = "Username Staff member who updated the restriction")
  val enteredStaffUsername: String,
)

data class PersonIdsWithLast(
  val personIds: List<Long>,
  val lastPersonId: Long,
)
