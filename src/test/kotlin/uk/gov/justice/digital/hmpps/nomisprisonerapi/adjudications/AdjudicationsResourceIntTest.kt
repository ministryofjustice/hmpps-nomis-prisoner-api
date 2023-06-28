package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.STAFF_CONTROL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.STAFF_REPORTING_OFFICER
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.SUSPECT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.VICTIM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.testData
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

const val adjudicationNumber = 9000123L

class AdjudicationsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  lateinit var prisoner: Offender
  lateinit var prisonerVictim: Offender
  lateinit var prisonerWitness: Offender
  lateinit var anotherSuspect: Offender

  lateinit var incident: AdjudicationIncident
  lateinit var staff: Staff
  lateinit var staffInvestigator: Staff
  lateinit var staffWitness: Staff
  lateinit var staffVictim: Staff
  lateinit var staffInvolvedWithForce: Staff
  lateinit var staffIncidentReportingOfficer: Staff

  @DisplayName("GET /adjudications/adjudication-number/{adjudicationNumber}")
  @Nested
  inner class GetAdjudication {
    private var offenderBookingId: Long = 0

    @BeforeEach
    internal fun createPrisonerWithAdjudication() {
      testData(repository) {
        staff = staff(firstName = "SIMON", lastName = "BROWN")
        staffInvestigator = staff(firstName = "ISLA", lastName = "INVESTIGATOR")
        staffWitness = staff(firstName = "KOFI", lastName = "WITNESS")
        staffVictim = staff(firstName = "KWEKU", lastName = "VICTIM")
        staffInvolvedWithForce = staff(firstName = "JANE", lastName = "MUSCLES")
        staffIncidentReportingOfficer = staff(firstName = "EAGLE", lastName = "EYES")
        prisonerVictim = offender(firstName = "CHARLIE", lastName = "VICTIM") { booking {} }
        prisonerWitness = offender(firstName = "CLIVE", lastName = "SNITCH") { booking {} }
        anotherSuspect = offender(firstName = "KILLER", lastName = "BROWN") { booking {} }
        incident = adjudicationIncident(
          reportingStaff = staff,
          prisonId = "MDI",
          agencyInternalLocationId = -41,
          reportedDateTime = LocalDateTime.parse("2023-01-02T15:00"),
          reportedDate = LocalDate.parse("2023-01-02"),
          incidentDateTime = LocalDateTime.parse("2023-01-01T18:00"),
          incidentDate = LocalDate.parse("2023-01-01"),
          incidentDetails = "There was a fight in the toilets",
        ) {
          repair(repairType = "PLUM", comment = "Fixed the bog", repairCost = BigDecimal("10.30"))
          repair(repairType = "CLEA")
          party(role = WITNESS, staff = staffWitness)
          party(role = VICTIM, staff = staffVictim)
          party(role = STAFF_CONTROL, staff = staffInvolvedWithForce)
          party(role = STAFF_REPORTING_OFFICER, staff = staffIncidentReportingOfficer)
          party(role = VICTIM, offenderBooking = prisonerVictim.latestBooking())
          party(role = WITNESS, offenderBooking = prisonerWitness.latestBooking())
          party(
            role = SUSPECT,
            offenderBooking = anotherSuspect.latestBooking(),
            adjudicationNumber = 987654,
            actionDecision = IncidentDecisionAction.PLACED_ON_REPORT_ACTION_CODE,
          )
        }
        prisoner = offender(nomsId = "A1234TT") {
          booking {
            adjudicationParty(incident = incident, adjudicationNumber = adjudicationNumber) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
              )
              charge(
                offenceCode = "51:3",
                guiltyEvidence = "DEAD SWAN",
                reportDetail = null,
              )
              hearing(
                internalLocationId = -41,
                scheduleDate = LocalDate.of(2023, 1, 2),
                scheduleTime = LocalDateTime.of(2023, 1, 2, 14, 0),
                hearingDate = LocalDate.of(2023, 1, 3),
                hearingTime = LocalDateTime.of(2023, 1, 3, 15, 0),
                hearingStaffId = staff.id,
              ) {
                result(chargeSequence = 1)
              }
              investigation(
                investigator = staffInvestigator,
                comment = "Isla comment for investigation",
                assignedDate = LocalDate.parse("2023-01-02"),
              ) {
                evidence(
                  date = LocalDate.parse("2023-01-03"),
                  detail = "smashed light bulb",
                  type = "PHOTO",
                )
                evidence(
                  date = LocalDate.parse("2023-01-04"),
                  detail = "syringe",
                  type = "DRUGTEST",
                )
              }
            }
          }
        }

        offenderBookingId = prisoner.latestBooking().bookingId
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.deleteHearingByAdjudicationNumber(adjudicationNumber)
      repository.delete(incident)
      repository.delete(prisoner)
      repository.delete(prisonerVictim)
      repository.delete(prisonerWitness)
      repository.delete(anotherSuspect)
      repository.delete(staff)
      repository.delete(staffInvestigator)
      repository.delete(staffWitness)
      repository.delete(staffVictim)
      repository.delete(staffInvolvedWithForce)
      repository.delete(staffIncidentReportingOfficer)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when adjudication not found`() {
        webTestClient.get().uri("/adjudications/adjudication-number/99999999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class SimpleAdjudication {
      @Test
      fun `returns adjudication data`() {
        webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234TT")
          .jsonPath("bookingId").isEqualTo(offenderBookingId)
          .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
          .jsonPath("partyAddedDate").isEqualTo("2023-05-10")
          .jsonPath("comment").isEqualTo("party comment")
          .jsonPath("incident.adjudicationIncidentId").isEqualTo(incident.id)
          .jsonPath("incident.reportingStaff.firstName").isEqualTo("SIMON")
          .jsonPath("incident.reportingStaff.lastName").isEqualTo("BROWN")
          .jsonPath("incident.incidentDate").isEqualTo("2023-01-01")
          .jsonPath("incident.incidentTime").isEqualTo("18:00:00")
          .jsonPath("incident.reportedDate").isEqualTo("2023-01-02")
          .jsonPath("incident.reportedTime").isEqualTo("15:00:00")
          .jsonPath("incident.internalLocation.description").isEqualTo("MDI-1-1-001")
          .jsonPath("incident.internalLocation.code").isEqualTo("1")
          .jsonPath("incident.internalLocation.locationId").isEqualTo("-41")
          .jsonPath("incident.prison.code").isEqualTo("MDI")
          .jsonPath("incident.prison.description").isEqualTo("MOORLAND")
          .jsonPath("incident.details").isEqualTo("There was a fight in the toilets")
          .jsonPath("incident.incidentType.code").isEqualTo("GOV")
          .jsonPath("incident.incidentType.description").isEqualTo("Governor's Report")
          .jsonPath("incident.repairs[0].type.code").isEqualTo("PLUM")
          .jsonPath("incident.repairs[0].type.description").isEqualTo("Plumbing")
          .jsonPath("incident.repairs[0].comment").isEqualTo("Fixed the bog")
          .jsonPath("incident.repairs[0].cost").isEqualTo("10.3")
          .jsonPath("incident.repairs[1].type.code").isEqualTo("CLEA")
          .jsonPath("incident.repairs[1].type.description").isEqualTo("Cleaning")
          .jsonPath("incident.repairs[1].comment").doesNotExist()
          .jsonPath("incident.repairs[1].cost").doesNotExist()
          .jsonPath("incident.staffWitnesses[0].firstName").isEqualTo("KOFI")
          .jsonPath("incident.staffWitnesses[0].lastName").isEqualTo("WITNESS")
          .jsonPath("incident.staffWitnesses[0].staffId").isEqualTo(staffWitness.id)
          .jsonPath("incident.staffVictims[0].staffId").isEqualTo(staffVictim.id)
          .jsonPath("incident.reportingOfficers[0].staffId").isEqualTo(staffIncidentReportingOfficer.id)
          .jsonPath("incident.otherStaffInvolved[0].staffId").isEqualTo(staffInvolvedWithForce.id)
          .jsonPath("incident.prisonerVictims[0].firstName").isEqualTo("CHARLIE")
          .jsonPath("incident.prisonerVictims[0].lastName").isEqualTo("VICTIM")
          .jsonPath("incident.prisonerVictims[0].offenderNo").isEqualTo(prisonerVictim.nomsId)
          .jsonPath("incident.prisonerWitnesses[0].offenderNo").isEqualTo(prisonerWitness.nomsId)
          .jsonPath("incident.otherPrisonersInvolved[0].offenderNo").isEqualTo(anotherSuspect.nomsId)
          .jsonPath("incident.otherPrisonersInvolved[1]").doesNotExist()
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].evidence").isEqualTo("HOOCH")
          .jsonPath("charges[0].reportDetail").isEqualTo("1234/123")
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].offence.description")
          .isEqualTo("Commits any assault - assault on non prison officer member of staff")
          .jsonPath("charges[0].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[0].offenceId").isNotEmpty
          .jsonPath("charges[0].chargeSequence").isEqualTo("1")
          .jsonPath("charges[1].evidence").isEqualTo("DEAD SWAN")
          .jsonPath("charges[1].reportDetail").doesNotExist()
          .jsonPath("charges[1].offence.code").isEqualTo("51:3")
          .jsonPath("charges[1].offence.description")
          .isEqualTo("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there")
          .jsonPath("charges[1].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[1].chargeSequence").isEqualTo("2")
          .jsonPath("charges[1].offenceId").isNotEmpty
          .jsonPath("investigations[0].comment").isEqualTo("Isla comment for investigation")
          .jsonPath("investigations[0].dateAssigned").isEqualTo("2023-01-02")
          .jsonPath("investigations[0].investigator.firstName").isEqualTo("ISLA")
          .jsonPath("investigations[0].investigator.lastName").isEqualTo("INVESTIGATOR")
          .jsonPath("investigations[0].investigator.staffId").isEqualTo(staffInvestigator.id)
          .jsonPath("investigations[0].evidence[0].detail").isEqualTo("smashed light bulb")
          .jsonPath("investigations[0].evidence[0].type.code").isEqualTo("PHOTO")
          .jsonPath("investigations[0].evidence[0].type.description").isEqualTo("Photographic Evidence")
          .jsonPath("investigations[0].evidence[0].date").isEqualTo("2023-01-03")
          .jsonPath("investigations[0].evidence[1].detail").isEqualTo("syringe")
          .jsonPath("investigations[0].evidence[1].type.code").isEqualTo("DRUGTEST")
          .jsonPath("investigations[0].evidence[1].type.description").isEqualTo("Drug Test Report")
          .jsonPath("investigations[0].evidence[1].date").isEqualTo("2023-01-04")
          .jsonPath("hearings[0].type.code").isEqualTo("GOV")
          .jsonPath("hearings[0].type.description").isEqualTo("Governor's Hearing")
          .jsonPath("hearings[0].scheduleDate").isEqualTo("2023-01-02")
          .jsonPath("hearings[0].scheduleTime").isEqualTo("14:00:00")
          .jsonPath("hearings[0].hearingDate").isEqualTo("2023-01-03")
          .jsonPath("hearings[0].hearingTime").isEqualTo("15:00:00")
          .jsonPath("hearings[0].comment").isEqualTo("Hearing comment")
          .jsonPath("hearings[0].representativeText").isEqualTo("rep text")
          .jsonPath("hearings[0].hearingStaff.staffId").isEqualTo(1)
          .jsonPath("hearings[0].representativeText").isEqualTo("rep text")
          .jsonPath("hearings[0].internalLocation.description").isEqualTo("MDI-1-1-001")
          .jsonPath("hearings[0].eventStatus.code").isEqualTo("SCH")
          .jsonPath("hearings[0].eventId").isEqualTo(1)
      }
    }
  }
}
