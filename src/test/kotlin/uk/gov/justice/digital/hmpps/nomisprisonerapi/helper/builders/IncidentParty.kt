package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Outcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class IncidentPartyDslMarker

@NomisDataDslMarker
interface IncidentPartyDsl

@Component
class IncidentPartyBuilderFactory(
  private val repository: IncidentPartyBuilderRepository,
) {
  fun builder() =
    IncidentPartyBuilder(repository)
}

@Component
class IncidentPartyBuilderRepository(
  val outcomeRepository: ReferenceCodeRepository<Outcome>,
) {
  fun lookupOutcome(code: String) = outcomeRepository.findByIdOrNull(Outcome.pk(code))!!
}

class IncidentPartyBuilder(
  private val repository: IncidentPartyBuilderRepository,

) :
  IncidentPartyDsl {
  private lateinit var incidentParty: IncidentParty

  fun build(
    role: String,
    incident: Incident,
    offenderBooking: OffenderBooking?,
    staff: Staff?,
    comment: String?,
    outcome: String?,
    index: Int,
  ): IncidentParty = IncidentParty(
    id = IncidentPartyId(incident.id, index),
    role = role,
    offenderBooking = offenderBooking,
    outcome = outcome?.let { repository.lookupOutcome(it) },
    staff = staff,
    comment = comment,
  )
    .also { incidentParty = it }
}
