package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentStatusRepository
import java.time.LocalDateTime

@DslMarker
annotation class IncidentDslMarker

@NomisDataDslMarker
interface IncidentDsl {
  @IncidentPartyDslMarker
  fun incidentParty(
    role: String = "VICT",
    offenderBooking: OffenderBooking? = null,
    staff: Staff? = null,
    comment: String? = "They witnessed everything",
    outcome: String? = null,
    dsl: IncidentPartyDsl.() -> Unit = {},
  ): IncidentParty
}

@Component
class IncidentBuilderFactory(
  private val repository: IncidentBuilderRepository,
  private val incidentPartyBuilderFactory: IncidentPartyBuilderFactory,
) {
  fun builder(): IncidentBuilder {
    return IncidentBuilder(repository, incidentPartyBuilderFactory)
  }
}

@Component
class IncidentBuilderRepository(
  private val repository: IncidentRepository,
  val incidentStatusRepository: IncidentStatusRepository,
) {
  fun lookupIncidentStatusCode(code: String): IncidentStatus = incidentStatusRepository.findByIdOrNull(code)!!
  fun save(incident: Incident): Incident = repository.save(incident)
}

class IncidentBuilder(
  private val repository: IncidentBuilderRepository,
  private val incidentPartyBuilderFactory: IncidentPartyBuilderFactory,
) : IncidentDsl {
  private lateinit var incident: Incident

  fun build(
    title: String,
    description: String,
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime,
    incidentDateTime: LocalDateTime,
    incidentStatus: String,
    questionnaire: Questionnaire,
  ): Incident =
    Incident(
      title = title,
      description = description,
      reportingStaff = reportingStaff,
      reportedDate = reportedDateTime.toLocalDate(),
      reportedTime = reportedDateTime.toLocalTime(),
      incidentDate = incidentDateTime.toLocalDate(),
      incidentTime = incidentDateTime.toLocalTime(),
      status = repository.lookupIncidentStatusCode(incidentStatus),
      questionnaire = questionnaire,
    )
      .let { repository.save(it) }
      .also { incident = it }

  override fun incidentParty(
    role: String,
    offenderBooking: OffenderBooking?,
    staff: Staff?,
    comment: String?,
    outcome: String?,
    dsl: IncidentPartyDsl.() -> Unit,
  ): IncidentParty =
    incidentPartyBuilderFactory.builder()
      .let { builder ->
        builder.build(
          role = role,
          outcome = outcome,
          offenderBooking = offenderBooking,
          staff = staff,
          comment = comment,
          incident = incident,
          index = incident.parties.size + 1,
        )
          .also { incident.parties += it }
          .also { builder.apply(dsl) }
      }
}
