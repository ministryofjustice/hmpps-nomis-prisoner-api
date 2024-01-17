package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class IncidentDslMarker

@NomisDataDslMarker
interface IncidentDsl {
  /* TODO
  @IncidentPartyDslMarker
  fun party(
    role: PartyRole = PartyRole.WITNESS,
    offenderBooking: OffenderBooking? = null,
    staff: Staff? = null,
    comment: String = "They witnessed everything",
    dsl: IncidentPartyDsl.() -> Unit = {},
  ): IncidentParty   */
}

@Component
class IncidentBuilderFactory(
  private val repository: IncidentBuilderRepository,
) {
  fun builder(): IncidentBuilder {
    return IncidentBuilder(repository)
  }
}

@Component
class IncidentBuilderRepository(
  private val repository: IncidentRepository,
  val incidentTypeRepository: ReferenceCodeRepository<IncidentType>,
  val incidentStatusRepository: IncidentStatusRepository,

) {
  fun lookupIncidentType(code: String): IncidentType =
    incidentTypeRepository.findByIdOrNull(IncidentType.pk(code))!!

  fun lookupIncidentStatusCode(code: String): IncidentStatus = incidentStatusRepository.findByIdOrNull(code)!!

  fun save(incident: Incident): Incident = repository.save(incident)
}

class IncidentBuilder(
  private val repository: IncidentBuilderRepository,
) : IncidentDsl {

  fun build(
    title: String,
    description: String,
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime,
    incidentDateTime: LocalDateTime,
    incidentStatus: String,
    incidentType: String,
  ): Incident {
    val let = Incident(
      title = title,
      description = description,
      reportingStaff = reportingStaff,
      reportedDate = reportedDateTime.toLocalDate(),
      reportedTime = reportedDateTime.toLocalTime(),
      incidentDate = incidentDateTime.toLocalDate(),
      incidentTime = incidentDateTime.toLocalTime(),
      status = repository.lookupIncidentStatusCode(incidentStatus),
      type = repository.lookupIncidentType(incidentType),
    )
      .let { repository.save(it) }
    return let
  }
}
