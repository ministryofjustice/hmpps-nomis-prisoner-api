package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class AdjudicationsResourceHearingsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = 101L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/hearings")
  @Nested
  inner class CreateHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisoner() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              charge(offenceCode = "51:1B")
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisonerWithNoBookings: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          prisonerWithNoBookings = offender(nomsId = "A9876AK")
        }
      }

      @Test
      fun `will return 404 if adjudication not found`() {
        webTestClient.post().uri("/adjudications/adjudication-number/88888/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 400 if hearing type is not valid`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing(hearingType = "VVV")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("""Hearing type VVV not found""")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an adjudication hearing`() {
        val hearingId = webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearing(
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          assertThat(hearingId).isNotNull
      }
    }

    private fun aHearing(
      hearingType: String = "GOV",
      internalLocationId: Long = aLocationInMoorland,
      agencyId: String = "MDI",
      hearingDate: String = "2023-01-01",
      hearingTime: String = "10:15",
    ): String =
      """
      {
        "hearingType": "$hearingType",
        "hearingDate": "$hearingDate",
        "hearingTime": "$hearingTime",
        "internalLocationId": $internalLocationId,
        "agencyId": "$agencyId"
      }
    """.trimIndent()
  }
}
