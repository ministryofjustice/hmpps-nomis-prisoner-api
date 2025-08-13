package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DslMarker
annotation class OffenderCaseNoteDslMarker

@NomisDataDslMarker
interface OffenderCaseNoteDsl

@Component
class OffenderCaseNoteBuilderFactory(
  private val repository: OffenderCaseNoteBuilderRepository,
) {
  fun builder(): OffenderCaseNoteBuilder = OffenderCaseNoteBuilder(repository)
}

@Component
class OffenderCaseNoteBuilderRepository(
  private val repository: OffenderCaseNoteRepository,
  private val taskTypeRepository: ReferenceCodeRepository<TaskType>,
  private val taskSubTypeRepository: ReferenceCodeRepository<TaskSubType>,
) {
  fun save(casenote: OffenderCaseNote): OffenderCaseNote = repository.saveAndFlush(casenote)

  fun lookupTaskType(code: String): TaskType = taskTypeRepository.findByIdOrNull(TaskType.pk(code))!!

  fun lookupTaskSubType(code: String): TaskSubType = taskSubTypeRepository.findByIdOrNull(TaskSubType.pk(code))!!
}

class OffenderCaseNoteBuilder(
  private val repository: OffenderCaseNoteBuilderRepository,
) : OffenderCaseNoteDsl {
  private lateinit var casenote: OffenderCaseNote

  fun build(
    offenderBooking: OffenderBooking,
    caseNoteType: String,
    caseNoteSubType: String,
    date: LocalDateTime,
    author: Staff,
    caseNoteText: String,
    amendmentFlag: Boolean,
    noteSourceCode: NoteSourceCode,
    // TODO provide dateCreation too!
    timeCreation: LocalDateTime?,
  ): OffenderCaseNote = OffenderCaseNote(
    offenderBooking = offenderBooking,
    occurrenceDate = date.toLocalDate(),
    occurrenceDateTime = date,
    caseNoteType = repository.lookupTaskType(caseNoteType),
    caseNoteSubType = repository.lookupTaskSubType(caseNoteSubType),
    author = author,
    agencyLocation = offenderBooking.location,
    caseNoteText = caseNoteText,
    amendmentFlag = amendmentFlag,
    noteSourceCode = noteSourceCode,
    dateCreation = (timeCreation ?: date).truncatedTo(ChronoUnit.DAYS),
    timeCreation = timeCreation,
    auditModuleName = "A_MODULE",
    createdDatetime = date,
    createdUserId = "username",
  )
    .let { repository.save(it) }
    .also { casenote = it }
}
