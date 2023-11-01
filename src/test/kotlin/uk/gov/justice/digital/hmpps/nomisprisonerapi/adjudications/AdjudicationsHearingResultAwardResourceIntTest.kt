package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
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

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/awards")
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
                "awards": [
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
          .jsonPath("awardsCreated[0].sanctionSequence").isEqualTo(4)
          .jsonPath("awardsCreated[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

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
          check {
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
                "awards": [
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
          .jsonPath("awardsCreated[0].sanctionSequence").isEqualTo(4)
          .jsonPath("awardsCreated[0].bookingId").isEqualTo(prisoner.latestBooking().bookingId)

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
          check {
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
        "awards": [{
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

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/awards")
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
                "awardsToCreate": [
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
                "awardsToUpdate": []
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
        val bookingId = prisoner.bookings.first().bookingId

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
          .jsonPath("awardsCreated[0].sanctionSequence").isEqualTo(8)
          .jsonPath("awardsCreated[0].bookingId").isEqualTo(bookingId)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/8")
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
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
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
        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/2")
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
                "awardsToCreate": [
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
                "awardsToUpdate": []
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("awardsCreated[0].sanctionSequence").isEqualTo(8)
          .jsonPath("awardsCreated[0].bookingId").isEqualTo(bookingId)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/8")
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
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
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
        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
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
                  "awardsToCreate": [],
                  "awardsToUpdate": [
                    { 
                      "sanctionSequence": 5,
                      "award": {
                         "sanctionType": "CC",
                         "sanctionStatus": "IMMEDIATE",
                         "sanctionDays": 3,
                         "effectiveDate": "2023-01-04"
                      }
                    },
                    { 
                      "sanctionSequence": 6,
                      "award": {
                         "sanctionType": "ASSO",
                         "sanctionStatus": "SUSPENDED",
                         "sanctionDays": 2,
                         "effectiveDate": "2023-02-04"
                      }
                    },
                    { 
                      "sanctionSequence": 7,
                      "award": {
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

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(3)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-02-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTRA_WORK")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(6)
      }

      @Test
      fun `will delete awards no longer referenced and return their IDs`() {
        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
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
                  "awardsToCreate": [],
                  "awardsToUpdate": [
                    { 
                      "sanctionSequence": 6,
                      "award": {
                         "sanctionType": "ASSO",
                         "sanctionStatus": "SUSPENDED",
                         "sanctionDays": 2,
                         "effectiveDate": "2023-02-04"
                      }
                    },
                    { 
                      "sanctionSequence": 7,
                      "award": {
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
          .expectBody()
          .jsonPath("awardsDeleted[0].sanctionSequence").isEqualTo("5")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-02-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTRA_WORK")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-01-04")
          .jsonPath("sanctionDays").isEqualTo(6)
      }

      @Test
      fun `can add, update and delete all in a single call`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """
                {
                  "awardsToCreate": [
                    {
                       "sanctionType": "ASSO",
                       "sanctionStatus": "SUSPENDED",
                       "sanctionDays": 2,
                       "effectiveDate": "2023-02-04"
                    }
                  ],
                  "awardsToUpdate": [
                    { 
                      "sanctionSequence": 7,
                      "award": {
                         "sanctionType": "EXTRA_WORK",
                         "sanctionStatus": "IMMEDIATE",
                         "sanctionDays": 16,
                         "effectiveDate": "2023-02-04"
                      }
                    }
                  ]
                }
      """,
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("awardsCreated[0].sanctionSequence").isEqualTo("8")
          .jsonPath("awardsDeleted[0].sanctionSequence").isEqualTo("5")
          .jsonPath("awardsDeleted[1].sanctionSequence").isEqualTo("6")

        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTRA_WORK")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("effectiveDate").isEqualTo("2023-02-04")
          .jsonPath("sanctionDays").isEqualTo(16)

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/8")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("effectiveDate").isEqualTo("2023-02-04")
          .jsonPath("sanctionDays").isEqualTo(2)

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-created"),
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("resultSequence", "1")
            assertThat(it).containsEntry("sanctionSequence", "8")
          },
          isNull(),
        )

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-updated"),
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("sanctionSequence", "7")
          },
          isNull(),
        )

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-deleted"),
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("sanctionSequence", "5")
          },
          isNull(),
        )

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-deleted"),
          check {
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("sanctionSequence", "6")
          },
          isNull(),
        )
      }
    }
    private fun aUpdateHearingResultAwardRequest(
      sanctionType: String = "ASSO",
      sanctionStatus: String = "IMMEDIATE",
      effectiveDate: String = "2023-01-01",
    ): String =
      """
      {
        "awardsToCreate": [{
         "sanctionType": "$sanctionType",
         "sanctionStatus": "$sanctionStatus",
         "commentText": "a comment",
         "sanctionDays": 2,
         "effectiveDate": "$effectiveDate",
         "compensationAmount": 10.5
        }],
        "awardsToUpdate": []
      }
      """.trimIndent()
  }

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/quash")
  @Nested
  inner class SquashHearingResultAward {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var previousIncident: AdjudicationIncident
    private val dpsCreateAdjudication = 123456L
    private val nomisMigratedAdjudication = 123455L
    private lateinit var nomisCreatedCharge1: AdjudicationIncidentCharge
    private lateinit var nomisCreatedCharge2: AdjudicationIncidentCharge
    private lateinit var dpsHearing: AdjudicationHearing
    private lateinit var lastNomisHearing: AdjudicationHearing
    private lateinit var previousNomisHearing: AdjudicationHearing

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
            adjudicationParty(incident = previousIncident, adjudicationNumber = dpsCreateAdjudication) {
              val charge = charge(offenceCode = "51:1A")
              dpsHearing = hearing(
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
                    sanctionIndex = 1,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 9,
                    effectiveDate = LocalDate.parse("2022-01-01"),
                  )
                  award(
                    sanctionIndex = 2,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 10,
                    effectiveDate = LocalDate.parse("2022-01-02"),
                  )
                  award(
                    sanctionIndex = 3,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ASSO",
                    sanctionDays = 11,
                    effectiveDate = LocalDate.parse("2022-01-03"),
                  )
                }
              }
            }
            adjudicationParty(incident = existingIncident, adjudicationNumber = nomisMigratedAdjudication) {
              nomisCreatedCharge1 = charge(offenceCode = "51:1B")
              nomisCreatedCharge2 = charge(offenceCode = "51:1C")
              previousNomisHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                result(
                  charge = nomisCreatedCharge1,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    sanctionIndex = 4,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                }
                result(
                  charge = nomisCreatedCharge2,
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
                }
              }

              lastNomisHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2023-01-04"),
                hearingTime = LocalDateTime.parse("2023-01-04T15:00:00"),
                hearingStaff = reportingStaff,
              ) {
                result(
                  charge = nomisCreatedCharge2,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
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
      repository.deleteHearingByAdjudicationNumber(dpsCreateAdjudication)
      repository.deleteHearingByAdjudicationNumber(nomisMigratedAdjudication)
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
          .uri("/adjudications/adjudication-number/$dpsCreateAdjudication/charge/${nomisCreatedCharge1.id.chargeSequence}/quash")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$dpsCreateAdjudication/charge/${nomisCreatedCharge1.id.chargeSequence}/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$dpsCreateAdjudication/charge/${nomisCreatedCharge1.id.chargeSequence}/quash")
          .contentType(MediaType.APPLICATION_JSON)
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
          .uri("/adjudications/adjudication-number/88888/charge/1/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.put().uri("/adjudications/adjudication-number/$dpsCreateAdjudication/charge/88/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Charge not found for adjudication number $dpsCreateAdjudication and charge sequence 88")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `awards and outcome all set to quashed`() {
        val bookingId = prisoner.bookings.first().bookingId

        webTestClient.put()
          .uri("/adjudications/adjudication-number/$dpsCreateAdjudication/charge/1/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/adjudications/hearings/${dpsHearing.id}/charge/1/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("QUASHED")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")

        // different adjudication so not quashed
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/4")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")

        verify(telemetryClient).trackEvent(
          eq("hearing-result-quashed"),
          check {
            assertThat(it).containsEntry("offenderNo", prisoner.nomsId)
            assertThat(it).containsEntry("hearingId", dpsHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", dpsCreateAdjudication.toString())
          },
          isNull(),
        )
        verify(telemetryClient, times(3)).trackEvent(
          eq("hearing-result-award-quashed"),
          any(),
          isNull(),
        )
      }

      @Test
      fun `will only quash awards for the specific charge for a migrated adjudication`() {
        val bookingId = prisoner.bookings.first().bookingId

        webTestClient.put()
          .uri("/adjudications/adjudication-number/$nomisMigratedAdjudication/charge/${nomisCreatedCharge2.id.chargeSequence}/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isOk

        // nomisCreatedCharge1 awards remains unchanged
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/4")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/6")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/7")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")
      }

      @Test
      fun `will only quash hearing outcome last hearing for that charge`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$nomisMigratedAdjudication/charge/${nomisCreatedCharge2.id.chargeSequence}/quash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isOk

        // first hearing for different charge untouched
        webTestClient.get()
          .uri("/adjudications/hearings/${previousNomisHearing.id}/charge/${nomisCreatedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")

        // first hearing for charge also untouched
        webTestClient.get()
          .uri("/adjudications/hearings/${previousNomisHearing.id}/charge/${nomisCreatedCharge2.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")

        // latest hearing for other charge untouched
        webTestClient.get()
          .uri("/adjudications/hearings/${previousNomisHearing.id}/charge/${nomisCreatedCharge1.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("PROVED")

        // latest hearing for this charge set to quashed
        webTestClient.get()
          .uri("/adjudications/hearings/${lastNomisHearing.id}/charge/${nomisCreatedCharge2.id.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("findingType.code").isEqualTo("QUASHED")
      }
    }
  }

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/unquash")
  @Nested
  inner class UnquashHearingResultAward {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var incident: AdjudicationIncident
    private val adjudicationNumber = 123456L
    private lateinit var hearing: AdjudicationHearing
    private lateinit var charge: AdjudicationIncidentCharge
    private lateinit var hearingResult: AdjudicationHearingResult

    @BeforeEach
    fun createPrisonerWithAdjudicationAndHearing() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        incident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = incident, adjudicationNumber = adjudicationNumber) {
              charge = charge(offenceCode = "51:1B")
              hearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.GOVERNORS_HEARING,
              ) {
                hearingResult = result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "QUASHED",
                ) {
                  award(
                    sanctionIndex = 1,
                    statusCode = "QUASHED",
                    sanctionCode = "CC",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 2,
                    statusCode = "QUASHED",
                    sanctionCode = "ASSO",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 3,
                    statusCode = "QUASHED",
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
      repository.deleteHearingByAdjudicationNumber(adjudicationNumber)
      repository.delete(incident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest()))
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
          .uri("/adjudications/adjudication-number/88888/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.put().uri("/adjudications/adjudication-number/$adjudicationNumber/charge/88/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Charge not found for adjudication number $adjudicationNumber and charge sequence 88")
      }

      @Test
      fun `will return 400 if sanction type not valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest(firstSanctionType = "madeup")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction type madeup not found")
      }

      @Test
      fun `will return 400 if sanction status not valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(anUnquashHearingResultAwardRequest(firstSanctionStatus = "nope")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sanction status nope not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update existing awards`() {
        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionStatus.code").isEqualTo("QUASHED")

        webTestClient.put()
          .uri("/adjudications/adjudication-number/$adjudicationNumber/charge/${charge.id.chargeSequence}/unquash")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """
                {
                  "awards": {
                    "awardsToCreate": [],
                    "awardsToUpdate": [
                      { 
                        "sanctionSequence": 1,
                        "award": {
                           "sanctionType": "CC",
                           "sanctionStatus": "IMMEDIATE",
                           "sanctionDays": 3,
                           "effectiveDate": "2023-01-04"
                        }
                      },
                      { 
                        "sanctionSequence": 2,
                        "award": {
                           "sanctionType": "ASSO",
                           "sanctionStatus": "SUSPENDED",
                           "sanctionDays": 2,
                           "effectiveDate": "2023-02-04"
                        }
                      },
                      { 
                        "sanctionSequence": 3,
                        "award": {
                           "sanctionType": "EXTW",
                           "sanctionStatus": "IMMEDIATE",
                           "sanctionDays": 6,
                           "effectiveDate": "2023-01-04"
                        }
                      }
                    ]
                  },
                  "findingCode": "PROVED" 
                }
            """,
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("CC")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("ASSO")
          .jsonPath("sanctionStatus.code").isEqualTo("SUSPENDED")

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/awards/3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("sanctionType.code").isEqualTo("EXTW")
          .jsonPath("sanctionStatus.code").isEqualTo("IMMEDIATE")
      }
    }
    private fun anUnquashHearingResultAwardRequest(firstSanctionType: String = "CC", firstSanctionStatus: String = "IMMEDIATE"): String =
      // language=json
      """
      {
        "awards": {
          "awardsToUpdate": [
            { 
              "sanctionSequence": 1,
              "award": {
                 "sanctionType": "$firstSanctionType",
                 "sanctionStatus": "$firstSanctionStatus",
                 "sanctionDays": 3,
                 "effectiveDate": "2023-01-04"
              }
            },
            { 
              "sanctionSequence": 2,
              "award": {
                 "sanctionType": "ASSO",
                 "sanctionStatus": "SUSPENDED",
                 "sanctionDays": 2,
                 "effectiveDate": "2023-02-04"
              }
            },
            { 
              "sanctionSequence": 3,
              "award": {
                 "sanctionType": "EXTW",
                 "sanctionStatus": "IMMEDIATE",
                 "sanctionDays": 6,
                 "effectiveDate": "2023-01-04"
              }
            }
          ],
          "awardsToCreate": []
        },
        "findingCode": "PROVED"
      }
      """.trimIndent()
  }

  @DisplayName("DELETE /adjudications/adjudication-number/{adjudicationNumber}/charge/{chargeSequence}/awards")
  @Nested
  inner class DeleteHearingResultAwards {
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
    private lateinit var previousHearingResult: AdjudicationHearingResult

    @BeforeEach
    fun createPrisonerWithAdjudicationAndHearingAndAwards() {
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
                previousHearingResult = result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    sanctionIndex = 4,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 9,
                    effectiveDate = LocalDate.parse("2022-01-01"),
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
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
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
          .uri("/adjudications/adjudication-number/88888/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.delete().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/88/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Charge not found for adjudication number $existingAdjudicationNumber and charge sequence 88")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete awards and audit the deletions`() {
        val bookingId = prisoner.bookings.first().bookingId
        webTestClient.get().uri("/adjudications/hearings/${previousHearingResult.hearing.id}/charge/${previousHearingResult.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.resultAwards.size()").isEqualTo(1)

        webTestClient.get().uri("/adjudications/hearings/${existingHearingResult.hearing.id}/charge/${existingHearingResult.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.resultAwards.size()").isEqualTo(2)

        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/charge/${existingCharge.id.chargeSequence}/awards")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk

        // from another incident so ignored
        webTestClient.get().uri("/adjudications/hearings/${previousHearingResult.hearing.id}/charge/${previousHearingResult.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.resultAwards.size()").isEqualTo(1)

        webTestClient.get().uri("/adjudications/hearings/${existingHearingResult.hearing.id}/charge/${existingHearingResult.chargeSequence}/result")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.resultAwards.size()").isEqualTo(0)

        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-deleted"),
          check {
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("sanctionSequence", "5")
            assertThat(it).containsEntry("bookingId", bookingId.toString())
          },
          isNull(),
        )
        verify(telemetryClient).trackEvent(
          eq("hearing-result-award-deleted"),
          check {
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
            assertThat(it).containsEntry("sanctionSequence", "6")
            assertThat(it).containsEntry("bookingId", bookingId.toString())
          },
          isNull(),
        )
      }
    }
  }
}
