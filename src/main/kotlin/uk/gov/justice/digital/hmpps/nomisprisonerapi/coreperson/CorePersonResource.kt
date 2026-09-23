package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RequestMapping("/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonResource(private val corePersonService: CorePersonService) {
  @GetMapping("/{prisonNumber}")
  @Operation(
    summary = "Get an offender by prison number",
    description = "Retrieves an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core person information returned",
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
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ): CorePerson = corePersonService.getOffender(prisonNumber)

  @GetMapping("{offenderId}/identifier/{sequenceNumber}")
  @Operation(
    summary = "Get an identifier by offender id and sequence number",
    description = "Retrieves an offender identifier. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Identifier returned",
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
        description = "Identifier or offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdentifier(
    @Schema(description = "The offender id", example = "1234567")
    @PathVariable offenderId: Long,
    @Schema(description = "The sequence number", example = "3")
    @PathVariable sequenceNumber: Int,
  ) = corePersonService.getIdentifier(offenderId, sequenceNumber)

  @GetMapping("alias/{offenderId}")
  @Operation(
    summary = "Get an alias by offender id",
    description = "Retrieves an alias. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alias returned",
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
        description = "Alias does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlias(
    @Schema(description = "The offender id", example = "1234567")
    @PathVariable offenderId: Long,
  ): CoreOffender = corePersonService.getAlias(offenderId)

  @GetMapping("{prisonNumber}/aliases-identifiers")
  @Operation(
    summary = "Get the aliases and identifiers for an offender by prison number",
    description = "Retrieves the aliases and offenders for an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Identifier returned",
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
  fun getOffenderAliasesAndIdentifiers(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ) = corePersonService.getOffenderAliasesAndIdentifiers(prisonNumber)

  @GetMapping("/{prisonNumber}/religions")
  @Operation(
    summary = "Get all the religion information for an offender by prison number",
    description = "Retrieves the religion information for an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core religion information returned",
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
  fun getOffenderReligionsByPrisonNumber(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ): List<OffenderBelief> = corePersonService.getOffenderReligions(prisonNumber)

  @GetMapping("/{prisonNumber}/addresses-contacts")
  @Operation(
    summary = "Get all the address and contact information for an offender by prison number",
    description = "Retrieves the address and contact information for an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core addresses and contacts returned",
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
  fun getOffenderAddressesAndContactsByPrisonNumber(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ): CorePersonAddressContact = corePersonService.getAddressesAndContacts(prisonNumber)

  @PostMapping("/{prisonNumber}/merge")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update the offender by prison number as a result of a merge",
    description = "Updates the offender information as a result of a merge. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
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
  fun updateOffenderByPrisonNumberAfterMerge(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
    @RequestBody @Valid
    request: CorePersonMergeRequest,
  ) {
    corePersonService.updateOffenderAfterMerge(prisonNumber, request)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/{offenderId}/email")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an offender email",
    description = "Creates an offender email in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Offender Email ID aka InternetAddressId Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateOffenderEmailResponse::class),
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
  fun createOffenderEmail(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @RequestBody @Valid
    request: CreateOffenderEmailRequest,
  ): CreateOffenderEmailResponse = corePersonService.createOffenderEmail(offenderId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/{offenderId}/email/{emailAddressId}")
  @Operation(
    summary = "Updates an offender email",
    description = "Updates an offender email in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender Email ID aka InternetAddressId Update",
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
        description = "Offender or email address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateOffenderEmail(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Email address Id", example = "76554")
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    request: UpdateOffenderEmailRequest,
  ) = corePersonService.updateOffenderEmail(offenderId = offenderId, emailAddressId = emailAddressId, request = request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{offenderId}/email/{emailAddressId}")
  @Operation(
    summary = "Deletes an offender email",
    description = "Deletes an offender email in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender Email ID aka InternetAddressId Delete",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The email exists but not for this offender",
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
        description = "Offender or email address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteOffenderEmail(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Email address Id", example = "76554")
    @PathVariable
    emailAddressId: Long,
  ) = corePersonService.deleteOffenderEmail(offenderId = offenderId, emailAddressId = emailAddressId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/{offenderId}/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an offender global phone",
    description = "Creates an offender global phone in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Offender Phone ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateOffenderPhoneResponse::class),
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
  fun createOffenderPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @RequestBody @Valid
    request: CreateOffenderPhoneRequest,
  ): CreateOffenderPhoneResponse = corePersonService.createOffenderPhone(offenderId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/{offenderId}/phone/{phoneId}")
  @Operation(
    summary = "Updated an offender global phone",
    description = "Updates an offender global phone in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender Phone ID Updated",
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
        description = "Offender or phone does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateOffenderPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Phone Id", example = "35355")
    @PathVariable
    phoneId: Long,
    @RequestBody @Valid
    request: UpdateOffenderPhoneRequest,
  ) = corePersonService.updateOffenderPhone(offenderId = offenderId, phoneId = phoneId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{offenderId}/phone/{phoneId}")
  @Operation(
    summary = "Deleted an offender global phone",
    description = "Deletes an offender global phone in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender Phone ID Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Phone exists but not for this offender",
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
  fun deleteOffenderPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Phone Id", example = "35355")
    @PathVariable
    phoneId: Long,
  ) = corePersonService.deleteOffenderPhone(offenderId = offenderId, phoneId = phoneId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/{offenderId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an offender address",
    description = "Creates an offender address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Offender Address ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateOffenderAddressResponse::class),
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
  fun createOffenderAddress(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @RequestBody @Valid
    request: CreateOffenderAddressRequest,
  ): CreateOffenderAddressResponse = corePersonService.createOffenderAddress(offenderId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/{offenderId}/address/{addressId}")
  @Operation(
    summary = "Updates an offender address",
    description = "Updates an offender address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender Address Updated",
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
        description = "Offender or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateOffenderAddress(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Address Id", example = "47474")
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: UpdateOffenderAddressRequest,
  ) = corePersonService.updateOffenderAddress(offenderId = offenderId, addressId = addressId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/{offenderId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes an offender address",
    description = "Deletes an offender address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender Address Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The address exists but not for this offender",
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
  fun deleteOffenderAddress(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Address Id", example = "47474")
    @PathVariable
    addressId: Long,
  ) = corePersonService.deleteOffenderAddress(offenderId = offenderId, addressId = addressId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/{offenderId}/address/{addressId}/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an offender address phone",
    description = "Creates an offender phone associated with an address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Offender Phone ID Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateOffenderPhoneResponse::class),
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
        description = "Offender or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createOffenderAddressPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: CreateOffenderPhoneRequest,
  ): CreateOffenderPhoneResponse = corePersonService.createOffenderAddressPhone(
    offenderId = offenderId,
    addressId = addressId,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/{offenderId}/address/{addressId}/phone/{phoneId}")
  @Operation(
    summary = "Updates an offender address phone",
    description = "Updates an offender phone associated with an address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender Phone ID Updated",
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
        description = "Offender or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateOffenderAddressPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @Schema(description = "Phone Id", example = "585850")
    @PathVariable
    phoneId: Long,
    @RequestBody @Valid
    request: UpdateOffenderPhoneRequest,
  ) = corePersonService.updateOffenderAddressPhone(
    offenderId = offenderId,
    addressId = addressId,
    phoneId = phoneId,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{offenderId}/address/{addressId}/phone/{phoneId}")
  @Operation(
    summary = "Deletes an offender address phone",
    description = "Deletes an offender phone associated with an address in NOMIS. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender Phone ID Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Phone exists but not for this address or address exists but not for this offender",
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
  fun deleteOffenderAddressPhone(
    @Schema(description = "Offender Id", example = "12345")
    @PathVariable
    offenderId: Long,
    @Schema(description = "Address Id", example = "56789")
    @PathVariable
    addressId: Long,
    @Schema(description = "Phone Id", example = "585850")
    @PathVariable
    phoneId: Long,
  ) = corePersonService.deleteOffenderAddressPhone(
    offenderId = offenderId,
    addressId = addressId,
    phoneId = phoneId,
  )
}

@Schema(description = "The data held in NOMIS for an offender")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CorePerson(
  @Schema(description = "The prison number")
  val prisonNumber: String,
  @Schema(description = "In/Out Status", example = "IN, OUT, TRN")
  val inOutStatus: String?,
  @Schema(description = "Indicates that the person is currently in prison")
  val activeFlag: Boolean,
  @Schema(description = "List of offender records for the person")
  val offenders: List<CoreOffender>?,
  @Schema(description = "Current belief and history of all beliefs for the person")
  val beliefs: List<OffenderBelief>?,
  @Schema(description = "List of addresses for the person")
  val addresses: List<OffenderAddress>?,
  @Schema(description = "List of phone numbers for the person")
  val phoneNumbers: List<OffenderPhoneNumber>?,
  @Schema(description = "List of email addresses for the person")
  val emailAddresses: List<OffenderEmailAddress>?,
)

@Schema(description = "The data held in NOMIS for an offender")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CorePersonAddressContact(
  @Schema(description = "List of addresses for the person")
  val addresses: List<OffenderAddress>?,
  @Schema(description = "List of phone numbers for the person")
  val phoneNumbers: List<OffenderPhoneNumber>?,
  @Schema(description = "List of email addresses for the person")
  val emailAddresses: List<OffenderEmailAddress>?,
)

@Schema(description = "The data held in NOMIS for an offender.")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoreOffender(
  @Schema(description = "The offender id")
  val offenderId: Long,
  @Schema(description = "Title of this offender record")
  val title: CodeDescription?,
  @Schema(description = "First name of this offender record")
  val firstName: String,
  @Schema(description = "Middle name of this offender record")
  val middleName1: String?,
  @Schema(description = "Second middle name of this offender record")
  val middleName2: String?,
  @Schema(description = "Surname name of this offender record")
  val lastName: String,
  @Schema(description = "Date of birth of this offender record")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Birth place of this offender record")
  val birthPlace: String?,
  @Schema(description = "Birth country of this offender record")
  val birthCountry: CodeDescription?,
  @Schema(description = "Race of this offender record")
  val ethnicity: CodeDescription?,
  @Schema(description = "Sex of this offender record")
  val sex: CodeDescription?,
  @Schema(description = "Name type of this offender record")
  val nameType: CodeDescription?,
  @Schema(description = "Date this offender record was created. This is separate from the CREATE_DATETIME audit column.")
  val createDate: LocalDate?,
  @Schema(description = "The offender record associated with the current booking")
  val workingName: Boolean,
  @Schema(description = "List of identifiers for the offender")
  val identifiers: List<Identifier>,
)

@Schema(description = "The data held in NOMIS for an offender's identifiers")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Identifier(
  @Schema(description = "The offender id")
  val offenderId: Long,
  @Schema(description = "Unique NOMIS sequence for this identifier for this person")
  val sequence: Long,
  @Schema(description = "The identifier type")
  val type: CodeDescription,
  @Schema(description = "The identifier value", example = "NE121212T")
  val identifier: String,
  @Schema(description = "The issued authority", example = "Police")
  val issuedAuthority: String?,
  @Schema(description = "The issued date")
  val issuedDate: LocalDate?,
  @Schema(description = "Verified")
  val verified: Boolean,
)

@Schema(description = "The data held in NOMIS about an address")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OffenderAddress(
  @Schema(description = "Unique NOMIS ID of the address")
  val addressId: Long,
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
  val phoneNumbers: List<OffenderPhoneNumber>?,
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
  @Schema(description = "Usages for the address, also known as types")
  val usages: List<OffenderAddressUsage>?,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
  @Schema(description = "Date time when the record was last updated the record in NOMIS", required = true)
  val lastUpdatedDateTime: LocalDateTime?,
  @Schema(description = "Username of person who last updated the record in NOMIS", required = true)
  val lastUpdatedByUsername: String?,
)

@Schema(description = "Offender address usage")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderAddressUsage(
  @Schema(description = "Offender address id", example = "1123456")
  val addressId: Long,
  @Schema(description = "Address usage")
  val usage: CodeDescription,
  @Schema(description = "Whether the address usage is active")
  val active: Boolean,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
  @Schema(description = "Date time when the record was last updated the record in NOMIS", required = true)
  val lastUpdatedDateTime: LocalDateTime?,
  @Schema(description = "Username of person who last updated the record in NOMIS", required = true)
  val lastUpdatedByUsername: String?,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderPhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val phoneId: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
  @Schema(description = "Date time when the record was last updated the record in NOMIS", required = true)
  val lastUpdatedDateTime: LocalDateTime?,
  @Schema(description = "Username of person who last updated the record in NOMIS", required = true)
  val lastUpdatedByUsername: String?,
)

@Schema(description = "The data held in NOMIS about an email address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderEmailAddress(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val email: String,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
  @Schema(description = "Date time when the record was last updated the record in NOMIS", required = true)
  val lastUpdatedDateTime: LocalDateTime?,
  @Schema(description = "Username of person who last updated the record in NOMIS", required = true)
  val lastUpdatedByUsername: String?,
)

data class CreateOffenderEmailRequest(
  @Schema(description = "Email address", example = "test@test.justice.gov.uk")
  val email: String,
)

data class UpdateOffenderEmailRequest(
  @Schema(description = "Email address", example = "test@test.justice.gov.uk")
  val email: String,
)

data class CreateOffenderEmailResponse(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
)

data class CreateOffenderPhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class UpdateOffenderPhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class CreateOffenderPhoneResponse(
  @Schema(description = "Unique NOMIS Id of phone")
  val phoneId: Long,
)

data class CreateOffenderAddressRequest(
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
  @Schema(description = "true if this is the offender's primary address")
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

data class UpdateOffenderAddressRequest(
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
  @Schema(description = "true if this is the offender's primary address")
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

data class CreateOffenderAddressResponse(
  @Schema(description = "The address Id")
  val addressId: Long,
)

@Schema(description = "Offender beliefs")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderBelief(
  @Schema(description = "Offender belief id", example = "1123456")
  val beliefId: Long,
  @Schema(description = "Belief", example = "SCIE")
  val belief: CodeDescription,
  @Schema(description = "Date the belief started", example = "2024-01-01")
  val startDate: LocalDate,
  @Schema(description = "Date the belief ended", example = "2024-12-12")
  val endDate: LocalDate? = null,
  @Schema(description = "Was a reason given for change of belief?")
  val changeReason: Boolean? = null,
  @Schema(description = "Comments describing reason for change of belief")
  val comments: String? = null,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Update request for offender merge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorePersonMergeRequest(
  @Schema(description = "List of religions to be updated for the offender after a merge")
  val religions: List<CorePersonReligionRequest>,
)

@Schema(description = "Update request for offender belief merge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorePersonReligionRequest(
  @Schema(description = "Offender belief id", example = "1123456")
  val beliefId: Long,
  @Schema(description = "Date the belief ended", example = "2024-12-12")
  val endDate: LocalDate? = null,
)
