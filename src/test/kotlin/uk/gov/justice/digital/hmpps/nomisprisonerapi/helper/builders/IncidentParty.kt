package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderPartyRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffPartyRole
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
  val staffRoleRepository: ReferenceCodeRepository<IncidentStaffPartyRole>,
  val offenderRoleRepository: ReferenceCodeRepository<IncidentOffenderPartyRole>,
) {
  fun lookupOutcome(code: String) = outcomeRepository.findByIdOrNull(Outcome.pk(code))!!
  fun lookupStaffRole(code: String) = staffRoleRepository.findByIdOrNull(IncidentStaffPartyRole.pk(code))!!
  fun lookupOffenderRole(code: String) = offenderRoleRepository.findByIdOrNull(IncidentOffenderPartyRole.pk(code))!!
}

class IncidentPartyBuilder(
  private val repository: IncidentPartyBuilderRepository,

) :
  IncidentPartyDsl {
  private lateinit var incidentParty: IncidentParty

  fun buildOffender(
    role: String,
    incident: Incident,
    offenderBooking: OffenderBooking,
    comment: String?,
    outcome: String?,
    index: Int,
  ): IncidentOffenderParty =
    IncidentOffenderParty(
      id = IncidentPartyId(incident.id, index),
      role = repository.lookupOffenderRole(role),
      outcome = outcome?.let { repository.lookupOutcome(it) },
      offenderBooking = offenderBooking,
      comment = comment,
    )
      .also { incidentParty = it }

  fun buildStaff(
    role: String,
    incident: Incident,
    staff: Staff,
    comment: String?,
    partySequence: Int,
  ): IncidentStaffParty =
    IncidentStaffParty(
      id = IncidentPartyId(incident.id, partySequence),
      role = repository.lookupStaffRole(role),
      staff = staff,
      comment = comment,
    )
      .also { incidentParty = it }
}
