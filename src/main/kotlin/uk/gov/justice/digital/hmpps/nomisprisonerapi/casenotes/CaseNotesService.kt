package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.google.common.base.Utf8
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.WorkRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
@Transactional
class CaseNotesService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderCaseNoteRepository: OffenderCaseNoteRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val taskTypeRepository: ReferenceCodeRepository<TaskType>,
  private val taskSubTypeRepository: ReferenceCodeRepository<TaskSubType>,
  private val workRepository: WorkRepository,
) {
  private val seeDps = "... see DPS for full text"
  private val amendmentDateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
  private val dpsModules = listOf("PRISON_API", "ELITE2_API")

  private val pattern =
    // "<text> ...[<username> updated the case note[s] on <date> <time>] <amend text>"
    " \\.\\.\\.\\[(\\w+) updated the case notes? on (\\d{2,4}[/-]\\d{2}[/-]\\d{2,4}) (\\d{2}:\\d{2}:\\d{2})] "
      .toRegex()

  // Early e.g. ...[PQS23R updated the case note on 12/12/2006 07:32:39] letter sent 27/11/06
  // Middle era ...[GQV81R updated the case notes on 18-08-2009 14:04:53] ViSOR ref number: 09/0196494. (from about 2009 and still occurring)
  // late        ...[UQP87J updated the case notes on 2024/07/19 02:39:26] obs every hour (2018 onwards)
  private val dateTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("[dd/MM/yyyy][dd-MM-yyyy][yyyy/MM/dd] HH:mm:ss")

  fun getCaseNote(caseNoteId: Long): CaseNoteResponse =
    offenderCaseNoteRepository.findByIdOrNull(caseNoteId)?.toCaseNoteResponse()
      ?: throw NotFoundException("Case note not found for caseNoteId=$caseNoteId")

  @Audit
  fun createCaseNote(offenderNo: String, request: CreateCaseNoteRequest): CreateCaseNoteResponse {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found with a booking")

    val caseNoteType = taskTypeRepository.findByIdOrNull(TaskType.pk(request.caseNoteType))
      ?: throw BadDataException("CaseNote caseNoteType ${request.caseNoteType} is not valid")

    val caseNoteSubType = taskSubTypeRepository.findByIdOrNull(TaskSubType.pk(request.caseNoteSubType))
      ?: throw BadDataException("CaseNote caseNoteSubType ${request.caseNoteSubType} is not valid")

    validateTypes(caseNoteType, caseNoteSubType)

    val staffUserAccount =
      staffUserAccountRepository.findByUsername(request.authorUsername)
        ?: throw BadDataException("Username ${request.authorUsername} not found")

    val caseNote = offenderCaseNoteRepository.save(
      OffenderCaseNote(
        offenderBooking = offenderBooking,
        caseNoteType = caseNoteType,
        caseNoteSubType = caseNoteSubType,
        occurrenceDate = request.occurrenceDateTime.toLocalDate(),
        occurrenceDateTime = request.occurrenceDateTime,
        author = staffUserAccount.staff,
        agencyLocation = offenderBooking.location,
        caseNoteText = request.caseNoteText.truncate(),
        amendmentFlag = false,
        dateCreation = request.creationDateTime.truncatedTo(ChronoUnit.DAYS),
        timeCreation = request.creationDateTime,
        noteSourceCode = NoteSourceCode.INST,
      ),
    )
    return CreateCaseNoteResponse(caseNote.id, offenderBooking.bookingId)
  }

  @Audit
  fun updateCaseNote(caseNoteId: Long, request: UpdateCaseNoteRequest) {
    offenderCaseNoteRepository.findByIdOrNull<OffenderCaseNote, Long>(caseNoteId)
      ?.apply {
        caseNoteText = reconstructText(request)
      }
      ?: throw NotFoundException("Case note not found for caseNoteId=$caseNoteId")
  }

  @Audit
  fun deleteCaseNote(caseNoteId: Long) {
    offenderCaseNoteRepository.findByIdOrNull(caseNoteId)
      ?: throw NotFoundException("Case note not found for caseNoteId=$caseNoteId")

    offenderCaseNoteRepository.deleteById(caseNoteId)
  }

  private fun OffenderCaseNote.toCaseNoteResponse() = CaseNoteResponse(
    caseNoteId = id,
    bookingId = offenderBooking.bookingId,
    caseNoteType = caseNoteType.toCodeDescription(),
    caseNoteSubType = caseNoteSubType.toCodeDescription(),
    occurrenceDateTime = occurrenceDateTime,
    creationDateTime = timeCreation ?: dateCreation,
    authorUsername = author.accounts.first().username,
    // NB ^ omitted by the /{offenderNo}/case-notes/v2 endpoint
    authorStaffId = author.id,
    // see https://mojdt.slack.com/archives/C06G85DCF8T/p1726158063333349?thread_ts=1726156937.043299&cid=C06G85DCF8T
    authorFirstName = author.firstName,
    authorLastName = author.lastName,
    authorUsernames = author.accounts.map { it.username }.distinct(),
    prisonId = agencyLocation?.id ?: offenderBooking.location?.id,
    caseNoteText = parseMainText(caseNoteText),
    amendments = parseAmendments(this),
    noteSourceCode = noteSourceCode,
    createdDatetime = createdDatetime,
    createdUsername = createdUserId,
    auditModuleName = auditModuleName,
    sourceSystem = if (auditModuleName in dpsModules && (modifiedUserId == null || modifiedUserId == createdUserId)) {
      SourceSystem.DPS
    } else {
      SourceSystem.NOMIS
    },
  )

  internal fun parseMainText(caseNoteText: String): String {
    return pattern
      .find(caseNoteText)
      ?.let { caseNoteText.slice(0..it.range.first - 1) }
      ?: caseNoteText
  }

  internal fun parseAmendments(caseNote: OffenderCaseNote): List<CaseNoteAmendment> {
    val caseNoteText = caseNote.caseNoteText
    val matchResults = pattern.findAll(caseNoteText)
    val matchLastIndex = matchResults.count() - 1

    return matchResults.mapIndexed { index, matchResult ->

      val (user, date, time) = matchResult.destructured
      val startOfNext = if (matchLastIndex > index) {
        matchResults.elementAt(index + 1).range.first - 1
      } else {
        caseNoteText.length - 1
      }

      val amendText = caseNoteText.slice(matchResult.range.last + 1..startOfNext)
      val staff = staffUserAccountRepository.findByUsername(user)?.staff

      CaseNoteAmendment(
        text = amendText,
        authorUsername = user,
        authorStaffId = staff?.id,
        authorFirstName = staff?.firstName,
        authorLastName = staff?.lastName,
        createdDateTime = LocalDateTime.parse("$date $time", dateTimeFormat),
        sourceSystem = if (caseNote.auditModuleName in dpsModules && (caseNote.modifiedDatetime != null)) {
          SourceSystem.DPS
        } else {
          SourceSystem.NOMIS
        },
      )
    }.toList()
  }

  /**
   * For reconciliation or migration
   */
  fun getCaseNotes(offenderNo: String): PrisonerCaseNotesResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("offender $offenderNo not found")
    }

    return PrisonerCaseNotesResponse(
      offenderCaseNoteRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
        .map
        { it.toCaseNoteResponse() },
    )
  }

  private fun validateTypes(type: TaskType, subType: TaskSubType) {
    workRepository.findByWorkflowTypeAndWorkTypeAndWorkSubType("CNOTE", type.code, subType.code)
      ?: throw BadDataException("CNOTE (type,subtype)=(${type.code},${subType.code}) does not exist in the Works table")
  }

  internal fun reconstructText(request: UpdateCaseNoteRequest): String {
    var text = request.text
    request.amendments.forEach { amendment ->
      val timestamp = amendment.createdDateTime.format(amendmentDateTimeFormat)
      text += " ...[${amendment.authorUsername} updated the case notes on $timestamp] ${amendment.text}"
    }

    return text.truncate()
  }

  private fun String.truncate(): String =
    // encodedLength always >= length
    if (Utf8.encodedLength(this) <= MAX_CASENOTE_LENGTH_BYTES) {
      this
    } else {
      substring(0, MAX_CASENOTE_LENGTH_BYTES - (Utf8.encodedLength(this) - length) - seeDps.length) + seeDps
    }
}

private const val MAX_CASENOTE_LENGTH_BYTES: Int = 4000
