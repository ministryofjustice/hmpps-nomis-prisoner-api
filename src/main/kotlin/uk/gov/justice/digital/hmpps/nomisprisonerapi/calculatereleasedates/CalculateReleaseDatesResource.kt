package uk.gov.justice.digital.hmpps.nomisprisonerapi.calculatereleasedates

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceCalculationRepository
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "401",
      description = "Unauthorized to access this endpoint",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Missing required role. Requires the ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW role",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  ],
)
class CalculateReleaseDatesResource(private val service: CalculateReleaseDatesService) {

  @GetMapping("/sentence-calculations/{offenderSentenceCalculationId}")
  @Operation(
    summary = "Gets a single offender sentence calculation by id",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender sentence calculation",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = OffenderSentenceCalculationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender sentence calculation not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getOffenderSentenceCalculation(
    @PathVariable
    @Schema(description = "The offender sentence calculation id", example = "1234567")
    offenderSentenceCalculationId: Long,
  ): OffenderSentenceCalculationResponse = service.getOffenderSentenceCalculation(offenderSentenceCalculationId)
}

@Service
@Transactional(readOnly = true)
class CalculateReleaseDatesService(
  private val offenderSentenceCalculationRepository: OffenderSentenceCalculationRepository,
) {
  fun getOffenderSentenceCalculation(offenderSentenceCalculationId: Long): OffenderSentenceCalculationResponse = offenderSentenceCalculationRepository.findByIdOrNull(offenderSentenceCalculationId)?.toResponse()
    ?: throw NotFoundException("Offender sentence calculation with id $offenderSentenceCalculationId not found")
}

private fun OffenderSentenceCalculation.toResponse() = OffenderSentenceCalculationResponse(
  id = id,
  bookingId = offenderBooking.bookingId,
  offenderNo = offenderBooking.offender.nomsId,
  calculationDate = calculationDate,
  staffId = staff?.id,
  calculationReasonCode = calculationReasonCode,
  commentText = commentText,
  effectiveSentenceEndDate = effectiveSentenceEndDate,
  effectiveSentenceLength = effectiveSentenceLength,
  judiciallyImposedSentenceLength = judiciallyImposedSentenceLength,
  workflowHistoryId = workflowHistoryId,
  hdcedCalculatedDate = hdcedCalculatedDate,
  hdcedOverridedDate = hdcedOverridedDate,
  hdcadCalculatedDate = hdcadCalculatedDate,
  hdcadOverridedDate = hdcadOverridedDate,
  etdCalculatedDate = etdCalculatedDate,
  etdOverridedDate = etdOverridedDate,
  mtdCalculatedDate = mtdCalculatedDate,
  mtdOverridedDate = mtdOverridedDate,
  ltdCalculatedDate = ltdCalculatedDate,
  ltdOverridedDate = ltdOverridedDate,
  ardCalculatedDate = ardCalculatedDate,
  ardOverridedDate = ardOverridedDate,
  crdCalculatedDate = crdCalculatedDate,
  crdOverridedDate = crdOverridedDate,
  pedCalculatedDate = pedCalculatedDate,
  pedOverridedDate = pedOverridedDate,
  apdCalculatedDate = apdCalculatedDate,
  apdOverridedDate = apdOverridedDate,
  npdCalculatedDate = npdCalculatedDate,
  npdOverridedDate = npdOverridedDate,
  ledCalculatedDate = ledCalculatedDate,
  ledOverridedDate = ledOverridedDate,
  sedCalculatedDate = sedCalculatedDate,
  sedOverridedDate = sedOverridedDate,
  prrdCalculatedDate = prrdCalculatedDate,
  prrdOverridedDate = prrdOverridedDate,
  tariffCalculatedDate = tariffCalculatedDate,
  tariffOverridedDate = tariffOverridedDate,
  dprrdCalculatedDate = dprrdCalculatedDate,
  dprrdOverridedDate = dprrdOverridedDate,
  ersedOverridedDate = ersedOverridedDate,
  tersedOverridedDate = tersedOverridedDate,
  rotlOverridedDate = rotlOverridedDate,
  tusedCalculatedDate = tusedCalculatedDate,
  tusedOverridedDate = tusedOverridedDate,
  recordDatetime = recordDatetime,
  recordUserId = recordUserId,
  createDatetime = createDatetime,
  createUsername = createUsername,
  modifyDatetime = modifyDatetime,
  modifyUserId = modifyUserId,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "An offender sentence calculation (release dates calculation) for a booking")
data class OffenderSentenceCalculationResponse(
  @Schema(description = "The offender sentence calculation id")
  val id: Long,
  @Schema(description = "The booking id the calculation belongs to")
  val bookingId: Long,
  @Schema(description = "The NOMIS id of the offender", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "When the calculation was done")
  val calculationDate: LocalDateTime,
  @Schema(description = "The staff id of the user that did the calculation")
  val staffId: Long? = null,
  @Schema(description = "The reason for the calculation", example = "NEW")
  val calculationReasonCode: String,
  @Schema(description = "Free text comment")
  val commentText: String? = null,
  @Schema(description = "Effective sentence end date")
  val effectiveSentenceEndDate: LocalDate? = null,
  @Schema(description = "Effective sentence length in the format YY/MM/DD", example = "02/06/00")
  val effectiveSentenceLength: String? = null,
  @Schema(description = "Judicially imposed sentence length in the format YY/MM/DD", example = "02/06/00")
  val judiciallyImposedSentenceLength: String? = null,
  @Schema(description = "Workflow history id")
  val workflowHistoryId: Long? = null,

  @Schema(description = "Home Detention Curfew Eligibility Date - calculated")
  val hdcedCalculatedDate: LocalDate? = null,
  @Schema(description = "Home Detention Curfew Eligibility Date - overridden")
  val hdcedOverridedDate: LocalDate? = null,
  @Schema(description = "Home Detention Curfew Approved Date - calculated")
  val hdcadCalculatedDate: LocalDate? = null,
  @Schema(description = "Home Detention Curfew Approved Date - overridden")
  val hdcadOverridedDate: LocalDate? = null,
  @Schema(description = "Earliest Tariff Date - calculated")
  val etdCalculatedDate: LocalDate? = null,
  @Schema(description = "Earliest Tariff Date - overridden")
  val etdOverridedDate: LocalDate? = null,
  @Schema(description = "Mid Tariff Date - calculated")
  val mtdCalculatedDate: LocalDate? = null,
  @Schema(description = "Mid Tariff Date - overridden")
  val mtdOverridedDate: LocalDate? = null,
  @Schema(description = "Latest Tariff Date - calculated")
  val ltdCalculatedDate: LocalDate? = null,
  @Schema(description = "Latest Tariff Date - overridden")
  val ltdOverridedDate: LocalDate? = null,
  @Schema(description = "Automatic Release Date - calculated")
  val ardCalculatedDate: LocalDate? = null,
  @Schema(description = "Automatic Release Date - overridden")
  val ardOverridedDate: LocalDate? = null,
  @Schema(description = "Conditional Release Date - calculated")
  val crdCalculatedDate: LocalDate? = null,
  @Schema(description = "Conditional Release Date - overridden")
  val crdOverridedDate: LocalDate? = null,
  @Schema(description = "Parole Eligibility Date - calculated")
  val pedCalculatedDate: LocalDate? = null,
  @Schema(description = "Parole Eligibility Date - overridden")
  val pedOverridedDate: LocalDate? = null,
  @Schema(description = "Approved Parole Date - calculated")
  val apdCalculatedDate: LocalDate? = null,
  @Schema(description = "Approved Parole Date - overridden")
  val apdOverridedDate: LocalDate? = null,
  @Schema(description = "Non Parole Date - calculated")
  val npdCalculatedDate: LocalDate? = null,
  @Schema(description = "Non Parole Date - overridden")
  val npdOverridedDate: LocalDate? = null,
  @Schema(description = "Licence Expiry Date - calculated")
  val ledCalculatedDate: LocalDate? = null,
  @Schema(description = "Licence Expiry Date - overridden")
  val ledOverridedDate: LocalDate? = null,
  @Schema(description = "Sentence Expiry Date - calculated")
  val sedCalculatedDate: LocalDate? = null,
  @Schema(description = "Sentence Expiry Date - overridden")
  val sedOverridedDate: LocalDate? = null,
  @Schema(description = "Post Recall Release Date - calculated")
  val prrdCalculatedDate: LocalDate? = null,
  @Schema(description = "Post Recall Release Date - overridden")
  val prrdOverridedDate: LocalDate? = null,
  @Schema(description = "Tariff Date - calculated")
  val tariffCalculatedDate: LocalDate? = null,
  @Schema(description = "Tariff Date - overridden")
  val tariffOverridedDate: LocalDate? = null,
  @Schema(description = "DTO Post Recall Release Date - calculated")
  val dprrdCalculatedDate: LocalDate? = null,
  @Schema(description = "DTO Post Recall Release Date - overridden")
  val dprrdOverridedDate: LocalDate? = null,
  @Schema(description = "Early Removal Scheme Eligibility Date - overridden")
  val ersedOverridedDate: LocalDate? = null,
  @Schema(description = "Tariff Expiry Removal Scheme Date - overridden")
  val tersedOverridedDate: LocalDate? = null,
  @Schema(description = "Release on Temporary Licence Date - overridden")
  val rotlOverridedDate: LocalDate? = null,
  @Schema(description = "Top Up Supervision Expiry Date - calculated")
  val tusedCalculatedDate: LocalDate? = null,
  @Schema(description = "Top Up Supervision Expiry Date - overridden")
  val tusedOverridedDate: LocalDate? = null,

  @Schema(description = "Date the record was recorded")
  val recordDatetime: LocalDateTime,
  @Schema(description = "User that recorded the record")
  val recordUserId: String? = null,
  @Schema(description = "When the record was created")
  val createDatetime: LocalDateTime,
  @Schema(description = "User that created the record")
  val createUsername: String,
  @Schema(description = "When the record was last modified")
  val modifyDatetime: LocalDateTime? = null,
  @Schema(description = "User that last modified the record")
  val modifyUserId: String? = null,
)
