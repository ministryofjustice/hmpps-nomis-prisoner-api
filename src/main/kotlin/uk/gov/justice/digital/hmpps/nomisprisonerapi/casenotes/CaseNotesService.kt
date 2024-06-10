package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.google.common.base.Utf8
import jakarta.transaction.Transactional
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository

@Service
@Transactional
class CaseNotesService(
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderCaseNoteRepository: OffenderCaseNoteRepository,
  val staffUserAccountRepository: StaffUserAccountRepository,
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
        // TODO
        dateCreation = request.occurrenceDateTime.toLocalDate(),
        timeCreation = request.occurrenceDateTime,
      ),
    )
    return CreateCaseNoteResponse(caseNote.id)
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

    val staffUserAccount = staffUserAccountRepository.findByUsername(request.authorUsername)
      ?: throw BadDataException("Username ${request.authorUsername} not found")

    caseNote.caseNoteText = request.caseNoteText
    // TODO do we want to do an 'amend' as in appending text as at present?

    return caseNote.toCaseNoteResponse()
  }

  private fun OffenderCaseNote.toCaseNoteResponse() = CaseNoteResponse(
    caseNoteId = id,
    bookingId = offenderBooking.bookingId,
    caseNoteType = caseNoteType.toCodeDescription(),
    caseNoteSubType = caseNoteSubType.toCodeDescription(),
    occurrenceDateTime = occurrenceDateTime,
    authorUsername = author.accounts.first().username,
    // TODO not sure what will be required here
    prisonId = agencyLocation?.id ?: offenderBooking.location?.id,
    caseNoteText = caseNoteText,
    amended = amendmentFlag,
  )

  /**
   * For reconciliation or migration
   */
  fun getCaseNotes(bookingId: Long): BookingCaseNotesResponse {
    offenderBookingRepository.findById(bookingId).orElseThrow { NotFoundException("bookingId $bookingId not found") }
    return BookingCaseNotesResponse(
      offenderCaseNoteRepository.findAllByOffenderBooking_BookingId(bookingId).map { it.toCaseNoteResponse() },
    )
  }

  /**
   * For reconciliation or migration
   */
  fun getAllBookingIds(
    fromId: Long?,
    toId: Long?,
    activeOnly: Boolean = true,
    pageable: Pageable,
  ) = offenderCaseNoteRepository.findAllBookingIds(fromId, toId, activeOnly, pageable)

  private fun validateTextLength(value: String) {
    if (value.length >= CHECK_THRESHOLD || Utf8.encodedLength(value) > MAX_VARCHAR_BYTES) {
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
