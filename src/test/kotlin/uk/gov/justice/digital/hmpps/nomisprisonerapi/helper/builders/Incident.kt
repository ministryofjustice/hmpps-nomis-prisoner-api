package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentStatusRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class IncidentDslMarker

@NomisDataDslMarker
interface IncidentDsl {
  @IncidentPartyDslMarker
  fun staffParty(
    role: String = "VICT",
    staff: Staff,
    comment: String = "Staff said they witnessed everything",
    dsl: IncidentPartyDsl.() -> Unit = {},
  ): IncidentStaffParty

  @IncidentPartyDslMarker
  fun offenderParty(
    role: String = "VICT",
    offenderBooking: OffenderBooking,
    comment: String = "Offender said they witnessed everything",
    outcome: String? = null,
    dsl: IncidentPartyDsl.() -> Unit = {},
  ): IncidentOffenderParty

  @IncidentRequirementDslMarker
  fun requirement(
    comment: String = "Please update the name correct",
    recordingStaff: Staff,
    locationId: String,
    recordedDate: LocalDateTime = LocalDateTime.now(),
    dsl: IncidentRequirementDsl.() -> Unit = {},
  ): IncidentRequirement

  @IncidentQuestionDslMarker
  fun question(
    question: QuestionnaireQuestion,
    dsl: IncidentQuestionDsl.() -> Unit = {},
  ): IncidentQuestion

  @IncidentHistoryDslMarker
  fun history(
    questionnaire: Questionnaire,
    changeStaff: Staff,
    dsl: IncidentHistoryDsl.() -> Unit = {},
  ): IncidentHistory
}

@Component
class IncidentBuilderFactory(
  private val repository: IncidentBuilderRepository,
  private val incidentPartyBuilderFactory: IncidentPartyBuilderFactory,
  private val incidentRequirementBuilderFactory: IncidentRequirementBuilderFactory,
  private val incidentQuestionBuilderFactory: IncidentQuestionBuilderFactory,
  private val incidentHistoryBuilderFactory: IncidentHistoryBuilderFactory,
) {
  fun builder(): IncidentBuilder = IncidentBuilder(
    repository,
    incidentPartyBuilderFactory,
    incidentRequirementBuilderFactory,
    incidentQuestionBuilderFactory,
    incidentHistoryBuilderFactory,
  )
}

@Component
class IncidentBuilderRepository(
  private val repository: IncidentRepository,
  val incidentStatusRepository: IncidentStatusRepository,
  val agencyLocationRepository: AgencyLocationRepository,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupIncidentStatusCode(code: String): IncidentStatus = incidentStatusRepository.findByIdOrNull(code)!!
  fun save(incident: Incident): Incident = repository.save(incident)
}

class IncidentBuilder(
  private val repository: IncidentBuilderRepository,
  private val incidentPartyBuilderFactory: IncidentPartyBuilderFactory,
  private val incidentRequirementBuilderFactory: IncidentRequirementBuilderFactory,
  private val incidentQuestionBuilderFactory: IncidentQuestionBuilderFactory,
  private val incidentHistoryBuilderFactory: IncidentHistoryBuilderFactory,
) : IncidentDsl {
  private lateinit var incident: Incident

  fun build(
    id: Long,
    title: String,
    description: String,
    agencyId: String,
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime,
    incidentDateTime: LocalDateTime,
    incidentStatus: String,
    followUpDate: LocalDate,
    questionnaire: Questionnaire,
  ): Incident = Incident(
    id = id,
    title = title,
    description = description,
    agency = repository.lookupAgency(agencyId),
    reportingStaff = reportingStaff,
    reportedDate = reportedDateTime,
    reportedTime = reportedDateTime,
    incidentDate = incidentDateTime,
    incidentTime = incidentDateTime,
    status = repository.lookupIncidentStatusCode(incidentStatus),
    followUpDate = followUpDate,
    questionnaire = questionnaire,
    incidentType = questionnaire.code,
  )
    .let { repository.save(it) }
    .also { incident = it }

  override fun staffParty(
    role: String,
    staff: Staff,
    comment: String,
    dsl: IncidentPartyDsl.() -> Unit,
  ): IncidentStaffParty = incidentPartyBuilderFactory.builder()
    .let { builder ->
      builder.buildStaff(
        role = role,
        staff = staff,
        comment = comment,
        incident = incident,
        partySequence = incident.staffParties.count() + 1000,
      )
        .also { incident.staffParties += it }
        .also { builder.apply(dsl) }
    }

  override fun offenderParty(
    role: String,
    offenderBooking: OffenderBooking,
    comment: String,
    outcome: String?,
    dsl: IncidentPartyDsl.() -> Unit,
  ): IncidentOffenderParty = incidentPartyBuilderFactory.builder()
    .let { builder ->
      builder.buildOffender(
        role = role,
        outcome = outcome,
        offenderBooking = offenderBooking,
        comment = comment,
        incident = incident,
        index = incident.offenderParties.count() + 2000,
      )
        .also { incident.offenderParties += it }
        .also { builder.apply(dsl) }
    }

  override fun requirement(
    comment: String,
    recordingStaff: Staff,
    agencyId: String,
    recordedDate: LocalDateTime,
    dsl: IncidentRequirementDsl.() -> Unit,
  ): IncidentRequirement = incidentRequirementBuilderFactory.builder()
    .let { builder ->
      builder.build(
        comment = comment,
        incident = incident,
        recordingStaff = recordingStaff,
        agencyId = agencyId,
        recordedDate = recordedDate,
        requirementSequence = incident.requirements.size,
      )
        .also { incident.requirements += it }
        .also { builder.apply(dsl) }
    }

  override fun question(
    question: QuestionnaireQuestion,
    dsl: IncidentQuestionDsl.() -> Unit,
  ): IncidentQuestion = incidentQuestionBuilderFactory.builder()
    .let { builder ->
      builder.build(
        incident = incident,
        question = question,
        questionSequence = incident.questions.size + 1,
      )
        .also { incident.questions += it }
        .also { builder.apply(dsl) }
    }

  override fun history(
    questionnaire: Questionnaire,
    changeStaff: Staff,
    dsl: IncidentHistoryDsl.() -> Unit,
  ): IncidentHistory = incidentHistoryBuilderFactory.builder()
    .let { builder ->
      builder.build(
        questionnaire = questionnaire,
        changeStaff = changeStaff,
      )
        .also { incident.incidentHistory += it }
        .also { builder.apply(dsl) }
    }
}
