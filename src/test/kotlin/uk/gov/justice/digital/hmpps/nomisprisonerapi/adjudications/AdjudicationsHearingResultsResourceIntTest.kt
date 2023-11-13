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
import org.springframework.boot.test.mock.mockito.SpyBean
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AdjudicationsHearingResultsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var aLocationInMoorland: AgencyInternalLocation

  @SpyBean
  lateinit var hearingRepository: AdjudicationHearingRepository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI")
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/hearings/{hearingId}/charge/{chargeSequence}/result")
  @Nested
  inner class CreateHearingResult {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var migratedIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private val migratedAdjudicationNumber = 119999L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var migratedHearing: AdjudicationHearing
    private lateinit var existingCharge: AdjudicationIncidentCharge
    private lateinit var migratedCharge1: AdjudicationIncidentCharge
    private lateinit var migratedCharge2: AdjudicationIncidentCharge

    @BeforeEach
    fun createPrisonerWithAdjudicationAndHearing() {
      nomisDataBuilder.build {
        staff(firstName = "BILL", lastName = "STAFF") {
          account(username = "BILLSTAFF")
        }
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        migratedIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
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
              )
            }
            adjudicationParty(incident = migratedIncident, adjudicationNumber = migratedAdjudicationNumber) {
              migratedCharge1 = charge(offenceCode = "51:1B")
              migratedCharge2 = charge(offenceCode = "51:1A")
              migratedHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2022-01-03"),
                hearingTime = LocalDateTime.parse("2022-01-03T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.GOVERNORS_HEARING,
              ) {
                result(charge = migratedCharge1, findingCode = "NOT_PROCEED", pleaFindingCode = "NOT_GUILTY")
                result(charge = migratedCharge2, findingCode = "PROVED", pleaFindingCode = "GUILTY")
              }
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.deleteHearingByAdjudicationNumber(migratedAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(migratedIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
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
      fun `will return 404 if adjudication (associated with the hearing) not found`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if hearing not found`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing not found. Hearing Id: 88888")
      }

      @Test
      fun `will return 400 if plea finding type not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest(pleaFindingCode = "rubbish")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Plea finding type rubbish not found")
      }

      @Test
      fun `will return 400 if finding type not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest(findingCode = "rubbish")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Finding type rubbish not found")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an adjudication hearing result`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "GUILTY"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // confirm hearing has been updated with an adjudicator
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("hearingStaff.staffId").isEqualTo(reportingStaff.id)
          .jsonPath("hearingStaff.username").isEqualTo("JANESTAFF")

        webTestClient.get()
          .uri("/adjudications/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("NOT_PROCEED")
          .jsonPath("pleaFindingType.code").isEqualTo("GUILTY")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("chargeSequence", existingCharge.id.chargeSequence.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `update an adjudication hearing result`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "GUILTY"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // update the newly created result
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(
                pleaFindingCode = "NOT_ASKED",
                findingCode = "PROSECUTED",
                adjudicatorUsername = "BILLSTAFF",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // confirm hearing has been updated with an adjudicator
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("hearingStaff.username").isEqualTo("BILLSTAFF")

        webTestClient.get()
          .uri("/adjudications/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROSECUTED")
          .jsonPath("pleaFindingType.code").isEqualTo("NOT_ASKED")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-updated"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("findingCode", "PROSECUTED")
            assertThat(it).containsEntry("plea", "NOT_ASKED")
            assertThat(it).containsEntry("chargeSequence", existingCharge.id.chargeSequence.toString())
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class MigratedHearing {
      @Test
      fun `can create and remove and add results for multiple charges`() {
        // GIVEN there are two charges from a migrated records
        webTestClient.get()
          .uri("/adjudications/hearings/${migratedHearing.id}/charge/${migratedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("NOT_PROCEED")
          .jsonPath("pleaFindingType.code").isEqualTo("NOT_GUILTY")

        webTestClient.get()
          .uri("/adjudications/hearings/${migratedHearing.id}/charge/${migratedCharge2.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")
          .jsonPath("pleaFindingType.code").isEqualTo("GUILTY")

        // WHEN the first one is deleted
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$migratedAdjudicationNumber/hearings/${migratedHearing.id}/charge/${migratedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
        webTestClient.get()
          .uri("/adjudications/hearings/${migratedHearing.id}/charge/${migratedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound

        // AND re-added
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$migratedAdjudicationNumber/hearings/${migratedHearing.id}/charge/${migratedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_GUILTY", findingCode = "NOT_PROCEED"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // THEN both results should be present
        webTestClient.get()
          .uri("/adjudications/hearings/${migratedHearing.id}/charge/${migratedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("NOT_PROCEED")
          .jsonPath("pleaFindingType.code").isEqualTo("NOT_GUILTY")

        webTestClient.get()
          .uri("/adjudications/hearings/${migratedHearing.id}/charge/${migratedCharge2.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")
          .jsonPath("pleaFindingType.code").isEqualTo("GUILTY")
      }
    }

    private fun aHearingResultRequest(
      adjudicatorUsername: String = "JANESTAFF",
      findingCode: String = "NOT_PROCEED",
      pleaFindingCode: String = "NOT_GUILTY",
    ): String =
      """
      {
        "adjudicatorUsername": "$adjudicatorUsername",
        "findingCode": "$findingCode",
        "pleaFindingCode": "$pleaFindingCode"
      }
      """.trimIndent()
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/result")
  @Nested
  inner class CreateReferralResultWithoutHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingCharge: AdjudicationIncidentCharge
    private lateinit var existingSecondCharge: AdjudicationIncidentCharge

    @BeforeEach
    fun createPrisonerWithAdjudication() {
      nomisDataBuilder.build {
        staff(firstName = "BILL", lastName = "STAFF") {
          account(username = "BILLSTAFF")
        }
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              existingCharge = charge(offenceCode = "51:1B", generateOfficeId = false)
              existingSecondCharge = charge(offenceCode = "51:1C", generateOfficeId = false)
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
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
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
        webTestClient.post()
          .uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 400 if plea finding type not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest(pleaFindingCode = "rubbish")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Plea finding type rubbish not found")
      }

      @Test
      fun `will return 400 if finding type not valid`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest(findingCode = "rubbish")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Finding type rubbish not found")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an referral result with placeholder hearing`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_ASKED", findingCode = "REF_POLICE"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("hearing-result-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsKey("hearingId")
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("chargeSequence", existingCharge.id.chargeSequence.toString())
          },
          isNull(),
        )

        repository.runInTransaction {
          val adjudicationHearing = hearingRepository.findByAdjudicationNumber(existingAdjudicationNumber)[0]
          assertThat(adjudicationHearing.comment).isEqualTo("DPS_REFERRAL_PLACEHOLDER-${existingCharge.id.chargeSequence}")
          assertThat(adjudicationHearing.hearingDateTime).isEqualTo(LocalDateTime.now().with(LocalTime.MIDNIGHT))
          assertThat(adjudicationHearing.hearingResults[0].findingType.code).isEqualTo("REF_POLICE")
        }
      }

      @Test
      fun `create an referral result using existing placeholder hearing`() {
        // create a dummy hearings associated with 2 charges for the adjudication - to test the correct one is updated
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_ASKED", findingCode = "REF_POLICE"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingSecondCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_ASKED", findingCode = "REF_POLICE"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // update the hearing on first charge
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_ASKED", findingCode = "NOT_PROCEED"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("hearing-result-updated"),
          org.mockito.kotlin.check {
            assertThat(it).containsKey("hearingId")
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("chargeSequence", existingCharge.id.chargeSequence.toString())
          },
          isNull(),
        )

        repository.runInTransaction {
          assertThat(hearingRepository.findByAdjudicationNumber(existingAdjudicationNumber)).hasSize(2)
          val adjudicationHearingForCharge1 = hearingRepository.findByAdjudicationNumberAndComment(existingAdjudicationNumber, "DPS_REFERRAL_PLACEHOLDER-${existingCharge.id.chargeSequence}")!!
          val adjudicationHearingForCharge2 = hearingRepository.findByAdjudicationNumberAndComment(existingAdjudicationNumber, "DPS_REFERRAL_PLACEHOLDER-${existingSecondCharge.id.chargeSequence}")!!
          assertThat(adjudicationHearingForCharge1.hearingDateTime).isEqualTo(LocalDateTime.now().with(LocalTime.MIDNIGHT))
          assertThat(adjudicationHearingForCharge1.hearingResults[0].findingType.code).isEqualTo("NOT_PROCEED")
          // dummy hearing for other charge is not updated
          assertThat(adjudicationHearingForCharge2.hearingResults[0].findingType.code).isEqualTo("REF_POLICE")
        }
      }

      @Test
      fun `will set offenceId on all charges when missing`() {
        repository.runInTransaction {
          with(repository.adjudicationIncidentPartyRepository.findByAdjudicationNumber(existingAdjudicationNumber)!!) {
            assertThat(charges[0].offenceId).isNull()
            assertThat(charges[1].offenceId).isNull()
          }
        }

        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingResultRequest(pleaFindingCode = "NOT_ASKED", findingCode = "REF_POLICE"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          with(repository.adjudicationIncidentPartyRepository.findByAdjudicationNumber(existingAdjudicationNumber)!!) {
            // even though the referral is on a specific charge - offence id is set on all charges which is consistent
            // with NOMIS behaviour
            assertThat(charges[0].offenceId).isEqualTo("$existingAdjudicationNumber/1")
            assertThat(charges[1].offenceId).isEqualTo("$existingAdjudicationNumber/2")
          }
        }
      }
    }

    private fun aHearingResultRequest(
      adjudicatorUsername: String = "JANESTAFF",
      findingCode: String = "NOT_PROCEED",
      pleaFindingCode: String = "NOT_GUILTY",
    ): String =
      """
      {
        "adjudicatorUsername": "$adjudicatorUsername",
        "findingCode": "$findingCode",
        "pleaFindingCode": "$pleaFindingCode"
      }
      """.trimIndent()
  }

  @DisplayName("GET /adjudications/hearings/{hearingId}/result")
  @Nested
  inner class GetHearingResult {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var existingHearingResult: AdjudicationHearingResult
    private lateinit var existingCharge: AdjudicationIncidentCharge

    @BeforeEach
    fun createPrisonerAndAdjudication() {
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
                hearingStaff = reportingStaff,
              ) {
                existingHearingResult = result(
                  charge = existingCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    statusCode = "SUSPEN_RED",
                    sanctionCode = "STOP_EARN",
                    effectiveDate = LocalDate.parse("2023-01-04"),
                    statusDate = LocalDate.parse("2023-01-05"),
                    comment = "award comment",
                    sanctionMonths = 3,
                    sanctionDays = 4,
                    compensationAmount = BigDecimal.valueOf(14.2),
                    sanctionIndex = 3,
                  )
                }
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
        webTestClient.get().uri("/adjudications/hearings/123/charge/1/result")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjudications/hearings/123/charge/1/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjudications/hearings/123/charge/1/result")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if hearing result not found`() {
        webTestClient.get().uri("/adjudications/hearings/123/charge/1/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing Result not found. Hearing Id: 123, charge sequence: 1")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get adjudication hearing result`() {
        webTestClient.get()
          .uri("/adjudications/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")
          .jsonPath("findingType.description").isEqualTo("Charge Proved")
          .jsonPath("pleaFindingType.code").isEqualTo("NOT_GUILTY")
          .jsonPath("pleaFindingType.description").isEqualTo("Not guilty")
          .jsonPath("resultAwards[0]").exists()
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
      }
    }
  }

  @DisplayName("DELETE /adjudications/adjudication-number/{adjudicationNumber}/hearings/{hearingId}/charge/{chargeSequence}/result")
  @Nested
  inner class DeleteHearingResult {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var existingHearingResult: AdjudicationHearingResult
    private lateinit var existingCharge: AdjudicationIncidentCharge

    @BeforeEach
    fun createPrisonerAndAdjudication() {
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
                hearingStaff = reportingStaff,
              ) {
                existingHearingResult = result(
                  charge = existingCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    statusCode = "SUSPEN_RED",
                    sanctionCode = "STOP_EARN",
                    effectiveDate = LocalDate.parse("2023-01-04"),
                    statusDate = LocalDate.parse("2023-01-05"),
                    comment = "award comment",
                    sanctionMonths = 3,
                    sanctionDays = 4,
                    compensationAmount = BigDecimal.valueOf(14.2),
                    sanctionIndex = 3,
                  )
                }
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
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
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
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Hearing with id ${existingHearing.id} delete failed: Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will track event if hearing result not found`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.awardsDeleted.size()").isEqualTo(0)

        verify(telemetryClient).trackEvent(
          eq("hearing-result-delete-not-found"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", "88888")
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("chargeSequence", existingCharge.id.chargeSequence.toString())
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `delete an adjudication hearing result`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.awardsDeleted.size()").isEqualTo(1)
          .jsonPath("awardsDeleted[0].sanctionSequence").isEqualTo(3)
          .jsonPath("awardsDeleted[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

        webTestClient.get()
          .uri("/adjudications/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Hearing Result not found. Hearing Id: ${existingHearing.id}, charge sequence: 1")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-deleted"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
          },
          isNull(),
        )
        // confirm hearing adjudicator has been removed
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("hearingStaff").doesNotExist()
      }
    }
  }

  @DisplayName("DELETE /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/result")
  @Nested
  inner class DeleteResultWithDummyHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var existingHearingResult: AdjudicationHearingResult
    private lateinit var existingCharge: AdjudicationIncidentCharge

    @BeforeEach
    fun createPrisonerAndAdjudication() {
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
                scheduleTime = LocalDateTime.parse("2023-01-02T00:00:00"),
                hearingDate = LocalDate.parse("2023-01-02"),
                hearingTime = LocalDateTime.parse("2023-01-02T00:00:00"),
                comment = "$DPS_REFERRAL_PLACEHOLDER_HEARING-${existingCharge.id.chargeSequence}",
              ) {
                existingHearingResult = result(
                  charge = existingCharge,
                  pleaFindingCode = "NOT_ASKED",
                  findingCode = "PROSECUTED",
                ) {
                  award(
                    statusCode = "SUSPEN_RED",
                    sanctionCode = "STOP_EARN",
                    effectiveDate = LocalDate.parse("2023-01-04"),
                    statusDate = LocalDate.parse("2023-01-05"),
                    comment = "award comment",
                    sanctionMonths = 3,
                    sanctionDays = 4,
                    compensationAmount = BigDecimal.valueOf(14.2),
                    sanctionIndex = 3,
                  )
                }
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
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
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
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will track event if hearing result not found`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/77/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk()

        verify(telemetryClient).trackEvent(
          eq("hearing-result-delete-not-found"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("chargeSequence", "77")
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `delete an adjudication hearing result`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk

        // get using the dummy hearing
        webTestClient.get()
          .uri("/adjudications/hearings/${existingHearing.id}/charge/${existingCharge.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Hearing Result not found. Hearing Id: ${existingHearing.id}, charge sequence: 1")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-deleted"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
          },
          isNull(),
        )
      }
    }
  }
}
