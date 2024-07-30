package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ImprisonmentStatusChangeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SentencingResourceIntTest : IntegrationTestBase() {
  private val aDateString = "2023-01-01"
  private val aDateTimeString = "2023-01-01T10:30:00"
  private val aLaterDateString = "2023-01-05"
  private val aLaterDateTimeString = "2023-01-05T10:30:00"

  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = 0L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @SpyBean
  private lateinit var spRepository: StoredProcedureRepository

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
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                val offenderCharge2 = offenderCharge()
                courtEvent {
                  // overrides from the parent offender charge fields
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    offenderCharge = offenderCharge2,
                  )
                  courtOrder {
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
                  nextEventDateTime = null,
                  orderRequestedFlag = null,
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
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found or has no bookings")
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
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Trial")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[0].judgeName").isEqualTo("Mike")
          .jsonPath("courtEvents[0].courtId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("3514")
          .jsonPath("courtEvents[0].commentText").isEqualTo("Court event comment")
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDateTime").isEqualTo(aLaterDateTimeString)
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
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1Indicator").isEqualTo("rci1")
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
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1) // what is this?
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
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
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Trial")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[0].judgeName").doesNotExist()
          .jsonPath("courtEvents[0].courtId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode").doesNotExist()
          .jsonPath("courtEvents[0].commentText").doesNotExist()
          .jsonPath("courtEvents[0].orderRequestedFlag").doesNotExist()
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo("false")
          .jsonPath("courtEvents[0].nextEventDateTime").doesNotExist()
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /court-cases/{id}")
  @Nested
  inner class GetCourtCaseForMigration {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtCase: CourtCase
    private lateinit var courtCaseTwo: CourtCase
    private lateinit var offenderCharge1: OffenderCharge

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
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                val offenderCharge2 = offenderCharge()
                courtEvent {
                  // overrides from the parent offender charge fields
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    offenderCharge = offenderCharge2,
                  )
                  courtOrder {
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
                  nextEventDateTime = null,
                  orderRequestedFlag = null,
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
        webTestClient.get().uri("/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/court-cases/${courtCase.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if court case not found`() {
        webTestClient.get().uri("/court-cases/111")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 111 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court case and events`() {
        webTestClient.get().uri("/court-cases/${courtCase.id}")
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
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Trial")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[0].judgeName").isEqualTo("Mike")
          .jsonPath("courtEvents[0].courtId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("3514")
          .jsonPath("courtEvents[0].commentText").isEqualTo("Court event comment")
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDateTime").isEqualTo(aLaterDateTimeString)
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
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1Indicator").isEqualTo("rci1")
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
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1) // what is this?
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[0].lidsOffenceNumber").isEqualTo(11)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /court-cases/ids")
  @Nested
  inner class GetCourtCaseIdsForMigration {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/court-cases/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/court-cases/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private lateinit var staff: Staff
      private lateinit var prisoner1: Offender
      private lateinit var prisoner1Booking: OffenderBooking
      private lateinit var prisoner1Booking2: OffenderBooking
      private lateinit var prisoner2: Offender
      private lateinit var prisoner1CourtCase: CourtCase
      private lateinit var prisoner1CourtCase2: CourtCase
      private lateinit var prisoner1CourtCase3: CourtCase
      private lateinit var prisoner2CourtCase: CourtCase
      private val leedsCourtCasesNumberRange = 1..100
      private val casesAtLeeds: MutableList<CourtCase> = mutableListOf()

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
                ) {
                  audit(createDatetime = LocalDateTime.parse("2020-01-01T10:00"))
                }
                prisoner1CourtCase2 = courtCase(
                  reportingStaff = staff,
                  caseSequence = 2,
                ) {
                  audit(createDatetime = LocalDateTime.parse("2020-03-01T10:00"))
                }
              }
              alias {
                prisoner1Booking2 = booking(agencyLocationId = "MDI") {
                  prisoner1CourtCase3 = courtCase(
                    reportingStaff = staff,
                  ) {
                    audit(createDatetime = LocalDateTime.parse("2020-05-01T10:00"))
                  }
                }
              }
            }
          prisoner2 =
            offender(nomsId = "A1234AC") {
              booking(agencyLocationId = "LEI") {
                leedsCourtCasesNumberRange.forEachIndexed { index, it ->
                  casesAtLeeds.add(
                    courtCase(
                      reportingStaff = staff,
                      caseSequence = index,
                    ) {
                      audit(createDatetime = LocalDateTime.parse("2015-01-01T10:00").plusSeconds(index.toLong()))
                    },
                  )
                }
              }
            }
        }
      }

      @Test
      fun `will return total count when size is 1`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(103)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(103)
          .jsonPath("size").isEqualTo(1)
      }

      @Test
      fun `by default there will be a page size of 20`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(103)
          .jsonPath("numberOfElements").isEqualTo(20)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(6)
          .jsonPath("size").isEqualTo(20)
      }

      @Test
      fun `will order by case id ascending`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .queryParam("size", "300")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].caseId").isEqualTo(prisoner1CourtCase.id)
          .jsonPath("content[102].caseId").isEqualTo(casesAtLeeds[99].id)
      }

      @Test
      fun `supplying fromDate means only court cases created on or after that date are returned`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .queryParam("size", "200")
            .queryParam("fromDate", "2020-04-25")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(1)
      }

      @Test
      fun `supplying toDate means only court cases created on or before that date are returned`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .queryParam("size", "200")
            .queryParam("toDate", "2033-01-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(103)
      }

      @Test
      fun `can filter using both from and to dates`() {
        webTestClient.get().uri {
          it.path("/court-cases/ids")
            .queryParam("size", "200")
            .queryParam("fromDate", "2020-01-01")
            .queryParam("toDate", "2020-04-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(2)
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.deleteOffenders()
        repository.delete(staff)
      }
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
            alias {
              prisoner1Booking2 = booking(agencyLocationId = "MDI") {
                prisoner1CourtCase2 = courtCase(
                  reportingStaff = staff,
                ) {}
              }
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
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found or has no bookings")
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
      repository.deleteOffenders()
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
            alias {
              prisoner1Booking = booking(agencyLocationId = "MDI") {
                prisoner1CourtCase = courtCase(
                  reportingStaff = staff,
                ) {}
              }
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
      repository.deleteOffenders()
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
    private val aLaterDateString = "2023-01-05"

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
                offenderCharge = offenderCharge(offenceCode = "RT88074")
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
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[1].offenceDate").isEqualTo(aLaterDateString)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      // repository.delete(sentence)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/sentencing/court-cases")
  inner class CreateCourtCase {
    private val offenderNo: String = "A1234AB"
    private val offenderLeedsNo: String = "A1234AC"
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var prisonerAtLeeds: Offender

    @BeforeEach
    internal fun createPrisonerAndSentence() {
      nomisDataBuilder.build {
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.of(2023, 1, 1, 15, 30))
        }
        prisonerAtLeeds = offender(nomsId = offenderLeedsNo) {
          booking(agencyLocationId = "LEI", bookingBeginDate = LocalDateTime.of(2022, 1, 1, 15, 30)) {
            prisonTransfer(date = LocalDateTime.of(2023, 1, 2, 15, 30))
          }
        }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @AfterEach
    fun deletePrisoners() = repository.deleteOffenders()

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
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found or has no bookings")
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
      fun `can create a court case without a court appearance`() {
        val courtCaseId = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseWithoutAppearance(),
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
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  courtEventChargesToCreate = mutableListOf(
                    createOffenderChargeRequest(),
                    createOffenderChargeRequest(
                      offenceDate = LocalDate.of(2023, 1, 2),
                      offenceEndDate = LocalDate.of(2023, 1, 3),
                      offenceCode = "HP03001",
                      resultCode1 = "1002",
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        assertThat(courtCaseResponse.id).isGreaterThan(0)
        assertThat(courtCaseResponse.courtAppearanceIds.size).isEqualTo(2)
        assertThat(courtCaseResponse.courtAppearanceIds[0].id).isGreaterThan(0)
        assertThat(courtCaseResponse.courtAppearanceIds[1].id).isGreaterThan(0)
        assertThat(courtCaseResponse.courtAppearanceIds[0].courtEventChargesIds.size).isEqualTo(2)
        assertThat(courtCaseResponse.courtAppearanceIds[1].courtEventChargesIds.size).isEqualTo(2)
        assertThat(courtCaseResponse.courtAppearanceIds[0].courtEventChargesIds[0].offenderChargeId).isGreaterThan(0)
        assertThat(courtCaseResponse.courtAppearanceIds[0].courtEventChargesIds[1].offenderChargeId).isGreaterThan(0)

        // imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )

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
          .jsonPath("courtEvents[0].courtOrders[0].id").exists()
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[0].judgeName").doesNotExist()
          .jsonPath("courtEvents[0].courtId").isEqualTo("ABDRCT")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("1004")
          .jsonPath("courtEvents[0].commentText").doesNotExist()
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("offenderCharges[0].offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].resultCode1.code").isEqualTo("1067")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)
          // when next court appearance details are provided, nomis creates a 2nd court appearance without an outcome
          .jsonPath("courtEvents[1].courtOrders[0].id").doesNotExist()
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[1].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[1].judgeName").doesNotExist()
          .jsonPath("courtEvents[1].courtId").isEqualTo("COURT1")
          .jsonPath("courtEvents[1].outcomeReasonCode").doesNotExist()
          .jsonPath("courtEvents[1].commentText").doesNotExist()
          .jsonPath("courtEvents[1].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].nextEventDateTime").doesNotExist()
          .jsonPath("courtEvents[1].nextEventStartTime").doesNotExist()
          .jsonPath("courtEvents[1].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[1].createdByUsername").isNotEmpty
          // court charges are copied from originating court appearance
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[1].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceDate").isEqualTo("2023-01-02")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceEndDate").isEqualTo("2023-01-03")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenderCharge.offence.offenceCode").isEqualTo("HP03001")
          .jsonPath("courtEvents[1].courtEventCharges[1].offencesCount").isEqualTo(1)
      }

      @Test
      fun `will track telemetry for the create`() {
        val createResponse = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  courtEventChargesToCreate = mutableListOf(
                    createOffenderChargeRequest(),
                    createOffenderChargeRequest(
                      offenceDate = LocalDate.of(2023, 1, 2),
                      offenceEndDate = LocalDate.of(2023, 1, 3),
                      offenceCode = "HP03001",
                    ),
                  ),
                ),
              ),
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

      @Test
      fun `event status = completed if before or on the Booking start date`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  eventDateTime = LocalDateTime.of(2023, 1, 1, 9, 0),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-01T09:00:00")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Completed")
          // when next court appearance details are provided, nomis creates a 2nd court appearance without an outcome
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Scheduled (Approved)")
      }

      @Test
      fun `event status = scheduled if after the Booking start date`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  eventDateTime = LocalDateTime.of(2023, 1, 2, 9, 0),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-02T09:00:00")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          // next appearance
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Scheduled (Approved)")
      }

      @Test
      fun `event status = completed if event date before last movement`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  eventDateTime = LocalDateTime.of(2023, 1, 1, 9, 0),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderLeedsNo)
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-01T09:00:00")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Completed")
          // next appearance is after the last movement date, status is scheduled
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Scheduled (Approved)")
      }

      @Test
      fun `outcomes - use court event outcome if none provided for offender charge`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  outcomeReasonCode = "1002",
                  courtEventChargesToCreate = mutableListOf(
                    createOffenderChargeRequest(resultCode1 = null),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderLeedsNo)
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("1002")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.code").isEqualTo("1002")
          .jsonPath("offenderCharges[0].resultCode1.code").isEqualTo("1002")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Active")
      }

      @Test
      fun `outcomes - handles offender charge level result that differ from the appearance outcome`() {
        val courtCaseResponse = webTestClient.post().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtCaseRequestHierarchy(
                courtAppearance = createCourtAppearanceRequest(
                  outcomeReasonCode = "1002",
                  courtEventChargesToCreate = mutableListOf(
                    createOffenderChargeRequest(resultCode1 = "1005"),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateCourtCaseResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderLeedsNo/sentencing/court-cases/${courtCaseResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderLeedsNo)
          .jsonPath("courtEvents[0].id").value(Matchers.greaterThan(0))
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("1002")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.code").isEqualTo("1005")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1Indicator").isEqualTo("F")
          // offender charge has the additional chargeStatus, otherwise should be equal for a new court appearance
          .jsonPath("offenderCharges[0].resultCode1.code").isEqualTo("1005")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.deleteOffenderChargeByBooking(latestBookingId)
    }
  }

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances")
  inner class CreateCourtAppearance {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var staff: Staff
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.of(2023, 1, 5, 9, 0)) {
            courtCase = courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              offenderCharge1 = offenderCharge(resultCode1 = "1005", offenceCode = "RT88074", plea = "G")
              offenderCharge2 = offenderCharge(resultCode1 = "1067")
              courtEvent {
                // overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                  plea = "NG",
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
                courtOrder {
                  sentencePurpose(purposeCode = "REPAIR")
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
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
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
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
        webTestClient.post().uri("/prisoners/AB765/sentencing/court-cases/${courtCase.id}/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found or has no bookings")
      }

      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/1234/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 1234 for $offenderNo not found")
      }
    }

    @Nested
    inner class CreateCourtAppearanceSuccess {

      @Test
      fun `can add a new court appearance to a case`() {
        val courtAppearanceResponse =
          webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createCourtAppearanceRequest(
                  courtEventChargesToUpdate = mutableListOf(
                    createExistingOffenderChargeRequest(offenderChargeId = offenderCharge1.id),
                    createExistingOffenderChargeRequest(offenderChargeId = offenderCharge2.id),
                  ),
                  courtEventChargesToCreate = mutableListOf(
                    createOffenderChargeRequest(
                      offenceCode = "RT88077B",
                      resultCode1 = "1004",
                      offenceDate = LocalDate.of(2023, 1, 3),
                      offenceEndDate = LocalDate.of(2023, 1, 5),
                    ),
                    createOffenderChargeRequest(
                      offenceCode = "RT88083B",
                      offenceDate = LocalDate.of(2023, 2, 3),
                      offenceEndDate = LocalDate.of(2023, 2, 5),
                    ),
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateCourtAppearanceResponse::class.java)
            .returnResult().responseBody!!

        assertThat(courtAppearanceResponse.id).isGreaterThan(0)
        // only newly created Offender charges are returned - for mapping purposes
        assertThat(courtAppearanceResponse.courtEventChargesIds.size).isEqualTo(2)

        // imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[1].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[1].eventStatus.description").isEqualTo("Completed")
          .jsonPath("courtEvents[1].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[1].judgeName").doesNotExist()
          .jsonPath("courtEvents[1].courtId").isEqualTo("ABDRCT")
          .jsonPath("courtEvents[1].outcomeReasonCode.code").isEqualTo("1004")
          .jsonPath("courtEvents[1].commentText").doesNotExist()
          .jsonPath("courtEvents[1].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[1].nextEventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[1].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[1].createdByUsername").isNotEmpty
          .jsonPath("courtEvents[1].courtOrders[0].id").exists()
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[1].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenderCharge.offence.offenceCode").isEqualTo("RR84700")
          .jsonPath("courtEvents[1].courtEventCharges[1].offencesCount").isEqualTo(1)
          // confirm a second appearance has been created from the next event details
          .jsonPath("courtEvents[2].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[2].courtEventType.description").isEqualTo("Court Appearance")
          .jsonPath("courtEvents[2].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[2].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[2].judgeName").doesNotExist()
          .jsonPath("courtEvents[2].courtId").isEqualTo("COURT1")
          .jsonPath("courtEvents[2].outcomeReasonCode").doesNotExist()
          .jsonPath("courtEvents[2].commentText").doesNotExist()
          .jsonPath("courtEvents[2].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[2].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[2].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[2].nextEventDateTime").doesNotExist()
          .jsonPath("courtEvents[2].courtOrders[0].id").doesNotExist()
          .jsonPath("courtEvents[2].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[2].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[2].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[2].courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[2].courtEventCharges[0].resultCode1.code").isEqualTo("1005")
          .jsonPath("courtEvents[2].courtEventCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("courtEvents[2].courtEventCharges[1].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[2].courtEventCharges[1].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[2].courtEventCharges[1].offenderCharge.offence.offenceCode").isEqualTo("RR84700")
          .jsonPath("courtEvents[2].courtEventCharges[1].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[2].courtEventCharges[1].resultCode1.code").isEqualTo("1067")
          .jsonPath("courtEvents[2].courtEventCharges[1].resultCode1Indicator").isEqualTo("F")
          // the new offender charges
          .jsonPath("courtEvents[2].courtEventCharges[2].offenceDate").isEqualTo("2023-01-03")
          .jsonPath("courtEvents[2].courtEventCharges[2].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[2].courtEventCharges[2].offenderCharge.offence.offenceCode").isEqualTo("RT88077B")
          .jsonPath("courtEvents[2].courtEventCharges[2].offencesCount").isEqualTo(1)
          .jsonPath("courtEvents[2].courtEventCharges[2].resultCode1.code").isEqualTo("1004")
          .jsonPath("courtEvents[2].courtEventCharges[2].resultCode1Indicator").isEqualTo("F")
          .jsonPath("courtEvents[2].courtEventCharges[3].offenceDate").isEqualTo("2023-02-03")
          .jsonPath("courtEvents[2].courtEventCharges[3].offenceEndDate").isEqualTo("2023-02-05")
          .jsonPath("courtEvents[2].courtEventCharges[3].offenderCharge.offence.offenceCode").isEqualTo("RT88083B")
          // offender charges not updated for adding a new appearance using existing Offender charges
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[1].resultCode1.description")
          .isEqualTo("Bound Over to Leave the Island within 3 days")
          .jsonPath("offenderCharges[1].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[1].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[2].resultCode1.description")
          .isEqualTo("Restriction Order")
          .jsonPath("offenderCharges[2].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[2].chargeStatus.description").isEqualTo("Active")
      }

      @Test
      fun `will track telemetry for the create`() {
        val createResponse =
          webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createCourtAppearanceRequest(),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateCourtAppearanceResponse::class.java)
            .returnResult().responseBody!!

        verify(telemetryClient).trackEvent(
          eq("court-appearance-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", createResponse.id.toString())
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
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances/{id}")
  inner class UpdateCourtAppearance {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var staff: Staff
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.of(2023, 1, 5, 9, 0)) {
            courtCase = courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              offenderCharge1 = offenderCharge(resultCode1 = "1005", offenceCode = "RT88074", plea = "G")
              offenderCharge2 = offenderCharge(resultCode1 = "1067", offenceCode = "RR84700")
              offenderCharge3 = offenderCharge(resultCode1 = "1067", offenceCode = "RR84009")
              courtEvent = courtEvent(eventDateTime = LocalDateTime.of(2023, 1, 1, 10, 30)) {
                // overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                  plea = "NG",
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge3,
                )
                courtOrder {
                  sentencePurpose(purposeCode = "REPAIR")
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
              courtEvent2 = courtEvent(eventDateTime = LocalDateTime.of(2023, 2, 1, 10, 30)) {
                // overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                  plea = "NG",
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
                courtOrder {
                  sentencePurpose(purposeCode = "REPAIR")
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
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
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
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
        webTestClient.put()
          .uri("/prisoners/AB765/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found or has no bookings")
      }

      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.put().uri("/prisoners/$offenderNo/sentencing/court-cases/1234/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 1234 for $offenderNo not found")
      }

      @Test
      internal fun `404 when court appearance does not exist`() {
        webTestClient.put().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/1234")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court appearance 1234 for $offenderNo not found")
      }
    }

    @Nested
    inner class UpdateCourtAppearanceSuccess {

      @Test
      fun `can update a court appearance`() {
        val courtAppearanceResponse = webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                eventDateTime = LocalDateTime.of(2023, 1, 20, 14, 0),
                courtId = "LEEDYC",
                courtEventType = "19",
                outcomeReasonCode = "4506",
                nextEventDateTime = LocalDateTime.of(2023, 2, 20, 9, 0),
                nextCourtId = "LEICYC",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk.expectBody(CreateCourtAppearanceResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-20T14:00:00")
          .jsonPath("courtEvents[0].courtEventType.description").isEqualTo("Court Appearance - Police Product Order")
          .jsonPath("courtEvents[0].eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("courtEvents[0].directionCode.code").isEqualTo("OUT")
          .jsonPath("courtEvents[0].courtId").isEqualTo("LEEDYC")
          .jsonPath("courtEvents[0].outcomeReasonCode.code").isEqualTo("4506")

        // no new offender charges created
        assertThat(courtAppearanceResponse.courtEventChargesIds.size).isEqualTo(0)

        // imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
      }

      @Test
      fun `can update a court appearance next appearance fields without changing the generated next appearance`() {
        // create a court appearance with a next appearance date which will generate a second appearance using the provided date

        val appearanceResponse =
          webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createCourtAppearanceRequest(),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateCourtAppearanceResponse::class.java)
            .returnResult().responseBody!!

        // update the newly created appearance with next event date
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${appearanceResponse.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                eventDateTime = LocalDateTime.of(2023, 1, 20, 14, 0),
                courtId = "LEEDYC",
                courtEventType = "19",
                outcomeReasonCode = "4506",
                // next event date was previously 2023, 1, 10
                nextEventDateTime = LocalDateTime.of(2023, 2, 20, 9, 0),
                nextCourtId = "LEICYC",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          // should be 2 original appearance2 and the 2 just created
          .jsonPath("courtEvents.size()").isEqualTo(4)
          .jsonPath("courtEvents[2].id").isEqualTo(appearanceResponse.id)
          .jsonPath("courtEvents[2].eventDateTime").isEqualTo("2023-01-20T14:00:00")
          .jsonPath("courtEvents[2].nextEventDateTime").isEqualTo("2023-02-20T09:00:00")
          // check the appearance generated for the old dates is still there
          .jsonPath("courtEvents[3].id").isEqualTo(appearanceResponse.nextCourtAppearanceId!!)
          .jsonPath("courtEvents[3].eventDateTime").isEqualTo("2023-02-20T09:00:00")
          .jsonPath("courtEvents[3].nextEventDateTime").doesNotExist()
      }

      @Test
      fun `can update existing court event charges`() {
        val courtAppearanceResponse = webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    resultCode1 = "4508",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk.expectBody(UpdateCourtAppearanceResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo("2022-11-05")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo("2022-11-05")
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").isEqualTo(2)
          // as this is not the latest appearance, the underlying Offender Charge is not updated
          // 2 court event charges have been removed from the court appearance
          // 1 offender charge has been deleted as it is not referenced by other court appearance
          .jsonPath("offenderCharges[0].offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].resultCode1.code").isEqualTo("1005")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)

        // offender charges were updated here - no new ones
        assertThat(courtAppearanceResponse.createdCourtEventChargesIds.size).isEqualTo(0)
      }

      @Test
      fun `can delete existing court event charges and offender charges`() {
        // updating 1 out of 3 court event charges. Two are removed as court event charges, 1 offender charge is also removed as it is no longer referenced
        // by any court appearance in this case

        val courtAppearanceResponse = webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    resultCode1 = "4508",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk.expectBody(UpdateCourtAppearanceResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEvents[0].courtEventCharges.size()").isEqualTo(1)

        assertThat(courtAppearanceResponse.createdCourtEventChargesIds.size).isEqualTo(0)
        assertThat(courtAppearanceResponse.deletedOffenderChargesIds.size).isEqualTo(1)
        // no longer referenced by any court appearance in this case
        assertThat(courtAppearanceResponse.deletedOffenderChargesIds[0].offenderChargeId).isEqualTo(offenderCharge3.id)
      }

      @Test
      fun `can create new court event charges`() {
        val courtAppearanceResponse = webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToCreate = mutableListOf(
                  createOffenderChargeRequest(offenceCode = "VM08085"),
                  createOffenderChargeRequest(offenceCode = "BL19011"),
                ),
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                  ),
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge2.id,
                  ),
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge3.id,
                  ),
                ),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk.expectBody(UpdateCourtAppearanceResponse::class.java)
          .returnResult().responseBody!!

        val getResponse = webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectStatus().isOk.expectBody(CourtCaseResponse::class.java)
          .returnResult().responseBody!!

        val updatedCourtAppearance = getResponse.courtEvents[0]
        assertThat(updatedCourtAppearance.eventDateTime).isEqualTo(LocalDateTime.of(2023, 1, 5, 9, 0))
        assertThat(updatedCourtAppearance.courtEventCharges[3].offenceDate).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(updatedCourtAppearance.courtEventCharges[3].offenceEndDate).isEqualTo(LocalDate.of(2023, 1, 2))
        assertThat(updatedCourtAppearance.courtEventCharges[3].resultCode1!!.code).isEqualTo("1067")
        assertThat(updatedCourtAppearance.courtEventCharges[3].resultCode1Indicator).isEqualTo("F")
        assertThat(updatedCourtAppearance.courtEventCharges[3].offenderCharge.offence.offenceCode).isEqualTo("VM08085")

        assertThat(updatedCourtAppearance.courtEventCharges[4].offenderCharge.offence.offenceCode).isEqualTo("BL19011")

        assertThat(getResponse.offenderCharges[3].offence.offenceCode).isEqualTo("VM08085")
        assertThat(getResponse.offenderCharges[3].offenceDate).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(getResponse.offenderCharges[3].offenceEndDate).isEqualTo(LocalDate.of(2023, 1, 2))
        assertThat(getResponse.offenderCharges[3].resultCode1!!.code).isEqualTo("1067")
        assertThat(getResponse.offenderCharges[3].chargeStatus!!.code).isEqualTo("I")
        assertThat(getResponse.offenderCharges[3].resultCode1Indicator).isEqualTo("F")

        assertThat(getResponse.offenderCharges[4].offence.offenceCode).isEqualTo("BL19011")

        // 3 original charges updated, 2 new ones created
        // order of ids is important - must match the request order
        assertThat(offenderCharge1.id).isEqualTo(updatedCourtAppearance.courtEventCharges[0].offenderCharge.id)
        assertThat(courtAppearanceResponse.createdCourtEventChargesIds[0].offenderChargeId).isEqualTo(
          updatedCourtAppearance.courtEventCharges[3].offenderCharge.id,
        )
        assertThat(courtAppearanceResponse.createdCourtEventChargesIds[1].offenderChargeId).isEqualTo(
          updatedCourtAppearance.courtEventCharges[4].offenderCharge.id,
        )

        assertThat(courtAppearanceResponse.deletedOffenderChargesIds.size).isEqualTo(0)
      }

      @Test
      fun `can refresh offender charge with court event charge if updating the latest appearance`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    resultCode1 = "4508",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[1].eventDateTime").isEqualTo("2023-01-05T09:00:00")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceDate").isEqualTo("2022-11-05")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceEndDate").isEqualTo("2022-11-05")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RI64003")
          .jsonPath("courtEvents[1].courtEventCharges[0].offencesCount").isEqualTo(2)
          // updates underlying Offender charge including change of offence
          .jsonPath("offenderCharges[0].offence.offenceCode").isEqualTo("RI64003")
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo("2022-11-05")
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo("2022-11-05")
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Using or letting out for riding or associated purposes horse etc likely to suffer thereby")
          .jsonPath("offenderCharges[0].resultCode1.code").isEqualTo("4508")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(2)
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-appearance-updated"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", courtEvent.id.toString())
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
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-appearances/{id}")
  @Nested
  inner class GetCourtAppearance {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtAppearance: CourtEvent
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
              courtCase(reportingStaff = staff) {
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                val offenderCharge2 = offenderCharge()
                courtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    offenderCharge = offenderCharge2,
                  )
                }
              }
            }
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtAppearance.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if court case not found`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court appearance 11 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found or has no bookings")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court appearance`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").value(Matchers.greaterThan(0))
          .jsonPath("caseId").value(Matchers.greaterThan(0))
          .jsonPath("offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("eventDateTime").isEqualTo(aDateTimeString)
          .jsonPath("courtEventType.description").isEqualTo("Trial")
          .jsonPath("eventStatus.description").isEqualTo("Scheduled (Approved)")
          .jsonPath("directionCode.code").isEqualTo("OUT")
          .jsonPath("judgeName").isEqualTo("Mike")
          .jsonPath("courtId").isEqualTo("MDI")
          .jsonPath("outcomeReasonCode.code").isEqualTo("3514")
          .jsonPath("commentText").isEqualTo("Court event comment")
          .jsonPath("orderRequestedFlag").isEqualTo(false)
          .jsonPath("holdFlag").isEqualTo(false)
          .jsonPath("nextEventRequestFlag").isEqualTo(false)
          .jsonPath("nextEventDateTime").isEqualTo(aLaterDateTimeString)
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("courtEventCharges[0].eventId").exists()
          .jsonPath("courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEventCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("courtEventCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEventCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEventCharges[0].plea.description").isEqualTo("Not Guilty")
          .jsonPath("courtEventCharges[0].propertyValue").isEqualTo(3.2)
          .jsonPath("courtEventCharges[0].totalPropertyValue").isEqualTo(10)
          .jsonPath("courtEventCharges[0].cjitCode1").isEqualTo("cj1")
          .jsonPath("courtEventCharges[0].cjitCode2").isEqualTo("cj2")
          .jsonPath("courtEventCharges[0].cjitCode3").isEqualTo("cj3")
          .jsonPath("courtEventCharges[0].resultCode1.description").isEqualTo("Imprisonment")
          .jsonPath("courtEventCharges[0].resultCode1Indicator").isEqualTo("rci1")
          .jsonPath("courtEventCharges[0].mostSeriousFlag").isEqualTo(false)
          .jsonPath("courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEventCharges[0].offenderCharge.offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEventCharges[0].offenderCharge.offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEventCharges[0].offenderCharge.offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("courtEventCharges[0].offenderCharge.offencesCount").isEqualTo(1) // what is this?
          .jsonPath("courtEventCharges[0].offenderCharge.plea.description").isEqualTo("Guilty")
          .jsonPath("courtEventCharges[0].offenderCharge.propertyValue").isEqualTo(8.3)
          .jsonPath("courtEventCharges[0].offenderCharge.totalPropertyValue").isEqualTo(11)
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode1").isEqualTo("cj6")
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode2").isEqualTo("cj7")
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode3").isEqualTo("cj8")
          .jsonPath("courtEventCharges[0].offenderCharge.resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("courtEventCharges[0].offenderCharge.resultCode1Indicator").isEqualTo("F")
          .jsonPath("courtEventCharges[0].offenderCharge.mostSeriousFlag").isEqualTo(true)
          .jsonPath("courtEventCharges[0].offenderCharge.chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("courtEventCharges[0].offenderCharge.lidsOffenceNumber").isEqualTo(11)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(courtAppearance)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/offender-charges/{id}")
  @Nested
  inner class GetOffenderCharge {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtAppearance: CourtEvent
    private lateinit var offenderCharge1: OffenderCharge
    private val aDateString = "2023-01-01"
    private val aLaterDateString = "2023-01-05"

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland =
          offender(nomsId = "A1234AB") {
            booking(agencyLocationId = "MDI") {
              courtCase(reportingStaff = staff) {
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                val offenderCharge2 = offenderCharge()
                courtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    offenderCharge = offenderCharge2,
                  )
                }
              }
            }
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/${offenderCharge1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if offender charge not found`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender Charge 11 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found or has no bookings")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the offender charge`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenceDate").isEqualTo(aDateString)
          .jsonPath("offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offencesCount").isEqualTo(1) // what is this?
          .jsonPath("plea.description").isEqualTo("Guilty")
          .jsonPath("propertyValue").isEqualTo(8.3)
          .jsonPath("totalPropertyValue").isEqualTo(11)
          .jsonPath("cjitCode1").isEqualTo("cj6")
          .jsonPath("cjitCode2").isEqualTo("cj7")
          .jsonPath("cjitCode3").isEqualTo("cj8")
          .jsonPath("resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("resultCode1Indicator").isEqualTo("F")
          .jsonPath("mostSeriousFlag").isEqualTo(true)
          .jsonPath("chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("lidsOffenceNumber").isEqualTo(11)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(courtAppearance)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances/{id}")
  inner class UpdateCourtAppearanceDeleteCourtEventCharges {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent1: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var staff: Staff
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.of(2023, 1, 5, 9, 0)) {
            courtCase = courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              offenderCharge1 =
                offenderCharge(resultCode1 = "1005", offenceCode = "RT88074", plea = "G") // "Final" "Inactive"
              offenderCharge2 = offenderCharge(resultCode1 = "1067") // "Final" "Inactive"
              courtEvent1 = courtEvent {
                // overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
              }
              courtEvent2 = courtEvent {
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
              }
            }
          }
        }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class UpdateCourtAppearanceCourtEventChargeDeletionsSuccess {

      @Test
      fun `can remove all court event charges for an appearance`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                // empty list - remove all Court event charges for this appearance
                courtEventChargesToUpdate = mutableListOf(),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk

        // imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[1].id").isEqualTo(courtEvent2.id)
          .jsonPath("courtEvents[1].courtEventCharges.size()").isEqualTo(0)
          // court events charges remain for the first appearance
          .jsonPath("courtEvents[0].id").isEqualTo(courtEvent1.id)
          .jsonPath("courtEvents[0].courtEventCharges.size()").isEqualTo(2)
          // underlying offender charges are present
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenderCharges[1].id").isEqualTo(offenderCharge2.id)
      }

      @Test
      fun `will remove underlying offender charge if not referenced by any appearance in the case`() {
        // remove references to 1 of the 2 offender charges for all appearances in the court case
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                // keep one of the court event charges
                courtEventChargesToUpdate = mutableListOf(createExistingOffenderChargeRequest(offenderChargeId = offenderCharge1.id)),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk

        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                // keep one of the court event charges
                courtEventChargesToUpdate = mutableListOf(createExistingOffenderChargeRequest(offenderChargeId = offenderCharge1.id)),
              ),
            ),

          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].id").isEqualTo(courtEvent1.id)
          .jsonPath("courtEvents[0].courtEventCharges.size()").isEqualTo(1)
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEvents[1].id").isEqualTo(courtEvent2.id)
          .jsonPath("courtEvents[1].courtEventCharges.size()").isEqualTo(1)
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          // 1 remaining offender charge
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenderCharges[1]").doesNotExist()
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.deleteOffenderChargeByBooking(latestBookingId)
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances/{id}")
  inner class CourtOrderCreation {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent1: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private var latestBookingId: Long = 0
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var staff: Staff
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland = offender(nomsId = offenderNo) {
          booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.of(2023, 1, 5, 9, 0)) {
            courtCase = courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              offenderCharge1 = offenderCharge(
                resultCode1 = "1005",
                offenceCode = "RT88074",
                plea = "G",
              ) // no court order as Result code is Final and Inactive
              offenderCharge2 =
                offenderCharge(resultCode1 = "1067") // no court order as Result code is Final and Inactive

              offenderCharge3 =
                offenderCharge(resultCode1 = "1012") // court order as Result code is Final and Active
              courtEvent1 = courtEvent {
                // overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
              }
              courtEvent2 = courtEvent {
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge3,
                )
                courtOrder()
              }
            }
          }
        }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.deleteOffenderChargeByBooking(latestBookingId)
    }

    @Nested
    inner class CourtOrderCreationAndDeletionSuccess {

      @Test
      fun `changing a result to Final and Active will create a Court Order when none exists for the court appearance`() {
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEvents[0].courtOrders[0]").doesNotExist()

        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    // result code with Final disposition code and Active Charge status
                    resultCode1 = "1012",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].courtOrders[0].id").exists()
          .jsonPath("courtEvents[0].courtOrders[0].orderType").isEqualTo("AUTO")
          .jsonPath("courtEvents[0].courtOrders[0].orderStatus").isEqualTo("A")
          .jsonPath("courtEvents[0].courtOrders[0].issuingCourt").isEqualTo("ABDRCT")
          .jsonPath("courtEvents[0].courtOrders[0].courtDate").isEqualTo("2023-01-05")

        verify(telemetryClient).trackEvent(
          eq("court-order-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", courtEvent1.id.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `removing any Final and Active results will delete a Court Order for the court appearance`() {
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEvents[1].courtOrders[0]").exists()

        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    // result code with Final disposition code and Inactive Charge status
                    resultCode1 = "3502",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("courtEvents[0].courtOrders[0].id").doesNotExist()

        verify(telemetryClient).trackEvent(
          eq("court-order-deleted"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", courtEvent2.id.toString())
            assertThat(it).containsEntry("courtOrderId", courtEvent2.courtOrders[0].id.toString())
          },
          isNull(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisonerAtMoorland)
        repository.deleteOffenderChargeByBooking(latestBookingId)
      }
    }
  }

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/sentencing")
  inner class CreateSentence {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtCase: CourtCase
    private lateinit var courtAppearance: CourtEvent
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private var latestBookingId: Long = 0

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
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                offenderCharge2 = offenderCharge(offenceDate = LocalDate.parse(aLaterDateString))
                courtAppearance = courtEvent {
                  // overrides from the parent offender charge fields
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtOrder {
                    sentencePurpose(purposeCode = "REPAIR")
                    sentencePurpose(purposeCode = "PUNISH")
                  }
                }
              }
            }
          }
        latestBookingId = prisonerAtMoorland.latestBooking().bookingId
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
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
        webTestClient.post().uri("/prisoners/AB765/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found or has no bookings")
      }

      @Test
      internal fun `400 when category not valid`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id, category = "TREE"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence category TREE not found")
      }

      @Test
      internal fun `400 when calc type not valid`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id, calcType = "TREE"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence calculation with category 2020 and calculation type TREE not found")
      }

      @Test
      internal fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id, calcType = "ADIMP_ORA"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence calculation with category 2020 and calculation type ADIMP_ORA not found")
      }

      @Test
      internal fun `404 when offender charge id does not exist`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id, offenderChargeIds = mutableListOf(123)),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender Charge 123 for ${prisonerAtMoorland.nomsId} not found")
      }
    }

    @Nested
    inner class CreateSentenceSuccess {
      @Test
      fun `can create a sentence with data`() {
        val sentenceSeq = webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                caseId = courtCase.id,
                offenderChargeIds = mutableListOf(offenderCharge1.id, offenderCharge2.id),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
          .returnResult().responseBody!!.sentenceSeq

        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
        )

        webTestClient.get().uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/$sentenceSeq")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentenceSeq)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType").isEqualTo("ADIMP")
          .jsonPath("sentenceLevel").isEqualTo("IND")
          .jsonPath("category.code").isEqualTo("2020")
          .jsonPath("startDate").isEqualTo(aDateString)
          .jsonPath("endDate").isEqualTo(aLaterDateString)
          .jsonPath("fineAmount").isEqualTo("8.5")
          .jsonPath("courtOrder.id").isEqualTo(courtCase.courtEvents[0].courtOrders[0].id)
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("sentenceTerms.size()").isEqualTo(1)
          .jsonPath("sentenceTerms[0].startDate").isEqualTo(aDateString)
          .jsonPath("sentenceTerms[0].endDate").isEqualTo(aLaterDateString)
          .jsonPath("sentenceTerms[0].years").isEqualTo(7)
          .jsonPath("sentenceTerms[0].months").isEqualTo(2)
          .jsonPath("sentenceTerms[0].weeks").isEqualTo(3)
          .jsonPath("sentenceTerms[0].days").isEqualTo(4)
          .jsonPath("sentenceTerms[0].hours").isEqualTo(5)
          .jsonPath("sentenceTerms[0].sentenceTermType.description").isEqualTo("Imprisonment")
          .jsonPath("sentenceTerms[0].lifeSentenceFlag").isEqualTo(true)
          .jsonPath("offenderCharges.size()").isEqualTo(2)
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").isEqualTo(1)
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").isEqualTo(8.3)
          .jsonPath("offenderCharges[0].totalPropertyValue").isEqualTo(11)
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1Indicator").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[1].offenceDate").isEqualTo(aLaterDateString)
      }

      @Test
      fun `can create a sentence without a court order`() {
        // update charge (inactive charge) on the court case to remove court order
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventChargesToUpdate = mutableListOf(
                  createExistingOffenderChargeRequest(
                    offenderChargeId = offenderCharge1.id,
                    offenceDate = LocalDate.of(2022, 11, 5),
                    offenceEndDate = LocalDate.of(2022, 11, 5),
                    offenceCode = "RI64003",
                    // result code with Final disposition code and Inactive Charge status
                    resultCode1 = "3502",
                    offencesCount = 2,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        val sentenceSeq = webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                caseId = courtCase.id,
                offenderChargeIds = mutableListOf(offenderCharge1.id),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
          .returnResult().responseBody!!.sentenceSeq

        webTestClient.get().uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/$sentenceSeq")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentenceSeq)
          .jsonPath("courtOrder").doesNotExist()
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
      }

      @Test
      fun `will track telemetry for the create`() {
        val sentenceSeq = webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
          .returnResult().responseBody!!.sentenceSeq

        verify(telemetryClient).trackEvent(
          eq("sentence-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("sentenceSeq", sentenceSeq.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", prisonerAtMoorland.nomsId)
          },
          isNull(),
        )
      }
    }

    @AfterEach
    internal fun deleteSentence() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{sequence}")
  inner class UpdateSentence {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var sentenceTwo: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var newCourtCase: CourtCase
    private lateinit var offenderCharge: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private val aLaterDateString = "2023-01-05"

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
                offenderCharge = offenderCharge(offenceCode = "RT88074")
                offenderCharge2 = offenderCharge(offenceDate = LocalDate.parse(aLaterDateString))
              }
              newCourtCase = courtCase(reportingStaff = staff, caseSequence = 2) {
                offenderCharge = offenderCharge(offenceCode = "RT88080")
              }
              sentence = sentence(statusUpdateStaff = staff) {
                offenderSentenceCharge(offenderCharge = offenderCharge)
                offenderSentenceCharge(offenderCharge = offenderCharge2)
                term(startDate = LocalDate.parse(aLaterDateString), days = 35)
              }
              sentenceTwo = sentence(statusUpdateStaff = staff) {
                offenderSentenceCharge(offenderCharge = offenderCharge)
                term(startDate = LocalDate.parse(aLaterDateString), days = 20)
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
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when booking does not exist`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/9999/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender booking 9999 not found")
      }

      @Test
      internal fun `404 when sentence does not exist`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence for booking $latestBookingId and sentence sequence 5 not found")
      }

      @Test
      internal fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id, calcType = "ADIMP_ORA"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence calculation with category 2020 and calculation type ADIMP_ORA not found")
      }
    }

    @Nested
    inner class UpdateCourtAppearanceSuccess {

      @Test
      fun `can update a sentence`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                caseId = newCourtCase.id,
                calcType = "FTR_ORA",
                category = "1991",
                startDate = LocalDate.parse(aLaterDateString),
                endDate = null,
                sentenceLevel = "AGG",
                fine = BigDecimal.valueOf(9.7),

              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
        )

        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("caseId").isEqualTo(newCourtCase.id)
          .jsonPath("sentenceSeq").isEqualTo(sentenceTwo.id.sequence)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType").isEqualTo("FTR_ORA")
          .jsonPath("sentenceLevel").isEqualTo("AGG")
          .jsonPath("category.code").isEqualTo("1991")
          .jsonPath("startDate").isEqualTo(aLaterDateString)
          .jsonPath("endDate").doesNotExist()
          .jsonPath("fineAmount").isEqualTo("9.7")
          .jsonPath("createdDateTime").isNotEmpty
          // TODO update charges and term once DPS requirements known
          .jsonPath("offenderCharges.size()").isEqualTo(1)
          .jsonPath("sentenceTerms.size()").isEqualTo(1)
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(caseId = courtCase.id),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("sentence-updated"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("sentenceSequence", sentence.id.sequence.toString())
          },
          isNull(),
        )
      }
    }

    @AfterEach
    internal fun deleteSentence() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("DELETE /prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{sequence}")
  inner class DeleteSentence {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var offenderCharge: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private val aLaterDateString = "2023-01-05"

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
                offenderCharge = offenderCharge(offenceCode = "RT88074")
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
        webTestClient.delete()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `204 even when sentence does not exist`() {
      webTestClient.get().uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.delete().uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-delete-not-found"),
        org.mockito.kotlin.check {
          assertThat(it).containsEntry("sentenceSequence", "9999")
          assertThat(it).containsEntry("bookingId", latestBookingId.toString())
        },
        isNull(),
      )
    }

    @Test
    internal fun `204 when sentence does exist`() {
      webTestClient.get()
        .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete()
        .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will track telemetry for the delete`() {
      webTestClient.delete()
        .uri("/prisoners/booking-id/$latestBookingId/sentencing/sentence-sequence/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-deleted"),
        org.mockito.kotlin.check {
          assertThat(it).containsEntry("sentenceSequence", sentence.id.sequence.toString())
          assertThat(it).containsEntry("bookingId", latestBookingId.toString())
          assertThat(it).containsEntry("offenderNo", prisonerAtMoorland.nomsId)
        },
        isNull(),
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
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

  private fun createCourtCaseWithoutAppearance(
    courtId: String = "COURT1",
    legalCaseType: String = "A",
    startDate: LocalDate = LocalDate.of(2023, 1, 1),
    status: String = "A",
  ) =
    CreateCourtCaseRequest(
      courtId = courtId,
      legalCaseType = legalCaseType,
      startDate = startDate,
      status = status,
    )

  private fun createOffenderChargeRequest(
    offenceCode: String = "RT88074",
    offencesCount: Int? = 1,
    offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 2),
    resultCode1: String? = "1067",
    mostSeriousFlag: Boolean = true,
  ) =
    OffenderChargeRequest(
      offenceCode = offenceCode,
      offencesCount = offencesCount,
      offenceDate = offenceDate,
      offenceEndDate = offenceEndDate,
      resultCode1 = resultCode1,
    )

  private fun createExistingOffenderChargeRequest(
    offenderChargeId: Long,
    offenceCode: String = "RT88074",
    offencesCount: Int? = 1,
    offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 2),
    resultCode1: String? = "1067",
    mostSeriousFlag: Boolean = true,
  ) =
    ExistingOffenderChargeRequest(
      offenderChargeId = offenderChargeId,
      offenceCode = offenceCode,
      offencesCount = offencesCount,
      offenceDate = offenceDate,
      offenceEndDate = offenceEndDate,
      resultCode1 = resultCode1,
    )

  private fun createCourtAppearanceRequest(
    eventDateTime: LocalDateTime = LocalDateTime.of(2023, 1, 5, 9, 0),
    courtId: String = "ABDRCT",
    courtEventType: String = "CRT",
    outcomeReasonCode: String = "1004",
    nextEventDateTime: LocalDateTime = LocalDateTime.of(2023, 2, 20, 9, 0),
    nextCourtId: String = "COURT1",
    courtEventChargesToUpdate: MutableList<ExistingOffenderChargeRequest> = mutableListOf(),
    courtEventChargesToCreate: MutableList<OffenderChargeRequest> = mutableListOf(),
  ) =
    CourtAppearanceRequest(
      eventDateTime = eventDateTime,
      courtId = courtId,
      courtEventType = courtEventType,
      nextEventDateTime = nextEventDateTime,
      outcomeReasonCode = outcomeReasonCode,
      nextCourtId = nextCourtId,
      courtEventChargesToUpdate = courtEventChargesToUpdate,
      courtEventChargesToCreate = courtEventChargesToCreate,
    )

  private fun createSentence(
    caseId: Long,
    category: String = "2020",
    calcType: String = "ADIMP",
    startDate: LocalDate = LocalDate.parse(aDateString),
    endDate: LocalDate? = LocalDate.parse(aLaterDateString),
    status: String = "A",
    sentenceLevel: String = "IND",
    fine: BigDecimal? = BigDecimal.valueOf(8.5),
    offenderChargeIds: MutableList<Long> = mutableListOf(),
  ) =

    CreateSentenceRequest(
      startDate = startDate,
      status = status,
      endDate = endDate,
      sentenceCalcType = calcType,
      sentenceCategory = category,
      sentenceLevel = sentenceLevel,
      fine = fine,
      caseId = caseId,
      sentenceTerm = SentenceTermRequest(
        startDate = startDate,
        endDate = endDate,
        sentenceTermType = "IMP",
        lifeSentenceFlag = true,
        years = 7,
        months = 2,
        weeks = 3,
        days = 4,
        hours = 5,
      ),
      offenderChargeIds = offenderChargeIds,
    )
}
