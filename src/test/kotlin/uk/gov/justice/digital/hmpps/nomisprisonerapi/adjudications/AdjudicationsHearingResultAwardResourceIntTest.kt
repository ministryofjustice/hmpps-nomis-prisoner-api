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
    private lateinit var previousIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private val previousAdjudicationNumber = 123455L
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
        previousIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = previousIncident, adjudicationNumber = previousAdjudicationNumber) {
              val charge = charge(offenceCode = "51:1A")
              hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2022-01-03"),
                hearingTime = LocalDateTime.parse("2022-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 9,
                    effectiveDate = LocalDate.parse("2022-01-01"),
                  )
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 10,
                    effectiveDate = LocalDate.parse("2022-01-02"),
                  )
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ASSO",
                    sanctionDays = 11,
                    effectiveDate = LocalDate.parse("2022-01-03"),
                  )
                }
              }
            }
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
      repository.deleteHearingByAdjudicationNumber(previousAdjudicationNumber)
      repository.delete(previousIncident)
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
        webTestClient.post()
          .uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/awards")
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
          .jsonPath("developerMessage")
          .isEqualTo("Charge not found for adjudication number $existingAdjudicationNumber and charge sequence 88")
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

      @Test
      fun `will return 400 if appropriate consecutive award not found`() {
        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
              {
                "awardRequests": [
                  {
                    "sanctionType": "EXTRA_WORK",
                    "sanctionStatus": "SUSPENDED",
                    "commentText": "a comment",
                    "sanctionDays": 3,
                    "effectiveDate": "2023-01-01",
                    "consecutiveCharge" : {
                      "adjudicationNumber": $previousAdjudicationNumber,
                      "chargeSequence": 1
                    }
                  }
                ]
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Matching consecutive adjudication award not found. Adjudication number: 123455, charge sequence: 1, sanction code: EXTRA_WORK")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an adjudication hearing result award`() {
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
          .expectBody()
          .jsonPath("awardResponses[0].sanctionSequence").isEqualTo(4)
          .jsonPath("awardResponses[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/4")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(4)
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
            assertThat(it).containsEntry("sanctionSequence", "4")
          },
          isNull(),
        )
      }

      @Test
      fun `create a consecutive adjudication hearing result award`() {
        // given there is an existing ADA
        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(2)
          .jsonPath("sanctionType.code").isEqualTo("ADA")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2022-01-02")
          .jsonPath("sanctionDays").isEqualTo(10)
          .jsonPath("chargeSequence").isEqualTo(1)
          .jsonPath("adjudicationNumber").isEqualTo(previousAdjudicationNumber)

        webTestClient.post()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
              {
                "awardRequests": [
                  {
                    "sanctionType": "ADA",
                    "sanctionStatus": "SUSPENDED",
                    "commentText": "a comment",
                    "sanctionDays": 3,
                    "effectiveDate": "2023-01-01",
                    "consecutiveCharge" : {
                      "adjudicationNumber": $previousAdjudicationNumber,
                      "chargeSequence": 1
                    }
                  }
                ]
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("awardResponses[0].sanctionSequence").isEqualTo(4)
          .jsonPath("awardResponses[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/4")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(4)
          .jsonPath("sanctionType.code").isEqualTo("ADA")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-01-01")
          .jsonPath("sanctionDays").isEqualTo(3)
          .jsonPath("comment").isEqualTo("a comment")
          .jsonPath("consecutiveAward.sequence").isEqualTo(2)
          .jsonPath("consecutiveAward.sanctionType.code").isEqualTo("ADA")
          .jsonPath("consecutiveAward.sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("consecutiveAward.effectiveDate").isEqualTo("2022-01-02")
          .jsonPath("consecutiveAward.sanctionDays").isEqualTo(10)
          .jsonPath("consecutiveAward.chargeSequence").isEqualTo(1)
          .jsonPath("consecutiveAward.adjudicationNumber").isEqualTo(previousAdjudicationNumber)

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("bookingId", prisoner.latestBooking().bookingId.toString())
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("sanctionSequence", "4")
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

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/awards")
  @Nested
  inner class UpdateHearingResultAward {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var previousIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private val previousAdjudicationNumber = 123455L
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
        previousIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = previousIncident, adjudicationNumber = previousAdjudicationNumber) {
              val charge = charge(offenceCode = "51:1A")
              hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2022-01-03"),
                hearingTime = LocalDateTime.parse("2022-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 9,
                    effectiveDate = LocalDate.parse("2022-01-01"),
                  )
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 10,
                    effectiveDate = LocalDate.parse("2022-01-02"),
                  )
                  award(
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ASSO",
                    sanctionDays = 11,
                    effectiveDate = LocalDate.parse("2022-01-03"),
                  )
                }
              }
            }
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
                ) {
                  award(
                    sanctionIndex = 5,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 6,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ASSO",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 7,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "EXTW",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
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
      repository.deleteHearingByAdjudicationNumber(previousAdjudicationNumber)
      repository.delete(previousIncident)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest()))
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
        webTestClient.put()
          .uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.put().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/88/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Charge not found for adjudication number $existingAdjudicationNumber and charge sequence 88")
      }

      @Test
      fun `will return 400 if sanction type not valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest(sanctionType = "madeup")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction type madeup not found")
      }

      @Test
      fun `will return 400 if sanction status not valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aUpdateHearingResultAwardRequest(sanctionStatus = "nope")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction status nope not found")
      }

      @Test
      fun `will return 400 if appropriate consecutive award not found`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
              {
                "awardRequestsToCreate": [
                  {
                    "sanctionType": "EXTRA_WORK",
                    "sanctionStatus": "SUSPENDED",
                    "commentText": "a comment",
                    "sanctionDays": 3,
                    "effectiveDate": "2023-01-01",
                    "consecutiveCharge" : {
                      "adjudicationNumber": $previousAdjudicationNumber,
                      "chargeSequence": 1
                    }
                  }
                ],
                "awardRequestsToUpdate": []
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Matching consecutive adjudication award not found. Adjudication number: 123455, charge sequence: 1, sanction code: EXTRA_WORK")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create any adjudication hearing result awards that need adding`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aUpdateHearingResultAwardRequest(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("awardResponsesCreated[0].sanctionSequence").isEqualTo(8)
          .jsonPath("awardResponsesCreated[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/8")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(8)
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
            assertThat(it).containsEntry("sanctionSequence", "8")
          },
          isNull(),
        )
      }

      @Test
      fun `create a consecutive adjudication hearing result award`() {
        // given there is an existing ADA
        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(2)
          .jsonPath("sanctionType.code").isEqualTo("ADA")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2022-01-02")
          .jsonPath("sanctionDays").isEqualTo(10)
          .jsonPath("chargeSequence").isEqualTo(1)
          .jsonPath("adjudicationNumber").isEqualTo(previousAdjudicationNumber)

        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
              {
                "awardRequestsToCreate": [
                  {
                    "sanctionType": "ADA",
                    "sanctionStatus": "SUSPENDED",
                    "commentText": "a comment",
                    "sanctionDays": 3,
                    "effectiveDate": "2023-01-01",
                    "consecutiveCharge" : {
                      "adjudicationNumber": $previousAdjudicationNumber,
                      "chargeSequence": 1
                    }
                  }
                ], 
                "awardRequestsToUpdate": []
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("awardResponsesCreated[0].sanctionSequence").isEqualTo(8)
          .jsonPath("awardResponsesCreated[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/8")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(8)
          .jsonPath("sanctionType.code").isEqualTo("ADA")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-01-01")
          .jsonPath("sanctionDays").isEqualTo(3)
          .jsonPath("comment").isEqualTo("a comment")
          .jsonPath("consecutiveAward.sequence").isEqualTo(2)
          .jsonPath("consecutiveAward.sanctionType.code").isEqualTo("ADA")
          .jsonPath("consecutiveAward.sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("consecutiveAward.effectiveDate").isEqualTo("2022-01-02")
          .jsonPath("consecutiveAward.sanctionDays").isEqualTo(10)
          .jsonPath("consecutiveAward.chargeSequence").isEqualTo(1)
          .jsonPath("consecutiveAward.adjudicationNumber").isEqualTo(previousAdjudicationNumber)

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("bookingId", prisoner.latestBooking().bookingId.toString())
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("sanctionSequence", "8")
          },
          isNull(),
        )
      }

      @Test
      fun `will update existing awards`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTW")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """
                {
                  "awardRequestsToCreate": [],
                  "awardRequestsToUpdate": [
                    { 
                      "sanctionSequence": 5,
                      "awardRequests": {
                         "sanctionType": "CC",
                         "sanctionStatus": "IMMEDIATE",
                         "sanctionDays": 3,
                         "effectiveDate": "2023-01-04"
                      }
                    },
                    { 
                      "sanctionSequence": 6,
                      "awardRequests": {
                         "sanctionType": "ASSO",
                         "sanctionStatus": "SUSPENDED",
                         "sanctionDays": 2,
                         "effectiveDate": "2023-02-04"
                      }
                    },
                    { 
                      "sanctionSequence": 7,
                      "awardRequests": {
                         "sanctionType": "EXTRA_WORK",
                         "sanctionStatus": "IMMEDIATE",
                         "sanctionDays": 6,
                         "effectiveDate": "2023-01-04"
                      }
                    }
                  ]
                }
      """,
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(3)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-02-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/${prisoner.bookings.first().bookingId}/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTRA_WORK")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(6)
      }
    }

    private fun aUpdateHearingResultAwardRequest(
      sanctionType: String = "ASSO",
      sanctionStatus: String = "IMMEDIATE",
      effectiveDate: String = "2023-01-01",
    ): String =
      """
      {
        "awardRequestsToCreate": [{
         "sanctionType": "$sanctionType",
         "sanctionStatus": "$sanctionStatus",
         "commentText": "a comment",
         "sanctionDays": 2,
         "effectiveDate": "$effectiveDate",
         "compensationAmount": 10.5
        }],
        "awardRequestsToUpdate": []
      }
      """.trimIndent()
  }
}
