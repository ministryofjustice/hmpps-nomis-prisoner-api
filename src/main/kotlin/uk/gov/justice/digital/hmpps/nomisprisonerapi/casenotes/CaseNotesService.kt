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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class CaseNotesService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderCaseNoteRepository: OffenderCaseNoteRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val taskTypeRepository: ReferenceCodeRepository<TaskType>,
  private val taskSubTypeRepository: ReferenceCodeRepository<TaskSubType>,
) {
  fun getCaseNote(caseNoteId: Long): CaseNoteResponse =
    offenderCaseNoteRepository.findByIdOrNull(caseNoteId)?.toCaseNoteResponse()
      ?: throw NotFoundException("Case note not found for caseNoteId=$caseNoteId")

  @Audit
  fun createCaseNote(offenderNo: String, request: CreateCaseNoteRequest): CreateCaseNoteResponse {
    validateTextLength(request.caseNoteText)

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
        caseNoteText = request.caseNoteText,
        amendmentFlag = false,
        // Use date/timeCreation rather than createdDatetime. both provided for now
        dateCreation = request.occurrenceDateTime.toLocalDate(),
        timeCreation = request.occurrenceDateTime,
        createdDatetime = request.occurrenceDateTime,
        createdUserId = staffUserAccount.username,
        noteSourceCode = NoteSourceCode.INST,
      ),
    )
    return CreateCaseNoteResponse(caseNote.id, offenderBooking.bookingId)
  }

  @Audit
  fun amendCaseNote(caseNoteId: Long, request: AmendCaseNoteRequest): CaseNoteResponse {
    validateTextLength(request.caseNoteText)

    val caseNote = offenderCaseNoteRepository.findByIdOrNull(caseNoteId)
      ?: throw NotFoundException("Case note not found for caseNoteId=$caseNoteId")

    val caseNoteType = taskTypeRepository.findByIdOrNull(TaskType.pk(request.caseNoteType))
      ?: throw BadDataException("CaseNote caseNoteType ${request.caseNoteType} is not valid")

    val caseNoteSubType = taskSubTypeRepository.findByIdOrNull(TaskSubType.pk(request.caseNoteSubType))
      ?: throw BadDataException("CaseNote caseNoteSubType ${request.caseNoteSubType} is not valid")

    validateTypes(caseNoteType, caseNoteSubType)

    staffUserAccountRepository.findByUsername(request.authorUsername)
      ?: throw BadDataException("Username ${request.authorUsername} not found")

    caseNote.caseNoteText = request.caseNoteText
    // TODO do we want to do an 'amend' as in appending text as at present?

    return caseNote.toCaseNoteResponse()
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
    authorUsername = author.accounts.first().username,
    // NB ^ omitted by the /{offenderNo}/case-notes/v2 endpoint
    authorStaffId = author.id,
    // see https://mojdt.slack.com/archives/C06G85DCF8T/p1726158063333349?thread_ts=1726156937.043299&cid=C06G85DCF8T
    authorFirstName = author.firstName,
    authorLastName = author.lastName,
    prisonId = agencyLocation?.id ?: offenderBooking.location?.id,
    caseNoteText = parseMainText(caseNoteText),
    amendments = parseAmendments(caseNoteText),
    noteSourceCode = noteSourceCode,
    createdDatetime = LocalDateTime.of(dateCreation, timeCreation?.toLocalTime() ?: LocalTime.MIDNIGHT),
    createdUsername = createdUserId,
    auditModuleName = auditModuleName,
  )

  val pattern =
    // "<text> ...[<username> updated the case notes on <date> <time>] <amend text>"
    " \\.\\.\\.\\[([A-Za-z0-9_]+) updated the case notes on (\\d{4}/\\d{2}/\\d{2}) (\\d{2}:\\d{2}:\\d{2})] "
      .toRegex()

  val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

  internal fun parseMainText(caseNoteText: String): String {
    return pattern
      .find(caseNoteText)
      ?.let { caseNoteText.slice(0..it.range.first - 1) }
      ?: caseNoteText
  }

  internal fun parseAmendments(caseNoteText: String): List<CaseNoteAmendment> {
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
      )
    }.toList()
  }

  /**
   * For reconciliation or migration
   */
  fun getCaseNotes(offenderNo: String): PrisonerCaseNotesResponse {
    offenderRepository.findByNomsId(offenderNo).ifEmpty {
      throw NotFoundException("offender $offenderNo not found")
    }

    return PrisonerCaseNotesResponse(
      offenderCaseNoteRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
        .map
        { it.toCaseNoteResponse() },
    )
  }

  private fun validateTextLength(value: String) {
    if (value.length >= CHECK_THRESHOLD || Utf8.encodedLength(value) > MAX_CASENOTE_LENGTH_BYTES) {
      throw BadDataException("Case note text too long")
    }
  }

  private fun validateTypes(type: TaskType, subType: TaskSubType) {
    if (subType.parentCode != type.code) {
      throw BadDataException("CaseNote (type,subtype)=(${type.code},${subType.code}) does not exist")
    }
    // TODO do we need to validate the user's caseload in table WORKS? see CaseNoteTypeSubTypeValidator in prison-api
    /*
        SELECT CL.CASELOAD_ID CASE_LOAD_ID,
        CL.DESCRIPTION,
        CL.CASELOAD_TYPE "TYPE",
        CL.CASELOAD_FUNCTION
        FROM CASELOADS CL JOIN STAFF_USER_ACCOUNTS SUA on CL.CASELOAD_ID = SUA.WORKING_CASELOAD_ID
                WHERE SUA.USERNAME = :username
     */

    /*
    SELECT WKS.WORK_TYPE CODE,
        RC1.DOMAIN,
        RC1.DESCRIPTION,
        NULL PARENT_DOMAIN,
        NULL PARENT_CODE,
        'Y' ACTIVE_FLAG,
        WKS.WORK_SUB_TYPE SUB_CODE,
        RC2.DOMAIN SUB_DOMAIN,
        RC2.DESCRIPTION SUB_DESCRIPTION,
        'Y' SUB_ACTIVE_FLAG
      FROM (SELECT W.WORK_TYPE,
                   W.WORK_SUB_TYPE
            FROM WORKS W
            WHERE W.WORKFLOW_TYPE = 'CNOTE'
              AND W.CASELOAD_TYPE IN ((:caseLoadType),'BOTH')
          AND W.MANUAL_SELECT_FLAG = :active_flag
          AND W.ACTIVE_FLAG = :active_flag) WKS
        INNER JOIN REFERENCE_CODES RC1 ON RC1.CODE = WKS.WORK_TYPE AND RC1.DOMAIN = 'TASK_TYPE'
        INNER JOIN REFERENCE_CODES RC2 ON RC2.CODE = WKS.WORK_SUB_TYPE AND RC2.DOMAIN = 'TASK_SUBTYPE'
          AND COALESCE(RC2.PARENT_DOMAIN, 'TASK_TYPE') = 'TASK_TYPE'
      ORDER BY WKS.WORK_TYPE, WKS.WORK_SUB_TYPE
     */
  }
}

private const val CHECK_THRESHOLD: Int = 3900
private const val MAX_CASENOTE_LENGTH_BYTES: Int = 4000
