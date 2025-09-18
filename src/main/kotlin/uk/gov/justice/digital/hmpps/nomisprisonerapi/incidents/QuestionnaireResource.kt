package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/questionnaires"], produces = [MediaType.APPLICATION_JSON_VALUE])
class QuestionnaireResource(private val questionnaireService: QuestionnaireService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/ids")
  @Operation(
    summary = "get questionnaire IDs by filter",
    description = "Retrieves a paged list of incident questionnaire ids by filter. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids are returned",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdsByFilter(
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<QuestionnaireIdResponse> = questionnaireService.findIdsByFilter(
    pageRequest = pageRequest,
    QuestionnaireFilter(
      toDate = toDate,
      fromDate = fromDate,
    ),
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/{questionnaireId}")
  @Operation(
    summary = "Get incident questionnaire details",
    description = "Gets incident questionnaire details. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getQuestionnaire(
    @Schema(description = "Incident Questionnaire id") @PathVariable questionnaireId: Long,
  ) = questionnaireService.getQuestionnaire(questionnaireId)
}

@Schema(description = "Questionnaire")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class QuestionnaireResponse(
  @Schema(description = "The unique identifier of the questionnaire")
  val id: Long,
  @Schema(description = "A description of the questionnaire", example = "Escape from Establishment")
  val description: String?,
  @Schema(description = "Code to identify this questionnaire", example = "ESCAPE_EST")
  val code: String,
  @Schema(description = "If the questionnaire is active", example = "true")
  val active: Boolean,
  @Schema(description = "Sequence value of the questionnaires", example = "1")
  val listSequence: Int,
  @Schema(description = "List of Questions (and associated Answers) for this Questionnaire")
  val questions: List<QuestionResponse> = listOf(),
  @Schema(description = "List of Roles allowed for an offender's participation in an incident")
  val offenderRoles: List<String> = listOf(),
  @Schema(description = "The date the questionnaire is no longer used")
  val expiryDate: LocalDate? = null,
  @Schema(description = "Questionnaire created date", required = true)
  val createdDate: LocalDateTime,
  @Schema(description = "Questionnaire created by", required = true)
  val createdBy: String,
  @Schema(description = "Questionnaire modified date")
  val modifiedDate: LocalDateTime? = null,
  @Schema(description = "Questionnaire modified by")
  val modifiedBy: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NextQuestionResponse(
  @Schema(description = "The question id")
  val id: Long,
  @Schema(description = "The question text")
  val question: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QuestionResponse(
  @Schema(description = "The question id")
  val id: Long,
  @Schema(description = "The question text")
  val question: String,
  @Schema(description = "List of Answers to this question")
  val answers: List<AnswerResponse> = listOf(),
  @Schema(description = "If the question is active", example = "true")
  val active: Boolean,
  @Schema(description = "The date the question is no longer used")
  val expiryDate: LocalDate? = null,
  @Schema(description = "If the question has multiple answers", example = "true")
  val multipleAnswers: Boolean,
  @Schema(description = "The question id used to set the listSequence values", example = "1")
  val questionSequence: Int,
  @Schema(description = "The order of the questions", example = "1")
  val listSequence: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnswerResponse(
  @Schema(description = "The answer id")
  val id: Long,
  @Schema(description = "The answer text")
  val answer: String,
  @Schema(description = "The answer id used to set the listSequence values", example = "1")
  val answerSequence: Int,
  @Schema(description = "The order of the answers", example = "1")
  val listSequence: Int,
  @Schema(description = "If the answer is active", example = "true")
  val active: Boolean,
  @Schema(description = "The date the answer is no longer used")
  val expiryDate: LocalDate? = null,
  @Schema(description = "Question to be asked following this answer")
  var nextQuestion: NextQuestionResponse? = null,
  @Schema(description = "If the answer should include a date", example = "true")
  val dateRequired: Boolean,
  @Schema(description = "If the answer should include a comment", example = "true")
  val commentRequired: Boolean,
)
