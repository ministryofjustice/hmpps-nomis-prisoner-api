package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AdjudicationChargeBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AdjudicationIncidentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AdjudicationPartyBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

const val adjudicationNumber = 9000123L
class AdjudicationsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  lateinit var prisoner: Offender
  lateinit var incident: AdjudicationIncident
  lateinit var staff: Staff

  @DisplayName("GET /adjudications/adjudication-number/{adjudicationNumber}")
  @Nested
  inner class GetAdjudication {
    private var offenderBookingId: Long = 0

    @BeforeEach
    internal fun createPrisonerWithAdjudication() {
      staff = repository.save(StaffBuilder(firstName = "SIMON", lastName = "BROWN"))
      incident = repository.save(
        AdjudicationIncidentBuilder(
          reportingStaff = staff,
          prisonId = "MDI",
          agencyInternalLocationId = -41,
          reportedDateTime = LocalDateTime.parse("2023-01-02T15:00"),
          reportedDate = LocalDate.parse("2023-01-02"),
          incidentDateTime = LocalDateTime.parse("2023-01-01T18:00"),
          incidentDate = LocalDate.parse("2023-01-01"),
          incidentDetails = "There was a fight in the toilets",
        ),
      )
      prisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder()
              .withAdjudication(
                incident,
                AdjudicationPartyBuilder(adjudicationNumber = adjudicationNumber)
                  .withCharges(
                    AdjudicationChargeBuilder(offenceCode = "51:1N", guiltyEvidence = "HOOCH", reportDetail = "1234/123"),
                    AdjudicationChargeBuilder(offenceCode = "51:3", guiltyEvidence = "DEAD SWAN", reportDetail = null),
                  ),
              ),
          ),
      )

      offenderBookingId = prisoner.latestBooking().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(incident)
      repository.delete(prisoner)
      repository.delete(staff)
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
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].evidence").isEqualTo("HOOCH")
          .jsonPath("charges[0].reportDetail").isEqualTo("1234/123")
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].offence.description").isEqualTo("Commits any assault - assault on non prison officer member of staff")
          .jsonPath("charges[0].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[0].offenceId").isEqualTo("100/1")
          .jsonPath("charges[0].chargeSequence").isEqualTo("1")
          .jsonPath("charges[1].evidence").isEqualTo("DEAD SWAN")
          .jsonPath("charges[1].reportDetail").isEmpty
          .jsonPath("charges[1].offence.code").isEqualTo("51:3")
          .jsonPath("charges[1].offence.description").isEqualTo("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there")
          .jsonPath("charges[1].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[1].chargeSequence").isEqualTo("2")
          .jsonPath("charges[1].offenceId").isEqualTo("100/2")
      }
    }
  }
}
