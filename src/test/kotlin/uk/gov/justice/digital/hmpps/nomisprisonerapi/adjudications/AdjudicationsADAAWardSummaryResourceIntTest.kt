package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
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

class AdjudicationsADAAWardSummaryResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var aLocationInMoorland: AgencyInternalLocation

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI")
  }

  @DisplayName("GET /prisoners/booking-id/{bookingId}/awards/ada/summary")
  @Nested
  inner class GetAwardsAdaSummary {
    private val offenderNo = "A1965NM"
    private var bookingId = 0L
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var veryOldIncident: AdjudicationIncident
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var previousIncident: AdjudicationIncident
    private val veryOldAdjudicationNumber = 123454L
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
        veryOldIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        previousIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "LEI", bookingBeginDate = LocalDateTime.parse("1999-01-01T10:00"), active = false) {
            adjudicationParty(incident = veryOldIncident, adjudicationNumber = veryOldAdjudicationNumber) {
              val charge = charge(offenceCode = "51:1B")
              hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("1999-01-04"),
                hearingTime = LocalDateTime.parse("1999-01-04T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.INDEPENDENT_HEARING,
              ) {
                result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    sanctionIndex = 1,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 4,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 2,
                    statusCode = "SUSPENDED",
                    sanctionCode = "ADA",
                    sanctionDays = 5,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                }
              }
            }
          }
          bookingId = booking(agencyLocationId = "BXI", bookingBeginDate = LocalDateTime.parse("2022-01-01T10:00")) {
            adjudicationParty(incident = previousIncident, adjudicationNumber = previousAdjudicationNumber) {
              val charge = charge(offenceCode = "51:1A")
              hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2022-01-03"),
                hearingTime = LocalDateTime.parse("2022-01-03T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.INDEPENDENT_HEARING,
              ) {
                previousHearingResult = result(
                  charge = charge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    sanctionIndex = 1,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 3,
                    effectiveDate = LocalDate.parse("2022-01-03"),
                  )
                  award(
                    sanctionIndex = 2,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 3,
                    effectiveDate = LocalDate.parse("2022-01-03"),
                  )
                }
              }
            }
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              existingCharge = charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland.locationId,
                hearingDate = LocalDate.parse("2023-01-04"),
                hearingTime = LocalDateTime.parse("2023-01-04T15:00:00"),
                hearingTypeCode = AdjudicationHearingType.INDEPENDENT_HEARING,
              ) {
                existingHearingResult = result(
                  charge = existingCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    sanctionIndex = 3,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "CC",
                    sanctionDays = 2,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 4,
                    statusCode = "IMMEDIATE",
                    sanctionCode = "ADA",
                    sanctionDays = 4,
                    effectiveDate = LocalDate.parse("2023-01-04"),
                  )
                  award(
                    sanctionIndex = 5,
                    statusCode = "SUSPENDED",
                    sanctionCode = "ADA",
                    sanctionDays = 5,
                    effectiveDate = LocalDate.parse("2023-01-05"),
                  )
                  award(
                    sanctionIndex = 6,
                    statusCode = "PROSPECTIVE",
                    sanctionCode = "ADA",
                    sanctionDays = 15,
                    effectiveDate = LocalDate.parse("2023-02-04"),
                  )
                  award(
                    sanctionIndex = 7,
                    statusCode = "QUASHED",
                    sanctionCode = "ADA",
                    sanctionDays = 20,
                    effectiveDate = LocalDate.parse("2023-03-04"),
                  )
                  award(
                    sanctionIndex = 8,
                    statusCode = "QUASHED",
                    sanctionCode = "ADA",
                    sanctionMonths = 12,
                    sanctionDays = 10,
                    effectiveDate = LocalDate.parse("2023-03-04"),
                  )
                }
              }
            }
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(veryOldAdjudicationNumber)
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.deleteHearingByAdjudicationNumber(previousAdjudicationNumber)
      repository.delete(veryOldIncident)
      repository.delete(previousIncident)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if booking not found`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", 88888)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner with bookingId 88888 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return offender number related to booking`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(prisoner.nomsId)
      }

      @Test
      fun `there will be a summary for each ADA award across all adjudications for this booking`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries.size()").isEqualTo("6")
      }

      @Test
      fun `each ADA sanction is returned`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries[0].sanctionSequence").isEqualTo("1")
          .jsonPath("adaSummaries[1].sanctionSequence").isEqualTo("4")
          .jsonPath("adaSummaries[2].sanctionSequence").isEqualTo("5")
          .jsonPath("adaSummaries[3].sanctionSequence").isEqualTo("6")
          .jsonPath("adaSummaries[4].sanctionSequence").isEqualTo("7")
          .jsonPath("adaSummaries[5].sanctionSequence").isEqualTo("8")
      }

      @Test
      fun `each ADA sanction is returned regardless of status`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries[0].sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("adaSummaries[1].sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("adaSummaries[2].sanctionStatus.code").isEqualTo("SUSPENDED")
          .jsonPath("adaSummaries[3].sanctionStatus.code").isEqualTo("PROSPECTIVE")
          .jsonPath("adaSummaries[4].sanctionStatus.code").isEqualTo("QUASHED")
          .jsonPath("adaSummaries[5].sanctionStatus.code").isEqualTo("QUASHED")
      }

      @Test
      fun `summary of ADA is returned`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries[0].adjudicationNumber").isEqualTo(previousAdjudicationNumber)
          .jsonPath("adaSummaries[0].sanctionSequence").isEqualTo("1")
          .jsonPath("adaSummaries[0].days").isEqualTo("3")
          .jsonPath("adaSummaries[0].effectiveDate").isEqualTo("2022-01-03")
          .jsonPath("adaSummaries[0].sanctionStatus.code").isEqualTo("IMMEDIATE")
          .jsonPath("adaSummaries[0].sanctionStatus.description").isEqualTo("Immediate")
      }

      @Test
      fun `ADA days calculated from months as well as days`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries[5].days").isEqualTo("376") // 1 year (on leap year) + 10 days
      }
    }

    @Nested
    inner class NoAdjudications {
      private val noAdjudicationsOffenderNo = "A2965NM"
      private var noAdjudicationsBookingId = 0L
      private lateinit var noAdjudicationsPrisoner: Offender

      @BeforeEach
      fun createPrisonerWithAdjudicationAndHearingAndAwards() {
        nomisDataBuilder.build {
          noAdjudicationsPrisoner = offender(nomsId = noAdjudicationsOffenderNo) {
            noAdjudicationsBookingId = booking(agencyLocationId = "BXI").bookingId
          }
        }
      }

      @Test
      fun `will return offender number related to booking`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", noAdjudicationsBookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(noAdjudicationsPrisoner.nomsId)
      }

      @Test
      fun `will have no adjudication summaries`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/{bookingId}/awards/ada/summary", noAdjudicationsBookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("adaSummaries.size()").isEqualTo("0")
      }

      @AfterEach
      fun tearDown() {
        repository.delete(noAdjudicationsPrisoner)
      }
    }
  }
}
