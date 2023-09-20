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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationsHearingResultsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var aLocationInMoorland: AgencyInternalLocation

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI")
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/hearings")
  @Nested
  inner class CreateHearingResult {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing

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
              charge(offenceCode = "51:1B")
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
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingResultRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
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
        webTestClient.post().uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}/result")
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
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888/result")
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
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
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
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
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
        val hearingId =
          webTestClient.post()
            .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                aHearingResultRequest(pleaFindingCode = "GUILTY"),
              ),
            )
            .exchange()
            .expectStatus().isOk
        assertThat(hearingId).isNotNull

        // confirm hearing has been updated with an adjudicator
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("hearingStaff.staffId").isEqualTo(reportingStaff.id)
          .jsonPath("hearingStaff.username").isEqualTo("JANESTAFF")

        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}/result")
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
          },
          isNull(),
        )
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
              val charge = charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                existingHearingResult = result(
                  charge = charge,
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
        webTestClient.get().uri("/adjudications/hearings/123/result")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjudications/hearings/123/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjudications/hearings/123/result")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if hearing result not found`() {
        webTestClient.get().uri("/adjudications/hearings/123/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing Result not found. Hearing Id: 123, result sequence: 1")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get adjudication hearing result`() {
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}/result")
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

  @DisplayName("DELETE /adjudications/adjudication-number/{adjudicationNumber}/hearings/{hearingId}/result")
  @Nested
  inner class DeleteHearingResult {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing
    private lateinit var existingHearingResult: AdjudicationHearingResult

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
              val charge = charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                existingHearingResult = result(
                  charge = charge,
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
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
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
        webTestClient.delete().uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing with id ${existingHearing.id} delete failed: Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will track event if hearing result not found`() {
        webTestClient.delete().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk()

        verify(telemetryClient).trackEvent(
          eq("hearing-result-delete-not-found"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", "88888")
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `delete an adjudication hearing`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing Result not found. Hearing Id: ${existingHearing.id}, result sequence: 1")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-deleted"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
          },
          isNull(),
        )
      }
    }
  }
}