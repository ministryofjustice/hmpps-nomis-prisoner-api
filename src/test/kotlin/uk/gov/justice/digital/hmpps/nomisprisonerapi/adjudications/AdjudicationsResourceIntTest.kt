package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
      staff = repository.save(StaffBuilder())
      incident = repository.save(AdjudicationIncidentBuilder(reportingStaff = staff))
      prisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder()
              .withAdjudication(incident, AdjudicationPartyBuilder(adjudicationNumber = adjudicationNumber)),
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
      }
    }
  }
}
