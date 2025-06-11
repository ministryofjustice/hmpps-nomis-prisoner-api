package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.google.common.base.Utf8
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.SplashScreenService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus.Companion.closedStatusValues
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus.Companion.openStatusValues
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen.Companion.SPLASH_ALL_PRISONS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.QuestionnaireRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff as JPAStaff

@Service
@Transactional
class IncidentService(
  private val incidentRepository: IncidentRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val splashScreenService: SplashScreenService,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val incidentStatusRepository: IncidentStatusRepository,
  private val questionnaireRepository: QuestionnaireRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val INCIDENT_REPORTING_SCREEN_ID = "OIDINCRS"
  }

  private val seeDps = "... see DPS for full text"
  private val amendmentDateTimeFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")

  fun getIncident(incidentId: Long): IncidentResponse? = incidentRepository.findByIdOrNull(incidentId)?.toIncidentResponse()
    ?: throw NotFoundException("Incident with id=$incidentId does not exist")

  fun upsertIncident(incidentId: Long, request: UpsertIncidentRequest) {
    val agency = findAgencyOrThrow(request.location)
    val questionnaire = lookupQuestionnaire(request.typeCode)
    val status = lookupIncidentStatusCode(request.statusCode)
    val reportedStaff = lookupStaff(request.reportedBy)

    return (
      incidentRepository.findByIdOrNull(incidentId)?.apply {
        this.title = request.title
        this.description = reconstructText(request)
        this.incidentType = questionnaire.code
        this.agency = agency
        this.questionnaire = questionnaire
//      this.questions = mutableListOf<IncidentQuestion>()
//      this.offenderParties = mutableListOf<IncidentOffenderParty>()
//      this.incidentHistory = mutableListOf<IncidentHistory>()
//      this.staffParties = mutableListOf<IncidentStaffParty>()
//      this.requirements = mutableListOf<IncidentRequirement>()
        this.reportingStaff = reportedStaff.staff
        this.reportedDate = request.reportedDateTime
        this.reportedTime = request.reportedDateTime
        this.incidentDate = request.incidentDateTime
        this.incidentTime = request.incidentDateTime
      } ?: Incident(
        id = incidentId,
        title = request.title,
        description = reconstructText(request),
        incidentType = questionnaire.code,
        agency = agency,
        questionnaire = questionnaire,
        questions = mutableListOf<IncidentQuestion>(),
        offenderParties = mutableListOf<IncidentOffenderParty>(),
        incidentHistory = mutableListOf<IncidentHistory>(),
        staffParties = mutableListOf<IncidentStaffParty>(),
        requirements = mutableListOf<IncidentRequirement>(),
        reportingStaff = reportedStaff.staff,
        reportedDate = request.reportedDateTime,
        reportedTime = request.reportedDateTime,
        incidentDate = request.incidentDateTime,
        incidentTime = request.incidentDateTime,
        status = status,
      )
      ).let {
      incidentRepository.save(it)
    }
  }

  fun findIdsByFilter(pageRequest: Pageable, incidentFilter: IncidentFilter): Page<IncidentIdResponse> {
    log.info("Incident Id filter request : $incidentFilter with page request $pageRequest")
    return findAllIds(
      fromDate = incidentFilter.fromDate?.atStartOfDay(),
      toDate = incidentFilter.toDate?.plusDays(1)?.atStartOfDay(),
      pageRequest,
    ).map { IncidentIdResponse(it) }
  }

  fun findAllIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageRequest: Pageable,
  ) = if (fromDate == null && toDate == null) {
    incidentRepository.findAllIncidentIds(pageRequest)
  } else {
    // optimisation: only do the complex SQL if we have a filter
    // typically we won't when run in production
    incidentRepository.findAllIncidentIds(fromDate, toDate, pageRequest)
  }

  fun getIncidentsForBooking(bookingId: Long): List<IncidentResponse> = offenderBookingRepository.findByIdOrNull(bookingId)
    ?.let { incidentRepository.findAllIncidentsByBookingId(bookingId).map { it.toIncidentResponse() } }
    ?: throw NotFoundException("Prisoner with booking $bookingId not found")

  fun findAllIncidentAgencies(): List<IncidentAgencyId> {
    val blockedPrisonIds = splashScreenService.getBlockedPrisons(INCIDENT_REPORTING_SCREEN_ID).map { it.prisonId }
    return if (blockedPrisonIds.contains(SPLASH_ALL_PRISONS)) {
      listOf()
    } else {
      incidentRepository.findAllIncidentAgencies()
        .filter { it !in blockedPrisonIds }
        .map { IncidentAgencyId(it) }
    }
  }

  fun getIncidentCountsForReconciliation(agencyId: String): IncidentsReconciliationResponse = IncidentsReconciliationResponse(
    agencyId = agencyId,
    incidentCount = incidentRepository.countsByAgency(agencyId, openStatusValues, closedStatusValues),
  )

  fun getOpenIncidentIdsForReconciliation(agencyId: String, pageRequest: Pageable): Page<IncidentIdResponse> = incidentRepository.findAllIncidentIdsByAgencyAndStatus(agencyId, openStatusValues, pageRequest).map { IncidentIdResponse(it) }

  private fun findAgencyOrThrow(agencyId: String) = agencyLocationRepository.findByIdOrNull(agencyId)
    ?: throw BadDataException("Agency with id=$agencyId does not exist")

  private fun lookupIncidentStatusCode(code: String): IncidentStatus = incidentStatusRepository.findByIdOrNull(code)
    ?: throw BadDataException("Incident status with code=$code does not exist")

  private fun lookupQuestionnaire(code: String): Questionnaire = questionnaireRepository.findOneByCode(code)
    ?: throw BadDataException("Questionnaire with code=$code does not exist")

  private fun lookupStaff(username: String): StaffUserAccount = staffUserAccountRepository.findByUsername(username)
    ?: throw BadDataException("Staff user account $username not found")

  internal fun reconstructText(request: UpsertIncidentRequest): String? {
    var text = request.description
    request.descriptionAmendments.forEach { amendment ->
      val timestamp = amendment.createdDateTime.format(amendmentDateTimeFormat)
      text += "User:${amendment.lastName},${amendment.firstName} Date:$timestamp${amendment.text}"
    }

    return text.truncate()
  }

  private fun String.truncate(): String = // encodedLength always >= length
    if (Utf8.encodedLength(this) <= MAX_INCIDENT_LENGTH_BYTES) {
      this
    } else {
      substring(0, MAX_INCIDENT_LENGTH_BYTES - (Utf8.encodedLength(this) - length) - seeDps.length) + seeDps
    }
}
private const val MAX_INCIDENT_LENGTH_BYTES: Int = 4000

private fun Incident.toIncidentResponse(): IncidentResponse = IncidentResponse(
  incidentId = id,
  questionnaireId = questionnaire.id,
  title = title,
  description = description,
  status = status,
  type = questionnaire.code,
  agency = agency.toCodeDescription(),
  lockedResponse = lockedResponse,
  incidentDateTime = LocalDateTime.of(incidentDate.toLocalDate(), incidentTime.toLocalTime()),
  followUpDate = followUpDate,
  reportingStaff = reportingStaff.toStaff(),
  reportedDateTime = LocalDateTime.of(reportedDate.toLocalDate(), reportedTime.toLocalTime()),
  staffParties = staffParties.map { it.toStaffParty() },
  offenderParties = offenderParties.map { it.toOffenderParty() },
  requirements = requirements.map { it.toRequirement() },
  questions = questions.map { it.toQuestionResponse() },
  history = incidentHistory.map { it.toHistoryResponse() },
  createDateTime = createDatetime,
  createdBy = createUsername,
  lastModifiedDateTime = lastModifiedDateTime,
  lastModifiedBy = lastModifiedUsername,
)

private fun IncidentRequirement.toRequirement() = Requirement(
  staff = recordingStaff.toStaff(),
  sequence = id.requirementSequence,
  comment = comment,
  recordedDate = recordedDate,
  agencyId = agency.id,
  createDateTime = createDatetime,
  createdBy = createUsername,
  lastModifiedDateTime = lastModifiedDateTime,
  lastModifiedBy = lastModifiedUsername,
)

private fun IncidentQuestion.toQuestionResponse() = Question(
  questionId = question.id,
  sequence = id.questionSequence,
  question = question.questionText,
  createDateTime = createDatetime,
  createdBy = createUsername,
  answers = responses.map { response ->
    Response(
      questionResponseId = response.answer?.id,
      sequence = response.id.responseSequence,
      answer = response.answer?.answerText,
      comment = response.comment,
      responseDate = response.responseDate,
      recordingStaff = response.recordingStaff.toStaff(),
      createDateTime = response.createDatetime,
      createdBy = response.createUsername,
      lastModifiedDateTime = response.lastModifiedDateTime,
      lastModifiedBy = response.lastModifiedUsername,
    )
  },
)

private fun IncidentHistory.toHistoryResponse() = History(
  questionnaireId = questionnaire.id,
  type = questionnaire.code,
  description = questionnaire.description,
  incidentChangeDateTime = incidentChangeDateTime,
  incidentChangeStaff = incidentChangeStaff.toStaff(),
  createDateTime = createDatetime,
  createdBy = createUsername,
  questions = questions.map { historyQuestion ->
    HistoryQuestion(
      questionId = historyQuestion.question.id,
      sequence = historyQuestion.id.questionSequence,
      question = historyQuestion.question.questionText,
      answers = historyQuestion.responses.map { response ->
        HistoryResponse(
          questionResponseId = response.answer?.id,
          responseSequence = response.id.responseSequence,
          answer = response.answer?.answerText,
          responseDate = response.responseDate,
          comment = response.comment,
          recordingStaff = response.recordingStaff.toStaff(),
        )
      },
    )
  },
)

private fun IncidentStaffParty.toStaffParty() = StaffParty(
  staff = staff.toStaff(),
  sequence = id.partySequence,
  role = role.toCodeDescription(),
  comment = comment,
  createDateTime = createDatetime,
  createdBy = createUsername,
  lastModifiedDateTime = lastModifiedDateTime,
  lastModifiedBy = lastModifiedUsername,
)

private fun JPAStaff.toStaff() = Staff(
  staffId = id,
  firstName = firstName,
  lastName = lastName,
  username = accounts.maxByOrNull { it.type }?.username ?: "unknown",
)

private fun IncidentOffenderParty.toOffenderParty() = OffenderParty(
  offender = offenderBooking.offender.toOffender(),
  sequence = id.partySequence,
  role = role.toCodeDescription(),
  outcome = outcome?.toCodeDescription(),
  comment = comment,
  createDateTime = createDatetime,
  createdBy = createUsername,
  lastModifiedDateTime = lastModifiedDateTime,
  lastModifiedBy = lastModifiedUsername,
)

private fun Offender.toOffender() = Offender(
  offenderNo = nomsId,
  firstName = firstName,
  lastName = lastName,
)
