package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationsHearingResultAwardResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var aLocationInMoorland: AgencyInternalLocation

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI")
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/awards")
  @Nested
  inner class CreateHearingResultAward {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var existingCharge: AdjudicationIncidentCharge
    private lateinit var existingHearingResult: AdjudicationHearingResult

    @BeforeEach
    fun createPrisonerWithAdjudicationAndHearing() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              existingCharge = charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.GOVERNORS_HEARING,
                // no staff added to hearing as hearing result POST will add it
              ) {
                existingHearingResult = result(
                  charge = existingCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                )
              }
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
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest()))
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
        webTestClient.post().uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/88/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Charge not found for adjudication number $existingAdjudicationNumber and charge sequence 88")
      }

      @Test
      fun `will return 400 if sanction type not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest(sanctionType = "madeup")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction type madeup not found")
      }

      @Test
      fun `will return 400 if sanction status not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultAwardRequest(sanctionStatus = "nope")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction status nope not found")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an adjudication hearing result award`() {
        val hearingId =
          webTestClient.post()
            .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                aHearingResultAwardRequest(),
              ),
            )
            .exchange()
            .expectStatus().isOk
        assertThat(hearingId).isNotNull

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(1)
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-01")
          .jsonPath("sanctionDays").isEqualTo(2)
          .jsonPath("compensationAmount").isEqualTo(10.5)
          .jsonPath("comment").isEqualTo("a comment")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("bookingId", prisoner.latestBooking().bookingId.toString())
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("sanctionSequence", "1")
          },
          isNull(),
        )
      }
    }

    private fun aHearingResultAwardRequest(
      sanctionType: String = "ASSO",
      sanctionStatus: String = "IMMEDIATE",
      effectiveDate: String = "2023-01-01",
    ): String =
      """
      {
        "awardRequests": [{
         "sanctionType": "$sanctionType",
         "sanctionStatus": "$sanctionStatus",
         "commentText": "a comment",
         "sanctionDays": 2,
         "effectiveDate": "$effectiveDate",
         "compensationAmount": 10.5
        }]
      }
      """.trimIndent()
  }
}
