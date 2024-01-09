package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class SentencingResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = 0L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-cases/{id}")
  @Nested
  inner class GetCourtCase {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtCase: CourtCase
    private lateinit var courtCaseTwo: CourtCase
    private lateinit var offenderCharge1: OffenderCharge
    private val aDateString = "2023-01-01"
    private val aDateTimeString = "2023-01-01T10:30:00"
    private val aLaterDateString = "2023-01-05"
    private val aLaterDateTimeString = "2023-01-05T10:30:00"

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland =
          offender(nomsId = "A1234AB") {
            booking(agencyLocationId = "MDI") {
              courtCase = courtCase(
                reportingStaff = staff,
                beginDate = LocalDate.parse(aDateString),
                statusUpdateDate = LocalDate.parse(aDateString),
                statusUpdateStaff = staff,
              ) {
                offenderCharge1 = offenderCharge(offenceCode = "M1", plea = "G")
                val offenderCharge2 = offenderCharge()
                courtEvent() {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG", // overrides from the parent offender charge fields
                  )
                  courtEventCharge(
                    offenderCharge = offenderCharge2,
                  )
                  courtOrder() {
                    sentencePurpose(purposeCode = "REPAIR")
                    sentencePurpose(purposeCode = "PUNISH")
                  }
                }
              }
              courtCaseTwo = courtCase(
                reportingStaff = staff,
                beginDate = LocalDate.parse(aLaterDateString),
                statusUpdateDate = null,
                statusUpdateComment = null,
                statusUpdateReason = null,
                statusUpdateStaff = null,
                lidsCaseId = null,
                lidsCombinedCaseId = null,
                caseSequence = 2,
              ) {
                courtEvent(
                  commentText = null,
                  outcomeReasonCode = null,
                  judgeName = null,
                  directionCode = null,
                  nextEventStartTime = null,
                  nextEventDate = null,
                  nextEventRequestFlag = null,
                  orderRequestedFlag = null,
                  holdFlag = null,
                )
              }
            }
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if court case not found`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 11 not found")
      }

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court case and events`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("courtId").isEqualTo("COURT1")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("legalCaseType.code").isEqualTo("A")
          .jsonPath("legalCaseType.description").isEqualTo("Adult")
          .jsonPath("beginDate").isEqualTo(aDateString)
          .jsonPath("caseInfoNumber").isEqualTo("AB1")
          .jsonPath("statusUpdateComment").isEqualTo("a comment")
          .jsonPath("statusUpdateReason").isEqualTo("a reason")
          .jsonPath("statusUpdateDate").isEqualTo(aDateString)
          .jsonPath("statusUpdateStaffId").isEqualTo(staff.id)
          .jsonPath("lidsCaseNumber").isEqualTo(1)
          .jsonPath("lidsCaseId").isEqualTo(2)
          .jsonPath("lidsCombinedCaseId").isEqualTo(3)
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].id").exists()
          .jsonPath("courtEvents[0].offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("courtEvents[0].eventDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].startTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Trial")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.description").isEqualTo("In")
          .jsonPath("courtEvents[0].judgeName").isEqualTo("Mike")
          .jsonPath("courtEvents[0].courtId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode").isEqualTo("1046")
          .jsonPath("courtEvents[0].commentText").isEqualTo("Court event comment")
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].nextEventStartTime").isEqualTo(aLaterDateTimeString)
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
          .jsonPath("courtEvents[0].courtEventCharges[0].eventId").exists()
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].plea.description").isEqualTo("Not Guilty")
          .jsonPath("courtEvents[0].courtEventCharges[0].propertyValue").isEqualTo(3.2)
          .jsonPath("courtEvents[0].courtEventCharges[0].totalPropertyValue").isEqualTo(10)
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode1").isEqualTo("cj1")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode2").isEqualTo("cj2")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode3").isEqualTo("cj3")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.description").isEqualTo("Imprisonment")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode2.description")
          .isEqualTo("Detained during HM Pleasure")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1Indicator").isEqualTo("rci1")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode2Indicator").isEqualTo("rci2")
          .jsonPath("courtEvents[0].courtEventCharges[0].mostSeriousFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].courtOrders[0].id").exists()
          .jsonPath("courtEvents[0].courtOrders[0].orderType").isEqualTo("AUTO")
          .jsonPath("courtEvents[0].courtOrders[0].orderStatus").isEqualTo("A")
          .jsonPath("courtEvents[0].courtOrders[0].courtInfoId").isEqualTo("A12345")
          .jsonPath("courtEvents[0].courtOrders[0].issuingCourt").isEqualTo("MDI")
          .jsonPath("courtEvents[0].courtOrders[0].seriousnessLevel.description")
          .isEqualTo("High")
          .jsonPath("courtEvents[0].courtOrders[0].courtDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].courtOrders[0].requestDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].courtOrders[0].dueDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].courtOrders[0].commentText").isEqualTo("a court order comment")
          .jsonPath("courtEvents[0].courtOrders[0].nonReportFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].courtOrders[0].sentencePurposes[0].purposeCode").isEqualTo("REPAIR")
          .jsonPath("courtEvents[0].courtOrders[0].sentencePurposes[0].orderPartyCode").isEqualTo("CRT")
          .jsonPath("courtEvents[0].courtOrders[0].sentencePurposes[0].orderId").exists()
          .jsonPath("courtEvents[0].courtOrders[0].sentencePurposes[0].purposeCode").isEqualTo("REPAIR")
          .jsonPath("courtEvents[0].courtOrders[0].sentencePurposes[1].purposeCode").isEqualTo("PUNISH")
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges[0].offence.description").isEqualTo("Actual bodily harm")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1) // what is this?
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode2.description").isEqualTo("Detention Centre")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("r1")
          .jsonPath("offenderCharges[0].resultCode2Indicator").isEqualTo("r2")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Active")
          .jsonPath("offenderCharges[0].lidsOffenceNumber").isEqualTo(11)
      }

      @Test
      fun `will return the court case and events with minimal data`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCaseTwo.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("caseSequence").isEqualTo(2)
          .jsonPath("courtId").isEqualTo("COURT1")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("legalCaseType.code").isEqualTo("A")
          .jsonPath("legalCaseType.description").isEqualTo("Adult")
          .jsonPath("beginDate").isEqualTo(aLaterDateString)
          .jsonPath("caseInfoNumber").isEqualTo("AB1")
          .jsonPath("statusUpdateComment").doesNotExist()
          .jsonPath("statusUpdateReason").doesNotExist()
          .jsonPath("statusUpdateDate").doesNotExist()
          .jsonPath("statusUpdateStaffId").doesNotExist()
          .jsonPath("lidsCaseNumber").isEqualTo(1)
          .jsonPath("lidsCaseId").doesNotExist()
          .jsonPath("lidsCombinedCaseId").doesNotExist()
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].id").exists()
          .jsonPath("courtEvents[0].offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("courtEvents[0].eventDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].startTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Trial")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode").doesNotExist()
          .jsonPath("courtEvents[0].judgeName").doesNotExist()
          .jsonPath("courtEvents[0].courtId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode").doesNotExist()
          .jsonPath("courtEvents[0].commentText").doesNotExist()
          .jsonPath("courtEvents[0].orderRequestedFlag").doesNotExist()
          .jsonPath("courtEvents[0].holdFlag").doesNotExist()
          .jsonPath("courtEvents[0].nextEventRequestFlag").doesNotExist()
          .jsonPath("courtEvents[0].nextEventDate").doesNotExist()
          .jsonPath("courtEvents[0].nextEventStartTime").doesNotExist()
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(courtCase)
      repository.delete(courtCaseTwo)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-cases")
  @Nested
  inner class GetCourtCasesByOffender {
    private lateinit var staff: Staff
    private lateinit var prisoner1: Offender
    private lateinit var prisoner1Booking: OffenderBooking
    private lateinit var prisoner1Booking2: OffenderBooking
    private lateinit var prisoner2: Offender
    private lateinit var prisoner1CourtCase: CourtCase
    private lateinit var prisoner1CourtCase2: CourtCase
    private lateinit var prisoner2CourtCase: CourtCase

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner1 =
          offender(nomsId = "A1234AB") {
            prisoner1Booking = booking(agencyLocationId = "MDI") {
              prisoner1CourtCase = courtCase(
                reportingStaff = staff,
              ) {}
            }
            prisoner1Booking2 = booking(agencyLocationId = "MDI") {
              prisoner1CourtCase2 = courtCase(
                reportingStaff = staff,
              ) {}
            }
          }
        prisoner2 =
          offender(nomsId = "A1234AC") {
            booking(agencyLocationId = "MDI") {
              prisoner2CourtCase = courtCase(
                reportingStaff = staff,
              ) {}
            }
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court cases for the offender`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
          .jsonPath("$[0].offenderNo").isEqualTo(prisoner1.nomsId)
          .jsonPath("$[0].bookingId").isEqualTo(prisoner1Booking2.bookingId)
          .jsonPath("$[1].offenderNo").isEqualTo(prisoner1.nomsId)
          .jsonPath("$[1].bookingId").isEqualTo(prisoner1Booking.bookingId)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisoner1CourtCase)
      repository.delete(prisoner1CourtCase2)
      repository.delete(prisoner1)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/booking-id/{bookingId}/sentencing/court-cases")
  @Nested
  inner class GetCourtCasesByOffenderBooking {
    private lateinit var staff: Staff
    private lateinit var prisoner1: Offender
    private lateinit var prisoner1Booking: OffenderBooking
    private lateinit var prisoner1Booking2: OffenderBooking
    private lateinit var prisoner2: Offender
    private lateinit var prisoner1CourtCase: CourtCase
    private lateinit var prisoner1CourtCase2: CourtCase
    private lateinit var prisoner2CourtCase: CourtCase

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner1 =
          offender(nomsId = "A1234AB") {
            prisoner1Booking = booking(agencyLocationId = "MDI") {
              prisoner1CourtCase = courtCase(
                reportingStaff = staff,
              ) {}
            }
            prisoner1Booking2 = booking(agencyLocationId = "MDI") {
              prisoner1CourtCase2 = courtCase(
                reportingStaff = staff,
              ) {}
            }
          }
        prisoner2 =
          offender(nomsId = "A1234AC") {
            booking(agencyLocationId = "MDI") {
              prisoner2CourtCase = courtCase(
                reportingStaff = staff,
              ) {}
            }
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner1Booking.bookingId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner1Booking.bookingId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner1Booking.bookingId}/sentencing/court-cases")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner1Booking.bookingId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/booking-id/234/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender booking 234 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court cases for the offender booking`() {
        webTestClient.get().uri("/prisoners/booking-id/${prisoner1Booking.bookingId}/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(prisoner1.nomsId)
          .jsonPath("$[0].bookingId").isEqualTo(prisoner1Booking.bookingId)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisoner1CourtCase)
      repository.delete(prisoner1CourtCase2)
      repository.delete(prisoner1)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{seq}")
  @Nested
  inner class GetOffenderSentence {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var offenderCharge: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private val aDateString = "2023-01-01"
    private val aDateTimeString = "2023-01-01T10:30:00"
    private val aLaterDateString = "2023-01-05"
    private val aLaterDateTimeString = "2023-01-05T10:30:00"

    @BeforeEach
    internal fun createPrisonerAndSentence() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland =
          offender(nomsId = "A1234AB") {
            booking(agencyLocationId = "MDI") {
              courtCase = courtCase(reportingStaff = staff) {
                offenderCharge = offenderCharge(offenceCode = "M1")
                offenderCharge2 = offenderCharge(offenceDate = LocalDate.parse(aLaterDateString))
              }
              sentence = sentence(statusUpdateStaff = staff) {
                offenderSentenceCharge(offenderCharge = offenderCharge)
                offenderSentenceCharge(offenderCharge = offenderCharge2)
                term {}
                term(startDate = LocalDate.parse(aLaterDateString), days = 35)
              }
            }
          }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if sentence not found`() {
        webTestClient.get().uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Offender sentence for booking $latestBookingId and sentence sequence 11 not found")
      }

      @Test
      fun `will return 404 if offender booking not found`() {
        webTestClient.get().uri("/prisoners/booking-id/123/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender booking 123 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the offender sentence`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentence.id.sequence)
          .jsonPath("status").isEqualTo("I")
          .jsonPath("calculationType").isEqualTo("ADIMP_ORA")
          .jsonPath("category.code").isEqualTo("2003")
          .jsonPath("startDate").isEqualTo(aDateString)
          // .jsonPath("courtOrder").isEqualTo("I")
          .jsonPath("consecSequence").isEqualTo(2)
          .jsonPath("endDate").isEqualTo(aLaterDateString)
          .jsonPath("commentText").isEqualTo("a sentence comment")
          .jsonPath("absenceCount").isEqualTo(2)
          .jsonPath("etdCalculatedDate").isEqualTo("2023-01-02")
          .jsonPath("mtdCalculatedDate").isEqualTo("2023-01-03")
          .jsonPath("ltdCalculatedDate").isEqualTo("2023-01-04")
          .jsonPath("ardCalculatedDate").isEqualTo("2023-01-05")
          .jsonPath("crdCalculatedDate").isEqualTo("2023-01-06")
          .jsonPath("pedCalculatedDate").isEqualTo("2023-01-07")
          .jsonPath("npdCalculatedDate").isEqualTo("2023-01-08")
          .jsonPath("ledCalculatedDate").isEqualTo("2023-01-09")
          .jsonPath("sedCalculatedDate").isEqualTo("2023-01-10")
          .jsonPath("prrdCalculatedDate").isEqualTo("2023-01-11")
          .jsonPath("tariffCalculatedDate").isEqualTo("2023-01-12")
          .jsonPath("dprrdCalculatedDate").isEqualTo("2023-01-13")
          .jsonPath("tusedCalculatedDate").isEqualTo("2023-01-14")
          .jsonPath("aggSentenceSequence").isEqualTo(3)
          .jsonPath("aggAdjustDays").isEqualTo(6)
          .jsonPath("sentenceLevel").isEqualTo("AGG")
          .jsonPath("extendedDays").isEqualTo(4)
          .jsonPath("counts").isEqualTo(5)
          .jsonPath("statusUpdateReason").isEqualTo("update rsn")
          .jsonPath("statusUpdateComment").isEqualTo("update comment")
          .jsonPath("statusUpdateDate").isEqualTo("2023-01-05")
          .jsonPath("statusUpdateStaffId").isEqualTo(staff.id)
          .jsonPath("fineAmount").isEqualTo(12.5)
          .jsonPath("dischargeDate").isEqualTo("2023-01-05")
          .jsonPath("nomSentDetailRef").isEqualTo(11)
          .jsonPath("nomConsToSentDetailRef").isEqualTo(12)
          .jsonPath("nomConsFromSentDetailRef").isEqualTo(13)
          .jsonPath("nomConsWithSentDetailRef").isEqualTo(14)
          .jsonPath("lineSequence").isEqualTo(1)
          .jsonPath("hdcExclusionFlag").isEqualTo(true)
          .jsonPath("hdcExclusionReason").isEqualTo("hdc reason")
          .jsonPath("cjaAct").isEqualTo("A")
          .jsonPath("sled2Calc").isEqualTo("2023-01-20")
          .jsonPath("startDate2Calc").isEqualTo("2023-01-21")
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("sentenceTerms.size()").isEqualTo(2)
          .jsonPath("sentenceTerms[0].startDate").isEqualTo(aDateString)
          .jsonPath("sentenceTerms[0].endDate").isEqualTo(aLaterDateString)
          .jsonPath("sentenceTerms[0].years").isEqualTo(2)
          .jsonPath("sentenceTerms[0].months").isEqualTo(3)
          .jsonPath("sentenceTerms[0].weeks").isEqualTo(4)
          .jsonPath("sentenceTerms[0].days").isEqualTo(5)
          .jsonPath("sentenceTerms[0].hours").isEqualTo(6)
          .jsonPath("sentenceTerms[0].sentenceTermType.description").isEqualTo("Section 86 of 2000 Act")
          .jsonPath("sentenceTerms[0].lifeSentenceFlag").isEqualTo(true)
          .jsonPath("sentenceTerms[1].startDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges.size()").isEqualTo(2)
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge.id)
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges[0].offence.description").isEqualTo("Actual bodily harm")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode2.description").isEqualTo("Detention Centre")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("r1")
          .jsonPath("offenderCharges[0].resultCode2Indicator").isEqualTo("r2")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Active")
          .jsonPath("offenderCharges[1].offenceDate").isEqualTo(aLaterDateString)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(sentence)
      repository.delete(courtCase)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/sentencing/court-cases")
  inner class CreateCourtCase {
    private val offenderNo: String = "A1234AB"
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender

    @BeforeEach
    internal fun createPrisonerAndSentence() {
      nomisDataBuilder.build {
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI")
        }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when offender does not exist`() {
        webTestClient.post().uri("/prisoners/AB765/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found")
      }

      @Test
      internal fun `400 when legalCaseType not present in request`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "startDate": "2023-01-01",
              "court": "COURT1",
              "status": "A"
              }
              """,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      internal fun `400 when legalCaseType not valid`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(legalCaseType = "AXXX"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Legal Case Type AXXX not found")
      }
    }

    @Nested
    inner class CreateCourtCaseSuccess {
      @Test
      fun `can create a court case with minimal data`() {
        val courtCaseId = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/$courtCaseId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("courtId").isEqualTo("COURT1")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("legalCaseType.code").isEqualTo("A")
          .jsonPath("legalCaseType.description").isEqualTo("Adult")
          .jsonPath("beginDate").isEqualTo("2023-01-01")
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
      }

      @Test
      fun `can create multiple cases for the same prisoner`() {
        val firstCourtCaseId = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/$firstCourtCaseId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)

        val secondCourtCaseId = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/$secondCourtCaseId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(2)
      }

      @Test
      fun `can create a court case with court appearance`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        assertThat(courtCaseResponse.id).isNotNull()
        assertThat(courtCaseResponse.courtAppearanceIds.size).isEqualTo(2)
        assertThat(courtCaseResponse.courtAppearanceIds[0].courtEventChargesIds.size).isEqualTo(1)
        assertThat(courtCaseResponse.courtAppearanceIds[1].courtEventChargesIds.size).isEqualTo(1)

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("courtId").isEqualTo("COURT1")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("legalCaseType.code").isEqualTo("A")
          .jsonPath("legalCaseType.description").isEqualTo("Adult")
          .jsonPath("beginDate").isEqualTo("2023-01-01")
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].eventDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[0].startTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode").doesNotExist()
          .jsonPath("courtEvents[0].judgeName").doesNotExist()
          .jsonPath("courtEvents[0].courtId").isEqualTo("ABDRCT")
          .jsonPath("courtEvents[0].outcomeReasonCode").isEqualTo("ENT")
          .jsonPath("courtEvents[0].commentText").doesNotExist()
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDate").isEqualTo("2023-01-10")
          .jsonPath("courtEvents[0].nextEventStartTime").isEqualTo("2023-01-10T09:00:00")
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("M1")
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("offenderCharges[0].offence.offenceCode").isEqualTo("M1")
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("offenderCharges[0].offence.description").isEqualTo("Actual bodily harm")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)
          // when next court appearance details are provided, nomis creates a 2nd court appearance without an outcome
          .jsonPath("courtEvents[1].eventDate").isEqualTo("2023-01-10")
          .jsonPath("courtEvents[1].startTime").isEqualTo("2023-01-10T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[1].directionCode").doesNotExist()
          .jsonPath("courtEvents[1].judgeName").doesNotExist()
          .jsonPath("courtEvents[1].courtId").isEqualTo("COURT1")
          .jsonPath("courtEvents[1].outcomeReasonCode").doesNotExist()
          .jsonPath("courtEvents[1].commentText").doesNotExist()
          .jsonPath("courtEvents[1].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].nextEventRequestFlag").doesNotExist()
          .jsonPath("courtEvents[1].nextEventDate").doesNotExist()
          .jsonPath("courtEvents[1].nextEventStartTime").doesNotExist()
          .jsonPath("courtEvents[1].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[1].createdByUsername").isNotEmpty
          // court charges are copied from originating court appearance
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("M1")
          .jsonPath("courtEvents[1].courtEventCharges[0].offencesCount").isEqualTo(1)
      }

      @Test
      fun `will track telemetry for the create`() {
        val createResponse = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        verify(telemetryClient).trackEvent(
          eq("court-case-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("courtCaseId", createResponse.id.toString())
            assertThat(it).containsEntry("courtEventId", createResponse.courtAppearanceIds[0].id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "COURT1")
            assertThat(it).containsEntry("legalCaseType", "A")
          },
          isNull(),
        )
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.deleteOffenderChargeByBooking(latestBookingId)
    }

    private fun createCourtCaseRequestHierarchy(
      courtId: String = "COURT1",
      legalCaseType: String = "A",
      startDate: LocalDate = LocalDate.of(2023, 1, 1),
      status: String = "A",
      courtAppearance: CourtAppearanceRequest = createCourtAppearanceRequest(),
    ) =

      CreateCourtCaseRequest(
        courtId = courtId,
        legalCaseType = legalCaseType,
        startDate = startDate,
        status = status,
        courtAppearance = courtAppearance,
      )

    private fun createCourtAppearanceRequest(
      eventDate: LocalDate = LocalDate.of(2023, 1, 5),
      startTime: LocalDateTime = LocalDateTime.of(2023, 1, 5, 9, 0),
      courtId: String = "ABDRCT",
      courtEventType: String = "CRT",
      eventStatus: String = "SCH",
      outcomeReasonCode: String = "ENT",
      nextEventDate: LocalDate = LocalDate.of(2023, 1, 10),
      nextEventStartTime: LocalDateTime = LocalDateTime.of(2023, 1, 10, 9, 0),
      nextEventRequestFlag: Boolean = false,
      nextCourtId: String = "COURT1",
      courtEventCharges: MutableList<OffenderChargeRequest> = mutableListOf(createOffenderChargeRequest()),
    ) =
      CourtAppearanceRequest(
        eventDate = eventDate,
        startTime = startTime,
        courtId = courtId,
        courtEventType = courtEventType,
        eventStatus = eventStatus,
        nextEventDate = nextEventDate,
        nextEventStartTime = nextEventStartTime,
        nextEventRequestFlag = nextEventRequestFlag,
        outcomeReasonCode = outcomeReasonCode,
        courtEventCharges = courtEventCharges,
        nextCourtId = nextCourtId,
      )

    private fun createOffenderChargeRequest(
      offenceCode: String = "M1",
      statuteCode: String = "RC86",
      offencesCount: Int? = 1,
      offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
      offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 2),
      resultCode1: String? = "1002",
      mostSeriousFlag: Boolean = true,
    ) =
      OffenderChargeRequest(
        offenceCode = offenceCode,
        statuteCode = statuteCode,
        offencesCount = offencesCount,
        offenceDate = offenceDate,
        offenceEndDate = offenceEndDate,
        resultCode1 = resultCode1,
        mostSeriousFlag = mostSeriousFlag,
      )
  }
}
