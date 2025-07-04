package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason.Companion.RECALL_BREACH_HEARING
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode.Companion.RECALL_TO_PRISON
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.LinkCaseTxnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderFixedTermRecallRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ImprisonmentStatusChangeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SentencingResourceIntTest : IntegrationTestBase() {
  private val aDateString = "2023-01-01"
  private val aDateTimeString = "2023-01-01T10:30:00"
  private val aLaterDateString = "2023-01-05"
  private val aLaterDateTimeString = "2023-01-05T10:30:00"

  @Autowired
  lateinit var repository: Repository

  @Autowired
  lateinit var offenderSentenceRepository: OffenderSentenceRepository

  @Autowired
  lateinit var courtCaseRepository: CourtCaseRepository

  @Autowired
  lateinit var courtEventRepository: CourtEventRepository

  @Autowired
  lateinit var offenderFixedTermRecallRepository: OffenderFixedTermRecallRepository

  @Autowired
  lateinit var offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository

  @Autowired
  lateinit var linkCaseTxnRepository: LinkCaseTxnRepository

  private var aLocationInMoorland = 0L
  private lateinit var staff: Staff
  private lateinit var prisonerAtMoorland: Offender

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @MockitoSpyBean
  private lateinit var spRepository: StoredProcedureRepository

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
  }

  @AfterEach
  internal fun tearDown() {
    repository.deleteOffenders()
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-cases/{id}")
  @Nested
  inner class GetCourtCase {
    private lateinit var courtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
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
                  courtOrder = courtOrder {
                    sentencePurpose(purposeCode = "REPAIR")
                    sentencePurpose(purposeCode = "PUNISH")
                  }
                }
                sentence(
                  courtOrder = courtOrder,
                  calculationType = "AGG_IND_ORA",
                  category = "1991",
                )
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
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/1144")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 1144 not found")
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
          .jsonPath("primaryCaseInfoNumber").isEqualTo("AB1")
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
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].plea.description").isEqualTo("Not Guilty")
          .jsonPath("courtEvents[0].courtEventCharges[0].propertyValue").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode1").isEqualTo("cj1")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode2").isEqualTo("cj2")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode3").isEqualTo("cj3")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.description").isEqualTo("Imprisonment")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
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
          .jsonPath("offenderCharges[0].offencesCount").doesNotExist()
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[0].lidsOffenceNumber").isEqualTo(11)
          .jsonPath("sentences[0].calculationType.code").isEqualTo("AGG_IND_ORA")
          .jsonPath("sentences[0].category.code").isEqualTo("1991")
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
          .jsonPath("primaryCaseInfoNumber").isEqualTo("AB1")
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
    private lateinit var courtCase: CourtCase
    private lateinit var sourceCourtCase1: CourtCase
    private lateinit var sourceCourtCase2: CourtCase
    private lateinit var sourceOfSourceCourtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
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
                caseSequence = 1,
                reportingStaff = staff,
                beginDate = LocalDate.parse(aDateString),
                statusUpdateDate = LocalDate.parse(aDateString),
                statusUpdateStaff = staff,
              ) {
                offenderCaseIdentifier(reference = "caseRef1")
                offenderCaseIdentifier(reference = "caseRef2")
                offenderCaseIdentifier(reference = "caseRef3", type = "notOne")
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
                  courtOrder = courtOrder {
                    sentencePurpose(purposeCode = "REPAIR")
                    sentencePurpose(purposeCode = "PUNISH")
                  }
                }
                sentence(courtOrder = courtOrder)
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
              sourceCourtCase1 = courtCase(
                caseSequence = 3,
                reportingStaff = staff,
                combinedCase = courtCase,
              ) {
                val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G")
                courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge,
                    plea = "NG",
                  )
                }
              }
              sourceCourtCase2 = courtCase(
                caseSequence = 4,
                reportingStaff = staff,
                combinedCase = courtCase,
              ) {
                val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G")
                courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge,
                    plea = "NG",
                  )
                }
              }
              sourceOfSourceCourtCase = courtCase(
                caseSequence = 5,
                reportingStaff = staff,
                combinedCase = sourceCourtCase2,
              ) {
                val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G")
                courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge,
                    plea = "NG",
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
        webTestClient.get().uri("/court-cases/11111")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 11111 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `target case will contain all source case ids`() {
        val case: CourtCaseResponse = webTestClient.get().uri("/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(case.sourceCombinedCaseIds).containsExactlyInAnyOrder(sourceCourtCase1.id, sourceCourtCase2.id)
      }

      @Test
      fun `each source case will contain target case ids`() {
        val case1: CourtCaseResponse = webTestClient.get().uri("/court-cases/${sourceCourtCase1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()
        assertThat(case1.combinedCaseId).isEqualTo(courtCase.id)

        val case2: CourtCaseResponse = webTestClient.get().uri("/court-cases/${sourceCourtCase2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()
        assertThat(case2.combinedCaseId).isEqualTo(courtCase.id)
      }

      @Test
      fun `a case can be source and target so will contains both ids`() {
        val case2: CourtCaseResponse = webTestClient.get().uri("/court-cases/${sourceCourtCase2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()
        assertThat(case2.combinedCaseId).isEqualTo(courtCase.id)
        assertThat(case2.sourceCombinedCaseIds).containsExactly(sourceOfSourceCourtCase.id)
      }

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
          .jsonPath("primaryCaseInfoNumber").isEqualTo("AB1")
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
          .jsonPath("courtEvents[0].outcomeReasonCode.dispositionCode").isEqualTo("P")
          .jsonPath("courtEvents[0].outcomeReasonCode.conviction").isEqualTo("false")
          .jsonPath("courtEvents[0].commentText").isEqualTo("Court event comment")
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDateTime").isEqualTo(aLaterDateTimeString)
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
          .jsonPath("courtEvents[0].courtEventCharges[0].eventId").exists()
          .jsonPath("courtEvents[0].courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEvents[0].courtEventCharges[0].offencesCount").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].courtEventCharges[0].plea.description").isEqualTo("Not Guilty")
          .jsonPath("courtEvents[0].courtEventCharges[0].propertyValue").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode1").isEqualTo("cj1")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode2").isEqualTo("cj2")
          .jsonPath("courtEvents[0].courtEventCharges[0].cjitCode3").isEqualTo("cj3")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.description").isEqualTo("Imprisonment")
          .jsonPath("courtEvents[0].courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
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
          .jsonPath("offenderCharges[0].offencesCount").doesNotExist()
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[0].lidsOffenceNumber").isEqualTo(11)
          .jsonPath("caseInfoNumbers[0].reference").isEqualTo("caseRef1")
          .jsonPath("caseInfoNumbers[1].reference").isEqualTo("caseRef2")
          .jsonPath("caseInfoNumbers[2]").doesNotExist()
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
      private lateinit var prisoner1: Offender
      private lateinit var prisoner1Booking: OffenderBooking
      private lateinit var prisoner1Booking2: OffenderBooking
      private lateinit var prisoner2: Offender
      private lateinit var prisoner1CourtCase: CourtCase
      private lateinit var prisoner1CourtCase2: CourtCase
      private lateinit var prisoner1CourtCase3: CourtCase
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
                    audit(createDatetime = LocalDateTime.parse("2020-05-01T00:00"))
                  }
                }
              }
            }
          prisoner2 =
            offender(nomsId = "A1234AC") {
              booking(agencyLocationId = "LEI") {
                leedsCourtCasesNumberRange.forEachIndexed { index, _ ->
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
            .queryParam("fromDate", "2020-05-01")
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

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-cases/ids")
  @Nested
  inner class GetCourtCaseIdsForByOffenderForReconciliation {

    private lateinit var prisoner1: Offender
    private lateinit var prisoner1Booking: OffenderBooking
    private lateinit var prisoner1Booking2: OffenderBooking
    private lateinit var prisoner2: Offender
    private lateinit var prisoner1CourtCase: CourtCase
    private lateinit var prisoner1CourtCase2: CourtCase
    private lateinit var prisoner1CourtCase3: CourtCase

    @BeforeEach
    internal fun createPrisonerAndCourtCases() {
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
                )
              }
            }
          }
        prisoner2 =
          offender(nomsId = "A1234AC") {
            booking(agencyLocationId = "LEI")
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `will return all ids across bookings for offender`() {
        webTestClient.get().uri("/prisoners/${prisoner1.nomsId}/sentencing/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(3)
      }

      @Test
      fun `will return empty list if no court cases for offender`() {
        webTestClient.get().uri("/prisoners/${prisoner2.nomsId}/sentencing/court-cases/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
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

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-cases/post-merge")
  @Nested
  inner class GetCourtCasesChangedByMergePrisoners {
    private lateinit var prisonerWithRecentMerge: Offender
    private lateinit var prisonerWithRecentMergeCasesNotAffected: Offender
    private lateinit var prisonerWithOldMerge: Offender
    private lateinit var prisonerWithNoMerge: Offender

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      val mergeDate = LocalDateTime.parse("2002-01-01T12:00:00")
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerWithRecentMerge =
          offender(nomsId = "A1234AB") {
            booking(agencyLocationId = "MDI") {
              courtCase(
                caseInfoNumber = "A/100",
                reportingStaff = staff,
                caseStatus = "C",
                caseSequence = 3,
              ) {
// case added after a merge (very rare but would happen if the merge event was delayed for some reason)
                audit(createDatetime = mergeDate.plusMinutes(2), auditModule = "OCDCCASE")
              }
              courtCase(
                caseInfoNumber = "A/100",
                reportingStaff = staff,
                caseStatus = "C",
                caseSequence = 2,
              ) {
// case added by merge and then amened in NOMIS (very rare assuming event is process immediately)
                audit(
                  createDatetime = mergeDate.plusMinutes(1),
                  modifyDatetime = mergeDate.plusMinutes(2),
                  auditModule = "OCDCCASE",
                  createUserId = "SYS",
                )
              }
              courtCase(
                caseInfoNumber = "A/123",
                reportingStaff = staff,
                caseStatus = "A",
                caseSequence = 1,
              ) {
// case added by merge but never amended
                audit(createDatetime = mergeDate.plusMinutes(1), auditModule = "MERGE", createUserId = "SYS")
              }
            }
            booking(agencyLocationId = "MDI") {
              release()
              courtCase(
                caseInfoNumber = "A/100",
                reportingStaff = staff,
                caseStatus = "I",
                caseSequence = 2,
              ) {
// original case cloned by MERGE and made inactive
                audit(
                  createDatetime = mergeDate.minusDays(10),
                  modifyDatetime = mergeDate.plusMinutes(1),
                  auditModule = "MERGE",
                )
              }
              courtCase(
                caseInfoNumber = "A/123",
                reportingStaff = staff,
                caseStatus = "I",
                caseSequence = 1,
              ) {
// original case cloned by MERGE and made inactive
                audit(
                  createDatetime = mergeDate.minusDays(10),
                  modifyDatetime = mergeDate.plusMinutes(1),
                  auditModule = "MERGE",
                )
              }
            }
            booking(agencyLocationId = "MDI") {
              release()
              courtCase(
                caseInfoNumber = "A/321",
                reportingStaff = staff,
                caseSequence = 1,
              ) {
// some other old case
                audit(createDatetime = mergeDate.minusYears(1), modifyDatetime = mergeDate.minusYears(1))
              }
            }
          }
        prisonerWithRecentMergeCasesNotAffected =
          offender(nomsId = "A1234AC") {
            booking(agencyLocationId = "MDI") {
              courtCase(
                caseInfoNumber = "B/123",
                reportingStaff = staff,
                caseSequence = 2,
              ) {
// court case created after merge
                audit(createDatetime = mergeDate.plusMinutes(1))
              }
              courtCase(
                caseInfoNumber = "B/321",
                reportingStaff = staff,
                caseSequence = 1,
              ) {
// court case created before merge
                audit(createDatetime = mergeDate.minusDays(1))
              }
            }
            booking(agencyLocationId = "MDI") {
              release()
              courtCase(
                caseInfoNumber = "B/456",
                reportingStaff = staff,
                caseSequence = 1,
              ) {
                audit(createDatetime = mergeDate.minusDays(10), modifyDatetime = mergeDate.minusDays(2))
              }
            }
          }
        prisonerWithOldMerge =
          offender(nomsId = "A1234AD") {
            booking(agencyLocationId = "MDI") {
              courtCase(
                caseInfoNumber = "C/123",
                reportingStaff = staff,
                caseSequence = 1,
              ) {
// court case created after first merge but before recent merge
                audit(createDatetime = mergeDate.minusDays(10), auditModule = "MERGE", createUserId = "SYS")
              }
            }
            booking(agencyLocationId = "MDI") {
              release()
              courtCase(
                caseInfoNumber = "C/123",
                reportingStaff = staff,
                caseSequence = 1,
              ) {
// original case that was clone from previous merge
                audit(
                  createDatetime = mergeDate.minusDays(11),
                  modifyDatetime = mergeDate.minusDays(10),
                  auditModule = "MERGE",
                )
              }
            }
          }
        prisonerWithNoMerge =
          offender(nomsId = "A1234AE") {
            booking(agencyLocationId = "MDI") {
              courtCase(
                reportingStaff = staff,
                caseSequence = 1,
              ) {}
            }
          }

        mergeTransaction(
          requestDate = mergeDate,
          nomsId1 = "A9999AK",
          nomsId2 = "A1234AB",
        )
        mergeTransaction(
          requestDate = mergeDate,
          nomsId1 = "A9999AK",
          nomsId2 = "A1234AC",
        )
        mergeTransaction(
          requestDate = mergeDate,
          nomsId1 = "A1234AD",
          nomsId2 = "A9999AK",
        )
        mergeTransaction(
          requestDate = mergeDate.minusDays(10),
          nomsId1 = "A1234AD",
          nomsId2 = "A9999AK",
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisonerWithRecentMerge.nomsId}/sentencing/court-cases/post-merge")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisonerWithRecentMerge.nomsId}/sentencing/court-cases/post-merge")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisonerWithRecentMerge.nomsId}/sentencing/court-cases/post-merge")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/${prisonerWithRecentMerge.nomsId}/sentencing/court-cases/post-merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will fail if there has never been a merge for this prisoner`() {
        webTestClient.get().uri("/prisoners/${prisonerWithNoMerge.nomsId}/sentencing/court-cases/post-merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the created and amended court cases for the offender`() {
        val response: PostPrisonerMergeCaseChanges =
          webTestClient.get().uri("/prisoners/${prisonerWithRecentMerge.nomsId}/sentencing/court-cases/post-merge")
            .headers(setAuthorisation(roles = listOf("NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk.expectBodyResponse()

        assertThat(response.courtCasesDeactivated).hasSize(2)
        with(response.courtCasesDeactivated.find { it.caseSequence == 1 }!!) {
          assertThat(this.caseStatus.code).isEqualTo("I")
          assertThat(this.primaryCaseInfoNumber).isEqualTo("A/123")
        }
        with(response.courtCasesDeactivated.find { it.caseSequence == 2 }!!) {
          assertThat(this.caseStatus.code).isEqualTo("I")
          assertThat(this.primaryCaseInfoNumber).isEqualTo("A/100")
        }
        assertThat(response.courtCasesCreated).hasSize(2)
        with(response.courtCasesCreated.find { it.caseSequence == 1 }!!) {
          assertThat(this.caseStatus.code).isEqualTo("A")
          assertThat(this.primaryCaseInfoNumber).isEqualTo("A/123")
        }
        with(response.courtCasesCreated.find { it.caseSequence == 2 }!!) {
          assertThat(this.caseStatus.code).isEqualTo("C")
          assertThat(this.primaryCaseInfoNumber).isEqualTo("A/100")
        }
      }

      @Test
      fun `will not return anything if merge didn't copy any cases`() {
        val response: PostPrisonerMergeCaseChanges = webTestClient.get()
          .uri("/prisoners/${prisonerWithRecentMergeCasesNotAffected.nomsId}/sentencing/court-cases/post-merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(response.courtCasesDeactivated).hasSize(0)
        assertThat(response.courtCasesCreated).hasSize(0)
      }

      @Test
      fun `will not return anything when the merge happened before cases where added`() {
        val response: PostPrisonerMergeCaseChanges =
          webTestClient.get().uri("/prisoners/${prisonerWithOldMerge.nomsId}/sentencing/court-cases/post-merge")
            .headers(setAuthorisation(roles = listOf("NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk.expectBodyResponse()

        assertThat(response.courtCasesDeactivated).hasSize(0)
        assertThat(response.courtCasesCreated).hasSize(0)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.deleteOffenders()
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{seq}")
  @Nested
  inner class GetOffenderSentence {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var sentenceWithBadConsecutiveData: OffenderSentence
    private lateinit var recallSentence: OffenderSentence
    private lateinit var inactiveRecallSentence: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var appearance: CourtEvent
    private lateinit var courtOrder: CourtOrder
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
                appearance = courtEvent {
                  courtEventCharge(offenderCharge = offenderCharge)
                  courtEventCharge(offenderCharge = offenderCharge2)
                  courtOrder = courtOrder()
                }
                sentence = sentence(statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term {}
                  term(days = 35)
                }

                sentenceWithBadConsecutiveData = sentence(statusUpdateStaff = staff, courtOrder = courtOrder, status = "A", consecSequence = 999) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
                  term(days = 35)
                }
                recallSentence = sentence(
                  statusUpdateStaff = staff,
                  courtOrder = courtOrder,
                  calculationType = "FTR_ORA",
                  category = "2003",
                  status = "A",
                ) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
                }
                inactiveRecallSentence = sentence(
                  statusUpdateStaff = staff,
                  courtOrder = courtOrder,
                  calculationType = "FTR_ORA",
                  category = "2003",
                  status = "I",
                ) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
                }
              }
              fixedTermRecall(
                returnToCustodyDate = LocalDate.parse("2024-01-01"),
                staff = staff,
                comments = "Fixed term recall",
                recallLength = 14,
              )
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
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if sentence not found`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Offender sentence for booking $latestBookingId and sentence sequence 11 not found")
      }

      @Test
      fun `will return 404 if case not found`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/54321/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 54321 for A1234AB not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the offender sentence`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentence.id.sequence)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType.code").isEqualTo("ADIMP_ORA")
          .jsonPath("calculationType.description").isEqualTo("ORA CJA03 Standard Determinate Sentence")
          .jsonPath("category.code").isEqualTo("2003")
          .jsonPath("startDate").isEqualTo(aDateString)
          .jsonPath("courtOrder.eventId").isEqualTo(appearance.id)
          .jsonPath("courtOrder.courtDate").isEqualTo(appearance.eventDate.toString())
          .jsonPath("consecSequence").doesNotExist()
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
          .jsonPath("prisonId").isEqualTo(prisonerAtMoorland.latestBooking().location.id)
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("sentenceTerms.size()").isEqualTo(2)
          .jsonPath("sentenceTerms[0].startDate").isEqualTo(courtOrder.courtDate.toString())
          .jsonPath("sentenceTerms[0].endDate").doesNotExist()
          .jsonPath("sentenceTerms[0].years").isEqualTo(2)
          .jsonPath("sentenceTerms[0].months").isEqualTo(3)
          .jsonPath("sentenceTerms[0].weeks").isEqualTo(4)
          .jsonPath("sentenceTerms[0].days").isEqualTo(5)
          .jsonPath("sentenceTerms[0].hours").isEqualTo(6)
          .jsonPath("sentenceTerms[0].sentenceTermType.description").isEqualTo("Section 86 of 2000 Act")
          .jsonPath("sentenceTerms[0].lifeSentenceFlag").isEqualTo(true)
          .jsonPath("sentenceTerms[1].startDate").isEqualTo(courtOrder.courtDate.toString())
          .jsonPath("offenderCharges.size()").isEqualTo(2)
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge.id)
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").doesNotExist()
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[1].offenceDate").isEqualTo(aLaterDateString)
          .jsonPath("missingCourtOffenderChargeIds.size()").isEqualTo(0)
      }

      @Test
      fun `will return the offender sentence without the consecutive sequence if the target consecutive sentence does not exist `() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentenceWithBadConsecutiveData.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("consecSequence").doesNotExist()
      }

      @Test
      fun `will return the fixed term recall data offender sentence`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${recallSentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(recallSentence.id.sequence)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType.code").isEqualTo("FTR_ORA")
          .jsonPath("calculationType.description").isEqualTo("ORA 28 Day Fixed Term Recall")
          .jsonPath("recallCustodyDate.returnToCustodyDate").isEqualTo("2024-01-01")
          .jsonPath("recallCustodyDate.recallLength").isEqualTo(14)
          .jsonPath("recallCustodyDate.comments").isEqualTo("Fixed term recall")
      }

      @Test
      fun `will not return the fixed term recall data if the recall sentence is not active`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${inactiveRecallSentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(inactiveRecallSentence.id.sequence)
          .jsonPath("status").isEqualTo("I")
          .jsonPath("calculationType.code").isEqualTo("FTR_ORA")
          .jsonPath("calculationType.description").isEqualTo("ORA 28 Day Fixed Term Recall")
          .jsonPath("recallCustodyDate").doesNotExist()
      }

      @Test
      fun `will not return the fixed term recall data if the sentence is not a recall`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentence.id.sequence)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType.code").isEqualTo("ADIMP_ORA")
          .jsonPath("calculationType.description").isEqualTo("ORA CJA03 Standard Determinate Sentence")
          .jsonPath("recallCustodyDate").doesNotExist()
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
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
              createCourtCase(),
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
              createCourtCase(),
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
              createCourtCase(),
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
              createCourtCase(),
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
              createCourtCase(legalCaseType = "AXXX"),
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
              createCourtCase(),
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
              createCourtCase(),
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
              createCourtCase(),
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
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found")
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
                  courtEventCharges = mutableListOf(
                    offenderCharge1.id,
                    offenderCharge2.id,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateCourtAppearanceResponse::class.java)
            .returnResult().responseBody!!

        assertThat(courtAppearanceResponse.id).isGreaterThan(0)

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
// TODO should a charge request with an order producing ResultCode not be used ?  (currently using value from underlying offender charge)
          .jsonPath("courtEvents[1].courtOrders[0].id").doesNotExist()
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[1].courtEventCharges[0].offenderCharge.offence.offenceCode").isEqualTo("RT88074")
          .jsonPath("courtEvents[1].courtEventCharges[0].offencesCount").doesNotExist()
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenceEndDate").isEqualTo("2023-01-05")
          .jsonPath("courtEvents[1].courtEventCharges[1].offenderCharge.offence.offenceCode").isEqualTo("RR84700")
          .jsonPath("courtEvents[1].courtEventCharges[1].offencesCount").doesNotExist()
// confirm a second appearance has NOT been created from the next event details
          .jsonPath("courtEvents[2]").doesNotExist()
// offender charges not updated for adding a new appearance using existing Offender charges
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Inactive")
          .jsonPath("offenderCharges[1].resultCode1.description")
          .isEqualTo("Bound Over to Leave the Island within 3 days")
          .jsonPath("offenderCharges[1].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[1].chargeStatus.description").isEqualTo("Inactive")
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
          check {
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
  @DisplayName("POST /prisoners/{offenderNo}/sentencing/court-cases/{id}/charges")
  inner class CreateOffenderCharge {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private var latestBookingId: Long = 0

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
            )
          }
        }
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/charges")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/charges")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/charges")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
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
        webTestClient.post().uri("/prisoners/AB765/sentencing/court-cases/${courtCase.id}/charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found")
      }

      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/1234/charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 1234 for $offenderNo not found")
      }
    }

    @Nested
    inner class CreateChargeSuccess {

      @Test
      fun `can add a new offender charge to a case`() {
        val chargeResponse =
          webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/charges")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createOffenderChargeRequest(resultCode1 = "1081"),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(OffenderChargeIdResponse::class.java)
            .returnResult().responseBody!!

        assertThat(chargeResponse.offenderChargeId).isGreaterThan(0)

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("caseSequence").isEqualTo(1)
// the new offender charges
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Detention and Training Order")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Active")

// imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
      }

      @Test
      fun `will track telemetry for the create`() {
        val createResponse =
          webTestClient.post().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/charges")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createOffenderChargeRequest(),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(OffenderChargeIdResponse::class.java)
            .returnResult().responseBody!!

        verify(telemetryClient).trackEvent(
          eq("offender-charge-created"),
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("offenderChargeId", createResponse.offenderChargeId.toString())
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
  @DisplayName("PUT /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances/charges/{id}")
  inner class UpdateCourtCharge {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent: CourtEvent
    private lateinit var earlierCourtEvent: CourtEvent
    private var latestBookingId: Long = 0
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
              offenderCharge1 = offenderCharge(resultCode1 = "1004", offenceCode = "RR84005B", plea = "G")
              offenderCharge2 = offenderCharge(resultCode1 = "1067", offenceCode = "RR84700")
              offenderCharge3 = offenderCharge(resultCode1 = "1067", offenceCode = "RR84009")
              courtEvent = courtEvent(eventDateTime = LocalDateTime.of(2023, 1, 1, 10, 30)) {
// overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                  resultCode1 = "1002",
                  offenceDate = offenderCharge1.offenceDate,
                  plea = "NG",
                )
                courtEventCharge(
                  offenderCharge = offenderCharge2,
                  resultCode1 = "1067",
                )
                courtEventCharge(
                  offenderCharge = offenderCharge3,
                  resultCode1 = "1067",
                )
              }
              earlierCourtEvent = courtEvent(eventDateTime = LocalDateTime.of(2022, 1, 1, 10, 30)) {
// overrides from the parent offender charge fields
                courtEventCharge(
                  offenderCharge = offenderCharge1,
                )
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
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
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
          .uri("/prisoners/AB765/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found")
      }

      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/1234/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 1234 for $offenderNo not found")
      }
    }

    @Nested
    inner class UpdateCourtChargeSuccess {

      @Test
      fun `can update the court event charge and offender charge on the latest appearance`() {
// confirming that an initial court order does not exist
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtOrders[0].id").doesNotExist()

        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(resultCode1 = "2060"),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
// offence is not updated
          .jsonPath("offence.offenceCode").isEqualTo("RR84005B")
          .jsonPath("resultCode1.description").isEqualTo("Replaced With Another Offence")
          .jsonPath("resultCode1.code").isEqualTo("2060")
          .jsonPath("offenceDate").isEqualTo("2023-01-01")
          .jsonPath("offenceEndDate").isEqualTo("2023-01-02")

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEventCharges[0].resultCode1.code").isEqualTo("2060")
          .jsonPath("courtEventCharges[0].resultCode1.description").isEqualTo("Replaced With Another Offence")
          .jsonPath("courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
// confirm other CEC is not updated
          .jsonPath("courtEventCharges[1].resultCode1.code").isEqualTo("1067")
          .jsonPath("courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("courtEventCharges[0].offenceDate").isEqualTo("2023-01-01")
          .jsonPath("courtEventCharges[0].offenceEndDate").isEqualTo("2023-01-02")
          .jsonPath("courtOrders[0].id").doesNotExist()

// imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
      }

      @Test
      fun `can update the court event charge and offender charge on an earlier appearance`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${earlierCourtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(
                resultCode1 = "1101",
                offenceDate = LocalDate.parse("2024-01-01"),
                offenceEndDate = LocalDate.parse("2024-01-02"),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

// the result code for an earlier appearance should not be updated on the offender charge. Only the latest CEC result code is updated on the underlying charge
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/offender-charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offence.offenceCode").isEqualTo("RR84005B")
          .jsonPath("resultCode1.description").isEqualTo("Restriction Order")
// dates are updated
          .jsonPath("offenceDate").isEqualTo("2024-01-01")
          .jsonPath("offenceEndDate").isEqualTo("2024-01-02")

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${earlierCourtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEventCharges[0].resultCode1.code").isEqualTo("1101")
          .jsonPath("courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("courtEventCharges[0].offenceDate").isEqualTo("2024-01-01")
          .jsonPath("courtEventCharges[0].offenceEndDate").isEqualTo("2024-01-02")
          .jsonPath("courtOrders[0].id").exists()
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-charge-updated"),
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("offenderChargeId", offenderCharge1.id.toString())
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
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge
    private lateinit var offenderCharge4: OffenderCharge
    private lateinit var order: CourtOrder

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
              offenderCharge4 = offenderCharge(resultCode1 = "1067", offenceCode = "LO72002")
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
                order = courtOrder {
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
          .jsonPath("developerMessage").isEqualTo("Prisoner AB765 not found")
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
                courtEventCharges = mutableListOf(
                  offenderCharge1.id,
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

        assertThat(courtAppearanceResponse.deletedOffenderChargesIds.size).isEqualTo(2)
// no longer referenced by any court appearance in this case
        assertThat(courtAppearanceResponse.deletedOffenderChargesIds[0].offenderChargeId).isEqualTo(offenderCharge3.id)
      }

      @Test
      fun `can add new court event charges for existing offender charges`() {
// request object includes 3 offender charges that are already associated with the court case and 1 to be associated

        val courtAppearanceResponse = webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventCharges = mutableListOf(
                  offenderCharge1.id,
                  offenderCharge2.id,
                  offenderCharge3.id,
                  offenderCharge4.id,
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
          .jsonPath("courtEvents[0].courtEventCharges.size()").isEqualTo(4)
          .jsonPath("courtEvents[0].courtOrders[0].courtDate").isEqualTo("2023-01-05")

        assertThat(courtAppearanceResponse.deletedOffenderChargesIds.size).isEqualTo(0)
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
                courtEventCharges = mutableListOf(
                  offenderCharge1.id,
                  offenderCharge2.id,
                  offenderCharge3.id,
                  offenderCharge4.id,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-appearance-updated"),
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", courtEvent.id.toString())
          },
          isNull(),
        )
// this is an update on an appearance with an existing order, it should update the order date if different
        verify(telemetryClient).trackEvent(
          eq("court-order-updated"),
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
// assertThat(it).containsEntry("orderId", order.id.toString())
            assertThat(it).containsEntry("orderDate", "2023-01-05")
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
  @DisplayName("DELETE /prisoners/{offenderNo}/sentencing/court-cases/{id}/court-appearances/{id}")
  inner class DeleteCourtAppearance {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private var latestBookingId: Long = 0
    private lateinit var offenderCharge1: OffenderCharge

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
              courtEvent = courtEvent(eventDateTime = LocalDateTime.of(2023, 1, 1, 10, 30)) {
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
              courtEvent2 = courtEvent(eventDateTime = LocalDateTime.of(2023, 2, 1, 10, 30)) {
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
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      internal fun `will log event when court appearance does not exist`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/1234")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-appearance-delete-not-found"),
          check {
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("eventId", "1234")
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class DeleteCourtAppearanceSuccess {

      @Test
      fun `can delete a court appearance`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEvents.size()").isEqualTo(1)
          .jsonPath("courtEvents[0].id").isEqualTo(courtEvent2.id)

// imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
      }

      @Test
      fun `will track telemetry for the delete`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-appearance-deleted"),
          check {
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("eventId", courtEvent.id.toString())
            assertThat(it).containsEntry("caseId", courtEvent.courtCase?.id.toString())
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
  @DisplayName("DELETE /prisoners/{offenderNo}/sentencing/court-cases/{id}")
  inner class DeleteCourtCase {
    private val offenderNo: String = "A1234AB"
    private lateinit var courtCase: CourtCase
    private lateinit var courtEvent: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private var latestBookingId: Long = 0
    private lateinit var offenderCharge1: OffenderCharge

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
              courtEvent = courtEvent(eventDateTime = LocalDateTime.of(2023, 1, 1, 10, 30)) {
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
              courtEvent2 = courtEvent(eventDateTime = LocalDateTime.of(2023, 2, 1, 10, 30)) {
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
      }
      latestBookingId = prisonerAtMoorland.latestBooking().bookingId
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      internal fun `will log event when court case does not exist`() {
        webTestClient.delete().uri("/prisoners/$offenderNo/sentencing/court-cases/333")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-case-delete-not-found"),
          check {
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("caseId", "333")
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class DeleteCourtCaseSuccess {

      @Test
      fun `can delete a court appearance`() {
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound

// imprisonment status stored procedure is called
        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_RESULT.name),
        )
      }

      @Test
      fun `will track telemetry for the delete`() {
        webTestClient.delete()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("court-case-deleted"),
          check {
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("caseId", courtEvent.courtCase?.id.toString())
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
                offenderCharge1 = offenderCharge(
                  offenceCode = "RT88074",
                  plea = "G",
                  propertyValue = BigDecimal.valueOf(8.3),
                  totalPropertyValue = BigDecimal.valueOf(11),
                )
                val offenderCharge2 =
                  offenderCharge()
                courtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                    propertyValue = BigDecimal.valueOf(3.2),
                    totalPropertyValue = BigDecimal.valueOf(10),
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
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/1155")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court appearance 1155 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/court-appearances/${courtAppearance.id}")
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
          .jsonPath("courtEventCharges[0].offencesCount").doesNotExist()
          .jsonPath("courtEventCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEventCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEventCharges[0].plea.description").isEqualTo("Not Guilty")
          .jsonPath("courtEventCharges[0].propertyValue").isEqualTo(3.2)
          .jsonPath("courtEventCharges[0].totalPropertyValue").isEqualTo(10)
          .jsonPath("courtEventCharges[0].cjitCode1").isEqualTo("cj1")
          .jsonPath("courtEventCharges[0].cjitCode2").isEqualTo("cj2")
          .jsonPath("courtEventCharges[0].cjitCode3").isEqualTo("cj3")
          .jsonPath("courtEventCharges[0].resultCode1.description").isEqualTo("Imprisonment")
          .jsonPath("courtEventCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("courtEventCharges[0].mostSeriousFlag").isEqualTo(false)
          .jsonPath("courtEventCharges[0].offenderCharge.id").isEqualTo(offenderCharge1.id)
          .jsonPath("courtEventCharges[0].offenderCharge.offenceDate").isEqualTo(aDateString)
          .jsonPath("courtEventCharges[0].offenderCharge.offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEventCharges[0].offenderCharge.offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("courtEventCharges[0].offenderCharge.offencesCount").doesNotExist()
          .jsonPath("courtEventCharges[0].offenderCharge.plea.description").isEqualTo("Guilty")
          .jsonPath("courtEventCharges[0].offenderCharge.propertyValue").isEqualTo(8.3)
          .jsonPath("courtEventCharges[0].offenderCharge.totalPropertyValue").isEqualTo(11)
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode1").isEqualTo("cj6")
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode2").isEqualTo("cj7")
          .jsonPath("courtEventCharges[0].offenderCharge.cjitCode3").isEqualTo("cj8")
          .jsonPath("courtEventCharges[0].offenderCharge.resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("courtEventCharges[0].offenderCharge.resultCode1.dispositionCode").isEqualTo("F")
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
          .jsonPath("developerMessage").isEqualTo("Prisoner XXXX not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court charge`() {
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
          .jsonPath("offencesCount").doesNotExist() // what is this?
          .jsonPath("plea.description").isEqualTo("Guilty")
          .jsonPath("propertyValue").doesNotExist()
          .jsonPath("totalPropertyValue").doesNotExist()
          .jsonPath("cjitCode1").isEqualTo("cj6")
          .jsonPath("cjitCode2").isEqualTo("cj7")
          .jsonPath("cjitCode3").isEqualTo("cj8")
          .jsonPath("resultCode1.description").isEqualTo("Borstal Training")
          .jsonPath("resultCode1.dispositionCode").isEqualTo("F")
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

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-appearances/{eventId}/charges/{id}")
  @Nested
  inner class GetCourtEventCharge {
    private lateinit var courtCase: CourtCase
    private lateinit var sourceCourtCase: CourtCase
    private lateinit var firstCourtAppearance: CourtEvent
    private lateinit var secondCourtAppearance: CourtEvent
    private lateinit var thirdCourtAppearance: CourtEvent
    private lateinit var sourceCourtAppearance: CourtEvent
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge
    private lateinit var linkedCaseTransaction1: LinkCaseTxn
    private lateinit var linkedCaseTransaction2: LinkCaseTxn

    @BeforeEach
    internal fun createPrisonerAndCourtCase() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisonerAtMoorland =
          offender(nomsId = "A1234AB") {
            booking(agencyLocationId = "MDI") {
              courtCase = courtCase(reportingStaff = staff) {
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G")
                offenderCharge2 = offenderCharge(offenceCode = "RI64007")
                offenderCharge3 = offenderCharge(offenceCode = "RI64009")
                firstCourtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    whenModified = LocalDateTime.now().minusDays(2),
                    offenderCharge = offenderCharge2,
                  )
                }
                secondCourtAppearance = courtEvent(eventDateTime = LocalDateTime.now().plusDays(1)) {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    whenModified = LocalDateTime.now(),
                    resultCode1 = "4508",
                    offenderCharge = offenderCharge2,
                  )
                }
                thirdCourtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtEventCharge(
                    whenModified = LocalDateTime.now().minusDays(1),
                    offenderCharge = offenderCharge2,
                  )
                }
              }

              sourceCourtCase = courtCase(reportingStaff = staff, caseSequence = 2, combinedCase = courtCase) {
                sourceCourtAppearance = courtEvent {
                  courtEventCharge(
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                }
              }
            }
          }
        linkedCaseTransaction1 = linkedCaseTransaction(
          sourceCase = sourceCourtCase,
          targetCase = courtCase,
          courtEvent = secondCourtAppearance,
          offenderCharge = offenderCharge1,
          whenCreated = LocalDateTime.parse("2024-02-11T10:00"),
        )
        linkedCaseTransaction2 = linkedCaseTransaction(
          sourceCase = sourceCourtCase,
          targetCase = courtCase,
          courtEvent = secondCourtAppearance,
          offenderCharge = offenderCharge2,
          whenCreated = LocalDateTime.parse("2024-02-11T10:00"),
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if no court event charge is found - offender charge and appearance do exist seperately`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge3.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Court event charge with offenderChargeId ${offenderCharge3.id} for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      fun `will return 404 if charge not found`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/111")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender Charge 111 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the court event charge`() {
        val response: CourtEventChargeResponse = webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${firstCourtAppearance.id}/charges/${offenderCharge2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.offenderCharge.id).isEqualTo(offenderCharge2.id)
        assertThat(response.resultCode1?.code).isEqualTo("1002")
        assertThat(response.resultCode1?.description).isEqualTo("Imprisonment")
        assertThat(response.linkedCaseDetails).isNull()
      }

      @Test
      fun `will return the linked charge data for court event charge`() {
        val chargeResponse1: CourtEventChargeResponse = webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${secondCourtAppearance.id}/charges/${offenderCharge1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        with(chargeResponse1) {
          assertThat(offenderCharge.id).isEqualTo(offenderCharge1.id)
          assertThat(linkedCaseDetails).isNotNull
          with(linkedCaseDetails) {
            assertThat(this!!.eventId).isEqualTo(secondCourtAppearance.id)
            assertThat(caseId).isEqualTo(sourceCourtCase.id)
            assertThat(dateLinked).isEqualTo(LocalDate.parse("2024-02-11"))
          }
        }
        val chargeResponse2: CourtEventChargeResponse = webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-appearances/${secondCourtAppearance.id}/charges/${offenderCharge2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        with(chargeResponse2) {
          assertThat(offenderCharge.id).isEqualTo(offenderCharge2.id)
          assertThat(linkedCaseDetails).isNotNull
          with(linkedCaseDetails) {
            assertThat(this!!.eventId).isEqualTo(secondCourtAppearance.id)
            assertThat(caseId).isEqualTo(sourceCourtCase.id)
            assertThat(dateLinked).isEqualTo(LocalDate.parse("2024-02-11"))
          }
        }
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      linkCaseTxnRepository.deleteAll()
      repository.delete(sourceCourtAppearance)
      repository.delete(firstCourtAppearance)
      repository.delete(secondCourtAppearance)
      repository.delete(thirdCourtAppearance)
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
                courtEventCharges = mutableListOf(),
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
                courtEventCharges = mutableListOf(offenderCharge1.id),
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
                courtEventCharges = mutableListOf(offenderCharge1.id),
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
// court order creation deletion uses the result code from the court event charge
                courtEventCharge(
                  resultCode1 = offenderCharge1.resultCode1?.code,
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  resultCode1 = offenderCharge2.resultCode1?.code,
                  offenderCharge = offenderCharge2,
                )
              }
              courtEvent2 = courtEvent {
                courtEventCharge(
                  resultCode1 = offenderCharge1.resultCode1?.code,
                  offenderCharge = offenderCharge1,
                )
                courtEventCharge(
                  resultCode1 = offenderCharge2.resultCode1?.code,
                  offenderCharge = offenderCharge2,
                )
                courtEventCharge(
                  offenderCharge = offenderCharge3,
                  resultCode1 = offenderCharge3.resultCode1?.code,
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
                courtEventCharges = mutableListOf(
                  offenderCharge1.id,
                  offenderCharge3.id,
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
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "ABDRCT")
            assertThat(it).containsEntry("courtEventId", courtEvent1.id.toString())
          },
          isNull(),
        )
        verify(telemetryClient, never()).trackEvent(eq("court-order-updated"), any(), isNull())
      }

      @Test
      fun `removing any Final and Active results will delete a Court Order for the court appearance`() {
        webTestClient.get().uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courtEvents[1].courtOrders[0]").exists()

// there is 1 charge with Final disposition code and Active Charge status - update to inactive
        webTestClient.put()
          .uri("/prisoners/$offenderNo/sentencing/court-cases/${courtCase.id}/court-appearances/${courtEvent2.id}/charges/${offenderCharge3.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createOffenderChargeRequest(
                offenceDate = LocalDate.of(2022, 11, 5),
                offenceEndDate = LocalDate.of(2022, 11, 5),
                offenceCode = "RI64003",
// result code with Final disposition code and Inactive Charge status
                resultCode1 = "3502",
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
          check {
            assertThat(it).containsEntry("courtCaseId", courtCase.id.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", offenderNo)
            assertThat(it).containsEntry("court", "MDI")
            assertThat(it).containsEntry("courtEventId", courtEvent2.id.toString())
            assertThat(it).containsEntry("courtOrderId", courtEvent2.courtOrders[0].id.toString())
          },
          isNull(),
        )
        verify(telemetryClient, never()).trackEvent(eq("court-order-updated"), any(), isNull())
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisonerAtMoorland)
        repository.deleteOffenderChargeByBooking(latestBookingId)
      }
    }
  }

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/court-cases/{caseId}/sentences")
  inner class CreateSentence {
    private lateinit var courtCase: CourtCase
    private lateinit var courtAppearance: CourtEvent
    private lateinit var courtAppearanceNoCourtOrder: CourtEvent
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
                offenderCharge1 = offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = "1004")
                offenderCharge2 = offenderCharge(offenceDate = LocalDate.parse(aLaterDateString))
                courtAppearance = courtEvent {
// overrides from the parent offender charge fields
                  courtEventCharge(
                    resultCode1 = offenderCharge1.resultCode1?.code,
                    offenderCharge = offenderCharge1,
                    plea = "NG",
                  )
                  courtOrder(courtDate = LocalDate.of(2023, 1, 10)) {
                    sentencePurpose(purposeCode = "REPAIR")
                    sentencePurpose(purposeCode = "PUNISH")
                  }
                }
                courtAppearanceNoCourtOrder = courtEvent {
                  courtEventCharge(
                    resultCode1 = offenderCharge2.resultCode1?.code,
                    offenderCharge = offenderCharge2,
                    plea = "NG",
                  )
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
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/12345678/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 12345678 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      internal fun `400 when category not valid`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(category = "TREE", eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence category TREE not found")
      }

      @Test
      internal fun `400 when calc type not valid`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(calcType = "TREE", eventId = courtAppearance.id),
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
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(calcType = "ADIMP_ORA", eventId = courtAppearance.id),
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
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(offenderChargeIds = mutableListOf(123), eventId = courtAppearance.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offender Charge 123 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      internal fun `404 when consecutive sequence doesn't exist`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                offenderChargeIds = mutableListOf(123),
                consecSentenceSeq = 234,
                eventId = courtAppearance.id,
              ),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Consecutive sentence with sequence 234 and booking ${courtCase.offenderBooking.bookingId} not found")
      }
    }

    @Test
    fun `400 if associated appearance Id does not relate to a court order`() {
      webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentence(
              offenderChargeIds = mutableListOf(offenderCharge2.id),
              eventId = courtAppearanceNoCourtOrder.id,
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest.expectBody()
        .jsonPath("developerMessage")
        .isEqualTo("Court order not found for booking ${prisonerAtMoorland.latestBooking().bookingId} and court event ${courtAppearanceNoCourtOrder.id}")
    }

    @Nested
    inner class CreateSentenceSuccess {
      @Test
      fun `can create a sentence with data`() {
        val sentenceSeq =
          webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentence(
                  offenderChargeIds = mutableListOf(offenderCharge1.id, offenderCharge2.id),
                  eventId = courtAppearance.id,
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

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/$sentenceSeq")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentenceSeq)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType.code").isEqualTo("ADIMP")
          .jsonPath("sentenceLevel").isEqualTo("IND")
          .jsonPath("category.code").isEqualTo("2020")
          .jsonPath("startDate").isEqualTo(aDateString)
          .jsonPath("endDate").isEqualTo(aLaterDateString)
          .jsonPath("fineAmount").isEqualTo("8.5")
          .jsonPath("courtOrder.id").isEqualTo(courtCase.courtEvents[0].courtOrders[0].id)
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("sentenceTerms.size()").isEqualTo(0)
          .jsonPath("offenderCharges.size()").isEqualTo(2)
          .jsonPath("offenderCharges[0].id").isEqualTo(offenderCharge1.id)
          .jsonPath("offenderCharges[0].offenceDate").isEqualTo(aDateString)
          .jsonPath("offenderCharges[0].offenceEndDate").isEqualTo(aLaterDateString)
          .jsonPath("offenderCharges[0].offence.description")
          .isEqualTo("Driver of horsedrawn vehicle failing to stop on signal of traffic constable (other than traffic survey)")
          .jsonPath("offenderCharges[0].offencesCount").doesNotExist()
          .jsonPath("offenderCharges[0].plea.description").isEqualTo("Guilty")
          .jsonPath("offenderCharges[0].propertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].totalPropertyValue").doesNotExist()
          .jsonPath("offenderCharges[0].cjitCode1").isEqualTo("cj6")
          .jsonPath("offenderCharges[0].cjitCode2").isEqualTo("cj7")
          .jsonPath("offenderCharges[0].cjitCode3").isEqualTo("cj8")
          .jsonPath("offenderCharges[0].resultCode1.description").isEqualTo("Restriction Order")
          .jsonPath("offenderCharges[0].resultCode1.dispositionCode").isEqualTo("F")
          .jsonPath("offenderCharges[0].mostSeriousFlag").isEqualTo(true)
          .jsonPath("offenderCharges[0].chargeStatus.description").isEqualTo("Active")
          .jsonPath("offenderCharges[1].offenceDate").isEqualTo(aLaterDateString)
      }

      @Test
      fun `will track telemetry for the create`() {
        val sentenceSeq =
          webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentence(eventId = courtAppearance.id),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
            .returnResult().responseBody!!.sentenceSeq

        verify(telemetryClient).trackEvent(
          eq("sentence-created"),
          check {
            assertThat(it).containsEntry("sentenceSeq", sentenceSeq.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", prisonerAtMoorland.nomsId)
          },
          isNull(),
        )
      }

      @Test
      fun `court order date not updated when sentence exists`() {
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                offenderChargeIds = mutableListOf(offenderCharge1.id, offenderCharge2.id),
                eventId = courtAppearance.id,
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
          .returnResult().responseBody!!.sentenceSeq

        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}/court-appearances/${courtAppearance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createCourtAppearanceRequest(
// a different date to the court order
                eventDateTime = LocalDateTime.of(2023, 6, 6, 9, 0),
                courtEventCharges = mutableListOf(
                  offenderCharge1.id,
                  offenderCharge2.id,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

// don't update if sentence exists
        verify(telemetryClient, never()).trackEvent(eq("court-order-updated"), any(), isNull())
      }

      @Test
      fun `can create consecutive sentences`() {
// create first sentence
        val sentenceSeq1 =
          webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentence(
                  offenderChargeIds = mutableListOf(offenderCharge1.id, offenderCharge2.id),
                  eventId = courtAppearance.id,
                ),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
            .returnResult().responseBody!!.sentenceSeq

// create an aggregate sentence in between (although this will have a line seq, unlike when generated in nomis so not totally accurate)
        webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                offenderChargeIds = mutableListOf(),
                sentenceLevel = "AGG",
                calcType = "AGG_IND_ORA",
                category = "1991",
                eventId = courtAppearance.id,
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

// create second sentence consecutive to first
        val sentenceSeq2 =
          webTestClient.post().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentence(
                  consecSentenceSeq = sentenceSeq1,
                  eventId = courtAppearance.id,
                ),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateSentenceResponse::class.java)
            .returnResult().responseBody!!.sentenceSeq

        val sentence1 = webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/$sentenceSeq1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectBody(SentenceResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/$sentenceSeq2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("sentenceSeq").isEqualTo(sentenceSeq2)
          .jsonPath("consecSequence").isEqualTo(sentence1.sentenceSeq)
          .jsonPath("lineSequence").isNotEmpty()
      }
    }

    @AfterEach
    internal fun deleteSentence() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sequence}")
  inner class UpdateSentence {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var sentenceTwo: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
    private lateinit var courtOrder2: CourtOrder
    private lateinit var courtEvent: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private lateinit var newCourtCase: CourtCase
    private lateinit var offenderCharge: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge
    private lateinit var offenderCharge4: OffenderCharge
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
                offenderCharge2 =
                  offenderCharge(offenceCode = "RR84700", offenceDate = LocalDate.parse(aLaterDateString))
                offenderCharge4 =
                  offenderCharge(offenceCode = "LG72004", offenceDate = LocalDate.parse(aLaterDateString))
                courtEvent = courtEvent {
                  courtEventCharge(offenderCharge = offenderCharge)
                  courtEventCharge(offenderCharge = offenderCharge2)
                  courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                }
                sentence = sentence(statusUpdateStaff = staff, courtOrder = courtOrder) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term(days = 35, sentenceTermType = "IMP")
                  term(days = 15, sentenceTermType = "LIC")
                }
              }
              newCourtCase = courtCase(reportingStaff = staff, caseSequence = 2) {
                offenderCharge3 = offenderCharge(offenceCode = "RT88074", resultCode1 = "1002")
                courtEvent2 = courtEvent(eventDateTime = LocalDateTime.of(2023, 2, 1, 9, 0)) {
                  courtEventCharge(offenderCharge = offenderCharge3)
                  courtOrder2 = courtOrder(courtDate = LocalDate.of(2023, 2, 1))
                }
                sentenceTwo = sentence(courtOrder = courtOrder2, statusUpdateStaff = staff) {
                  offenderSentenceCharge(offenderCharge = offenderCharge3)
                  term(days = 20, sentenceTermType = "IMP")
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
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when court case does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/33/sentences/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent2.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 33 for A1234AB not found")
      }

      @Test
      internal fun `404 when sentence does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/5555")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence for booking $latestBookingId and sentence sequence 5555 not found")
      }

      @Test
      internal fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(calcType = "ADIMP_ORA", eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence calculation with category 2020 and calculation type ADIMP_ORA not found")
      }

      @Test
      internal fun `404 when consecutive sequence doesn't exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                consecSentenceSeq = 234,
                eventId = courtEvent.id,
              ),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Consecutive sentence with sequence 234 and booking ${courtCase.offenderBooking.bookingId} not found")
      }
    }

    @Nested
    inner class UpdateSentenceSuccess {

      @Test
      fun `can update a sentence`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${newCourtCase.id}/sentences/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(
                calcType = "FTR_ORA",
                category = "1991",
                eventId = courtEvent2.id,
                startDate = LocalDate.parse(aLaterDateString),
                endDate = null,
                sentenceLevel = "AGG",
                fine = BigDecimal.valueOf(9.7),
                offenderChargeIds = mutableListOf(offenderCharge2.id),
                consecSentenceSeq = sentence.id.sequence,
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
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("caseId").isEqualTo(newCourtCase.id)
          .jsonPath("sentenceSeq").isEqualTo(sentenceTwo.id.sequence)
          .jsonPath("status").isEqualTo("A")
          .jsonPath("calculationType.code").isEqualTo("FTR_ORA")
          .jsonPath("sentenceLevel").isEqualTo("AGG")
          .jsonPath("consecSequence").isEqualTo(sentence.id.sequence)
          .jsonPath("category.code").isEqualTo("1991")
          .jsonPath("startDate").isEqualTo(aLaterDateString)
          .jsonPath("endDate").doesNotExist()
          .jsonPath("fineAmount").isEqualTo("9.7")
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("offenderCharges.size()").isEqualTo(1)
          // update doesn't affect terms, there was 1 existing
          .jsonPath("sentenceTerms.size()").isEqualTo(1)

        // now update to remove the consecutive sentence status

        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${newCourtCase.id}/sentences/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent2.id),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentenceTwo.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(latestBookingId)
          .jsonPath("caseId").isEqualTo(newCourtCase.id)
          .jsonPath("sentenceSeq").isEqualTo(sentenceTwo.id.sequence)
          .jsonPath("consecSequence").doesNotExist()
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentence(eventId = courtEvent.id),
            ),
          )
          .exchange()
          .expectStatus().isOk
      }
    }

    @AfterEach
    internal fun deleteSentence() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @Nested
  @DisplayName("DELETE /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sequence}")
  inner class DeleteSentence {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
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
                courtEvent {
                  courtOrder = courtOrder { }
                }
                sentence = sentence(courtOrder = courtOrder, statusUpdateStaff = staff) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term {}
                  term(days = 35)
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
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `204 even when sentence does not exist`() {
      webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/9999 ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.delete().uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-delete-not-found"),
        check {
          assertThat(it).containsEntry("sentenceSequence", "9999")
          assertThat(it).containsEntry("bookingId", latestBookingId.toString())
        },
        isNull(),
      )
    }

    @Test
    internal fun `204 when sentence does exist`() {
      webTestClient.get()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will track telemetry for the delete`() {
      webTestClient.delete()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-deleted"),
        check {
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

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms")
  inner class CreateSentenceTerm {
    private lateinit var courtCase: CourtCase
    private lateinit var courtAppearance: CourtEvent
    private lateinit var courtAppearanceNoCourtOrder: CourtEvent
    private lateinit var offenderCharge1: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var courtOrder: CourtOrder
    private lateinit var sentence: OffenderSentence
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
                offenderCharge1 = offenderCharge(offenceCode = "RT88074")
                offenderCharge2 =
                  offenderCharge(offenceCode = "RR84700", offenceDate = LocalDate.parse(aLaterDateString))
                courtAppearance = courtEvent {
                  courtEventCharge(offenderCharge = offenderCharge1)
                  courtEventCharge(offenderCharge = offenderCharge2)
                  courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                }
                sentence = sentence(statusUpdateStaff = staff, courtOrder = courtOrder) {
                  offenderSentenceCharge(offenderCharge = offenderCharge1)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term(days = 35, sentenceTermType = "IMP")
                  term(days = 15, sentenceTermType = "LIC")
                }
                courtAppearanceNoCourtOrder = courtEvent {
                  courtEventCharge(
                    resultCode1 = offenderCharge2.resultCode1?.code,
                    offenderCharge = offenderCharge2,
                    plea = "NG",
                  )
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
        webTestClient.post()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when case does not exist`() {
        webTestClient.post()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/12345678/sentences/${sentence.id.sequence}/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 12345678 for ${prisonerAtMoorland.nomsId} not found")
      }

      @Test
      internal fun `400 when sentence term type not valid`() {
        webTestClient.post()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(sentenceTermType = "TREE"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence term type TREE not found")
      }
    }

    @Nested
    inner class CreateSentenceTermSuccess {
      @Test
      fun `can create a sentence term with data`() {
        val response =
          webTestClient.post()
            .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentenceTerm(),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateSentenceTermResponse::class.java)
            .returnResult().responseBody!!

        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
        )

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${response.bookingId}/sentence-sequence/${response.sentenceSeq}/term-sequence/${response.termSeq}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("startDate").isEqualTo(sentence.courtOrder!!.courtDate)
          .jsonPath("endDate").doesNotExist()
          .jsonPath("years").isEqualTo(7)
          .jsonPath("months").isEqualTo(2)
          .jsonPath("weeks").isEqualTo(3)
          .jsonPath("days").isEqualTo(4)
          .jsonPath("hours").isEqualTo(5)
          .jsonPath("sentenceTermType.description").isEqualTo("Imprisonment")
          .jsonPath("prisonId").isEqualTo("MDI")
          .jsonPath("lifeSentenceFlag").isEqualTo(true)
      }

      @Test
      fun `will track telemetry for the create`() {
        val response =
          webTestClient.post()
            .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createSentenceTerm(),
              ),
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateSentenceTermResponse::class.java)
            .returnResult().responseBody!!

        verify(telemetryClient).trackEvent(
          eq("sentence-term-created"),
          check {
            assertThat(it).containsEntry("sentenceSeq", response.sentenceSeq.toString())
            assertThat(it).containsEntry("termSeq", response.termSeq.toString())
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
  @DisplayName("PUT /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms/{termSequence}")
  inner class UpdateSentenceTerm {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var term: OffenderSentenceTerm
    private lateinit var sentenceTwo: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
    private lateinit var courtOrder2: CourtOrder
    private lateinit var courtEvent: CourtEvent
    private lateinit var courtEvent2: CourtEvent
    private lateinit var newCourtCase: CourtCase
    private lateinit var offenderCharge: OffenderCharge
    private lateinit var offenderCharge2: OffenderCharge
    private lateinit var offenderCharge3: OffenderCharge
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
                offenderCharge2 =
                  offenderCharge(offenceCode = "RR84700", offenceDate = LocalDate.parse(aLaterDateString))
                courtEvent = courtEvent {
                  courtEventCharge(offenderCharge = offenderCharge)
                  courtEventCharge(offenderCharge = offenderCharge2)
                  courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                }
                sentence = sentence(statusUpdateStaff = staff, courtOrder = courtOrder) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term = term(days = 35, sentenceTermType = "IMP")
                  term(days = 15, sentenceTermType = "LIC")
                }
              }
              newCourtCase = courtCase(reportingStaff = staff, caseSequence = 2) {
                offenderCharge3 = offenderCharge(offenceCode = "RT88074", resultCode1 = "1002")
                courtEvent2 = courtEvent(eventDateTime = LocalDateTime.of(2023, 2, 1, 9, 0)) {
                  courtEventCharge(offenderCharge = offenderCharge3)
                  courtOrder2 = courtOrder(courtDate = LocalDate.of(2023, 2, 1))
                }
                sentenceTwo = sentence(courtOrder = courtOrder2, statusUpdateStaff = staff) {
                  offenderSentenceCharge(offenderCharge = offenderCharge3)
                  term(days = 20, sentenceTermType = "IMP")
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
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when court case does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/33/sentences/${sentenceTwo.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 33 for A1234AB not found")
      }

      @Test
      internal fun `404 when sentence term does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/5555/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence term for offender ${prisonerAtMoorland.nomsId}, booking $latestBookingId, term sequence ${term.id.termSequence} and sentence sequence 5555 not found")
      }
    }

    @Nested
    inner class UpdateSentenceTermSuccess {

      @Test
      fun `can update a sentence`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${newCourtCase.id}/sentences/${sentenceTwo.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(days = 20),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).imprisonmentStatusUpdate(
          bookingId = eq(latestBookingId),
          changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
        )

        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentenceTwo.id.sequence}/term-sequence/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("days").isEqualTo(20)
          // not updated as taken from the court order
          .jsonPath("startDate").isEqualTo(courtOrder2.courtDate.toString())
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createSentenceTerm(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("sentence-term-updated"),
          check {
            assertThat(it).containsEntry("sentenceSequence", sentence.id.sequence.toString())
            assertThat(it).containsEntry("termSequence", term.id.termSequence.toString())
            assertThat(it).containsEntry("bookingId", latestBookingId.toString())
            assertThat(it).containsEntry("offenderNo", prisonerAtMoorland.nomsId)
            assertThat(it).containsEntry("caseId", courtCase.id.toString())
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
  @DisplayName("DELETE /prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms/{termSequence}")
  inner class DeleteSentenceTerm {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var term: OffenderSentenceTerm
    private lateinit var courtCase: CourtCase
    private lateinit var courtOrder: CourtOrder
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
                courtEvent {
                  courtOrder = courtOrder { }
                }
                sentence = sentence(courtOrder = courtOrder, statusUpdateStaff = staff) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term {}
                  term = term(days = 35)
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
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `204 even when sentence term does not exist`() {
      webTestClient.get()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/9999/term-sequence/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.delete()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/9999/sentence-terms/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-term-delete-not-found"),
        check {
          assertThat(it).containsEntry("sentenceSequence", "9999")
          assertThat(it).containsEntry("termSequence", term.id.termSequence.toString())
          assertThat(it).containsEntry("bookingId", latestBookingId.toString())
        },
        isNull(),
      )
    }

    @Test
    internal fun `204 when sentence term does exist`() {
      webTestClient.get()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will track telemetry for the delete`() {
      webTestClient.delete()
        .uri("/prisoners/${prisonerAtMoorland.nomsId}/court-cases/${courtCase.id}/sentences/${sentence.id.sequence}/sentence-terms/${term.id.termSequence}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-term-deleted"),
        check {
          assertThat(it).containsEntry("sentenceSequence", sentence.id.sequence.toString())
          assertThat(it).containsEntry("termSequence", term.id.termSequence.toString())
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

  @DisplayName("GET /prisoners/booking-id/{bookingId}/sentences/recall")
  @Nested
  inner class GetActiveRecallSentences {
    private var latestBookingId: Long = 0
    private lateinit var recallSentence: OffenderSentence
    private lateinit var nonRecallSentence: OffenderSentence
    private lateinit var inactiveRecallSentence: OffenderSentence
    private lateinit var courtCase: CourtCase
    private lateinit var appearance: CourtEvent
    private lateinit var courtOrder: CourtOrder
    private lateinit var offenderCharge: OffenderCharge

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
                appearance = courtEvent {
                  courtOrder = courtOrder()
                }
                // Active recall sentence
                recallSentence = sentence(
                  statusUpdateStaff = staff,
                  courtOrder = courtOrder,
                  status = "A",
                  category = "2020",
                  calculationType = "FTR_ORA",
                ) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
                }
                // Non-recall sentence
                nonRecallSentence = sentence(
                  statusUpdateStaff = staff,
                  courtOrder = courtOrder,
                  status = "A",
                  category = "2003",
                  calculationType = "ADIMP_ORA",
                ) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
                }
                // Inactive recall sentence
                inactiveRecallSentence = sentence(
                  statusUpdateStaff = staff,
                  courtOrder = courtOrder,
                  status = "I",
                  category = "2020",
                  calculationType = "FTR_ORA",
                ) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  term {}
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
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentences/recall")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentences/recall")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if booking not found`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/9999999/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Offender booking 9999999 not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return only active recall sentences`() {
        webTestClient.get()
          .uri("/prisoners/booking-id/$latestBookingId/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("$[0].bookingId").isEqualTo(latestBookingId)
          .jsonPath("$[0].sentenceSeq").isEqualTo(recallSentence.id.sequence)
          .jsonPath("$[0].status").isEqualTo("A")
          .jsonPath("$[0].calculationType.code").isEqualTo("FTR_ORA")
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/sentence-terms/booking-id/{bookingId}/sentence-sequence/{sentenceSequence}/term-sequence/{termSequence}")
  @Nested
  inner class GetOffenderSentenceTerm {
    private var latestBookingId: Long = 0
    private lateinit var sentence: OffenderSentence
    private lateinit var term: OffenderSentenceTerm
    private lateinit var courtCase: CourtCase
    private lateinit var appearance: CourtEvent
    private lateinit var courtOrder: CourtOrder
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
                appearance = courtEvent {
                  courtOrder = courtOrder()
                }
                sentence = sentence(statusUpdateStaff = staff, courtOrder = courtOrder) {
                  offenderSentenceCharge(offenderCharge = offenderCharge)
                  offenderSentenceCharge(offenderCharge = offenderCharge2)
                  term = term {}
                  term(days = 35)
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
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if sentence term not found`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/4444")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Sentence term for offender ${prisonerAtMoorland.nomsId}, booking $latestBookingId, term sequence 4444 and sentence sequence ${sentence.id.sequence} not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the offender sentence term`() {
        webTestClient.get()
          .uri("/prisoners/${prisonerAtMoorland.nomsId}/sentence-terms/booking-id/${prisonerAtMoorland.latestBooking().bookingId}/sentence-sequence/${sentence.id.sequence}/term-sequence/${term.id.termSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("startDate").isEqualTo(courtOrder.courtDate.toString())
          .jsonPath("endDate").doesNotExist()
          .jsonPath("years").isEqualTo(2)
          .jsonPath("months").isEqualTo(3)
          .jsonPath("weeks").isEqualTo(4)
          .jsonPath("days").isEqualTo(5)
          .jsonPath("hours").isEqualTo(6)
          .jsonPath("sentenceTermType.description").isEqualTo("Section 86 of 2000 Act")
          .jsonPath("lifeSentenceFlag").isEqualTo(true)
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
// repository.delete(sentence)
      repository.delete(prisonerAtMoorland)
      repository.delete(staff)
    }
  }

  private fun createCourtCase(
    courtId: String = "COURT1",
    legalCaseType: String = "A",
    startDate: LocalDate = LocalDate.of(2023, 1, 1),
    status: String = "A",
  ) = CreateCourtCaseRequest(
    courtId = courtId,
    legalCaseType = legalCaseType,
    startDate = startDate,
    status = status,
  )

  private fun createOffenderChargeRequest(
    offenceCode: String = "RT88074",
    offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 2),
    resultCode1: String? = "1067",
  ) = OffenderChargeRequest(
    offenceCode = offenceCode,
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
    courtEventCharges: MutableList<Long> = mutableListOf(),
  ) = CourtAppearanceRequest(
    eventDateTime = eventDateTime,
    courtId = courtId,
    courtEventType = courtEventType,
    nextEventDateTime = nextEventDateTime,
    outcomeReasonCode = outcomeReasonCode,
    nextCourtId = nextCourtId,
    courtEventCharges = courtEventCharges,
  )

  private fun createSentence(
    category: String = "2020",
    calcType: String = "ADIMP",
    startDate: LocalDate = LocalDate.parse(aDateString),
    endDate: LocalDate? = LocalDate.parse(aLaterDateString),
    status: String = "A",
    sentenceLevel: String = "IND",
    fine: BigDecimal? = BigDecimal.valueOf(8.5),
    offenderChargeIds: MutableList<Long> = mutableListOf(),
    consecSentenceSeq: Long? = null,
    eventId: Long,
  ) = CreateSentenceRequest(
    startDate = startDate,
    status = status,
    endDate = endDate,
    sentenceCalcType = calcType,
    sentenceCategory = category,
    sentenceLevel = sentenceLevel,
    fine = fine,
    offenderChargeIds = offenderChargeIds,
    consecutiveToSentenceSeq = consecSentenceSeq,
    eventId = eventId,
  )

  private fun createSentenceTerm(
    sentenceTermType: String = "IMP",
    lifeSentenceFlag: Boolean = true,
    years: Int = 7,
    months: Int = 2,
    weeks: Int = 3,
    days: Int = 4,
    hours: Int = 5,
  ) = SentenceTermRequest(
    sentenceTermType = sentenceTermType,
    lifeSentenceFlag = lifeSentenceFlag,
    years = years,
    months = months,
    weeks = weeks,
    days = days,
    hours = hours,
  )

  @Nested
  @DisplayName("POST /prisoners/{offenderNo}/sentences/recall")
  inner class RecallSentences {
    @Nested
    inner class Security {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: ConvertToRecallRequest

      @BeforeEach
      fun createPrisonerAndSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(
                    category = "2020",
                    calculationType = "ADIMP",
                    statusUpdateStaff = staff,
                    courtOrder = courtOrder,
                    status = "I",
                  ) {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
              }
            }
        }
        request = ConvertToRecallRequest(
          returnToCustody = null,
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "FTR_ORA",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          recallRevocationDate = LocalDate.now(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: ConvertToRecallRequest

      @BeforeEach
      fun createPrisonerAndSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(
                    category = "2020",
                    calculationType = "ADIMP",
                    statusUpdateStaff = staff,
                    courtOrder = courtOrder,
                    status = "I",
                  ) {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
              }
            }
        }

        request = ConvertToRecallRequest(
          returnToCustody = null,
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "FTR_ORA",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          recallRevocationDate = LocalDate.now(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.post()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceCategory = "1880") })))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `404 when sentence does not exist`() {
        webTestClient.post()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              request.copy(
                sentences = request.sentences.map {
                  it.copy(
                    sentenceId = SentenceId(
                      offenderBookingId = booking.bookingId,
                      sentenceSequence = 99,
                    ),
                  )
                },
              ),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage")
          .isEqualTo("Not Found: Sentence for booking ${booking.bookingId} and sentence sequence 99 not found")
      }
    }

    @Nested
    inner class RecallSentencesSuccess {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence1: OffenderSentence
      private lateinit var sentence2: OffenderSentence
      private lateinit var sentence3: OffenderSentence
      private lateinit var courtCase1: CourtCase
      private lateinit var courtCase2: CourtCase
      private lateinit var offenderCharge1: OffenderCharge
      private lateinit var offenderCharge2: OffenderCharge
      private lateinit var offenderCharge3: OffenderCharge
      private lateinit var request: ConvertToRecallRequest
      private lateinit var remandAdjustment: OffenderSentenceAdjustment
      private lateinit var taggedBailAdjustment: OffenderSentenceAdjustment
      private lateinit var unusedRemandAdjustment: OffenderSentenceAdjustment

      @Nested
      open inner class FirstFixedTermRecall {
        @BeforeEach
        fun createPrisonerAndSentence() {
          nomisDataBuilder.build {
            staff = staff {
              account(username = "T.SMITH")
            }
            prisoner =
              offender(nomsId = "A1234AB") {
                booking = booking(agencyLocationId = "MDI") {
                  courtCase1 = courtCase(reportingStaff = staff, agencyId = "ABDRCT", caseSequence = 1) {
                    lateinit var courtOrder: CourtOrder
                    offenderCharge1 = offenderCharge(offenceCode = "RT88074")
                    offenderCharge2 = offenderCharge(offenceCode = "RT88074")
                    courtEvent(eventDateTime = LocalDateTime.parse("2023-01-01T10:00"), agencyId = "ABDRCT") {
                      courtEventCharge(offenderCharge = offenderCharge1)
                    }
                    courtEvent(eventDateTime = LocalDateTime.parse("2023-01-02T10:00"), agencyId = "LEEDYC") {
                      courtEventCharge(offenderCharge = offenderCharge1)
                      courtEventCharge(offenderCharge = offenderCharge2)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2023-01-01"))
                    }
                    sentence1 = sentence(
                      category = "2020",
                      calculationType = "ADIMP",
                      statusUpdateStaff = staff,
                      courtOrder = courtOrder,
                      status = "I",
                    ) {
                      offenderSentenceCharge(offenderCharge = offenderCharge1)
                      term(days = 35, sentenceTermType = "IMP")
                      remandAdjustment = adjustment(adjustmentTypeCode = "RX", active = false)
                    }
                    sentence2 = sentence(
                      category = "2020",
                      calculationType = "ADIMP",
                      statusUpdateStaff = staff,
                      courtOrder = courtOrder,
                      status = "I",
                    ) {
                      offenderSentenceCharge(offenderCharge = offenderCharge2)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                  }
                  courtCase2 = courtCase(reportingStaff = staff, agencyId = "LEICYC", caseSequence = 2) {
                    lateinit var courtOrder: CourtOrder
                    offenderCharge3 = offenderCharge(offenceCode = "RT88074")
                    courtEvent(eventDateTime = LocalDateTime.parse("2023-05-02T10:00"), agencyId = "LEICYC") {
                      courtEventCharge(offenderCharge = offenderCharge3)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2023-05-02"))
                    }
                    sentence3 = sentence(category = "2020", calculationType = "ADIMP", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                      taggedBailAdjustment = adjustment(adjustmentTypeCode = "S240A", active = false)
                      unusedRemandAdjustment = adjustment(adjustmentTypeCode = "UR", active = false)
                      offenderSentenceCharge(offenderCharge = offenderCharge3)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                  }
                }
              }
          }
          request = ConvertToRecallRequest(
            returnToCustody = ReturnToCustodyRequest(
              returnToCustodyDate = LocalDate.parse("2023-01-01"),
              enteredByStaffUsername = "T.SMITH",
              recallLength = 28,
            ),
            sentences = listOf(
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence1.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "FTR_ORA",
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence2.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "FTR_ORA",
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence3.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "FTR_ORA",
              ),

            ),
            recallRevocationDate = LocalDate.now(),
          )
        }

        @Test
        fun `can recall a sentence`() {
          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isFalse
            assertThat(calculationType.description).isEqualTo("Sentencing Code Standard Determinate Sentence")
            assertThat(status).isEqualTo("I")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isFalse
            assertThat(calculationType.description).isEqualTo("Sentencing Code Standard Determinate Sentence")
            assertThat(status).isEqualTo("I")
          }
          with(offenderSentenceRepository.findById(sentence3.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isFalse
            assertThat(calculationType.description).isEqualTo("Sentencing Code Standard Determinate Sentence")
            assertThat(status).isEqualTo("I")
          }

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence3.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
        }

        @Test
        fun `will add a return to custody data`() {
          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isNotPresent()

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          val returnToCustodyData = offenderFixedTermRecallRepository.findById(booking.bookingId).orElseThrow()
          assertThat(returnToCustodyData.returnToCustodyDate).isEqualTo(LocalDate.parse("2023-01-01"))
          assertThat(returnToCustodyData.recallLength).isEqualTo(28L)
          assertThat(returnToCustodyData.staff.id).isEqualTo(staff.id)
        }

        @Test
        fun `will not add a return to custody data with not supplied`() {
          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isNotPresent()

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request.copy(returnToCustody = null)))
            .exchange()
            .expectStatus().isOk

          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isNotPresent()
        }

        @Test
        fun `will update the imprisonment status`() {
          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(spRepository).imprisonmentStatusUpdate(
            bookingId = eq(booking.bookingId),
            changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
          )
        }

        @Test
        fun `will update remand adjustment to recall remand and activate`() {
          with(offenderSentenceAdjustmentRepository.findByIdOrNull(remandAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("RX")
            assertThat(active).isFalse
          }

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceAdjustmentRepository.findByIdOrNull(remandAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("RSR")
            assertThat(active).isTrue
          }
        }

        @Test
        fun `will update tagged bail adjustment to recall tagged bail`() {
          with(offenderSentenceAdjustmentRepository.findByIdOrNull(taggedBailAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("S240A")
            assertThat(active).isFalse
          }

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceAdjustmentRepository.findByIdOrNull(taggedBailAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("RST")
            assertThat(active).isTrue
          }
        }

        @Test
        fun `will activate all sentence adjustments`() {
          with(offenderSentenceAdjustmentRepository.findByIdOrNull(unusedRemandAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("UR")
            assertThat(active).isFalse
          }

          val response: ConvertToRecallResponse = webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk.expectBodyResponse()

          with(offenderSentenceAdjustmentRepository.findByIdOrNull(unusedRemandAdjustment.id)!!) {
            assertThat(sentenceAdjustment.id).isEqualTo("UR")
            assertThat(active).isTrue
          }

          assertThat(response.sentenceAdjustmentsActivated).hasSize(2)
          assertThat(response.sentenceAdjustmentsActivated[0].sentenceId.offenderBookingId).isEqualTo(sentence1.id.offenderBooking.bookingId)
          assertThat(response.sentenceAdjustmentsActivated[0].sentenceId.sentenceSequence).isEqualTo(sentence1.id.sequence)
          assertThat(response.sentenceAdjustmentsActivated[0].adjustmentIds).hasSize(1)
          assertThat(response.sentenceAdjustmentsActivated[0].adjustmentIds[0]).isEqualTo(remandAdjustment.id)
          assertThat(response.sentenceAdjustmentsActivated[1].sentenceId.offenderBookingId).isEqualTo(sentence3.id.offenderBooking.bookingId)
          assertThat(response.sentenceAdjustmentsActivated[1].sentenceId.sentenceSequence).isEqualTo(sentence3.id.sequence)
          assertThat(response.sentenceAdjustmentsActivated[1].adjustmentIds).hasSize(2)
          assertThat(response.sentenceAdjustmentsActivated[1].adjustmentIds[0]).isEqualTo(taggedBailAdjustment.id)
          assertThat(response.sentenceAdjustmentsActivated[1].adjustmentIds[1]).isEqualTo(unusedRemandAdjustment.id)
        }

        @Test
        fun `will track telemetry for the recall`() {
          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(telemetryClient).trackEvent(
            eq("sentences-recalled"),
            check {
              assertThat(it["bookingId"]).isEqualTo(booking.bookingId.toString())
              assertThat(it["sentenceSequences"]).isEqualTo("${sentence1.id.sequence}, ${sentence2.id.sequence}, ${sentence3.id.sequence}")
              assertThat(it["offenderNo"]).isEqualTo(prisoner.nomsId)
            },
            isNull(),
          )
        }

        @Test
        fun `will create a court event when recallRevocationDate is supplied`() {
          assertThat(courtCase1.courtEvents).hasSize(2)
          assertThat(courtCase2.courtEvents).hasSize(1)

          // Set a specific recall revocation date
          val recallRevocationDate = LocalDate.parse("2023-02-15")
          val requestWithSpecificDate = request.copy(recallRevocationDate = recallRevocationDate)

          val recallResponse: ConvertToRecallResponse = webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestWithSpecificDate))
            .exchange()
            .expectStatus().isOk.expectBodyResponse()

          nomisDataBuilder.runInTransaction {
            // Get the court case from the sentence
            with(courtCaseRepository.findById(courtCase1.id).orElseThrow()) {
              assertThat(this.courtEvents).hasSize(3)
              with(this.courtEvents.first { it.eventDate == recallRevocationDate }) {
                assertThat(eventDate).isEqualTo(recallRevocationDate)
                assertThat(startTime.toLocalDate()).isEqualTo(recallRevocationDate)
                assertThat(startTime.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
                assertThat(courtEventType.code).isEqualTo("BREACH")
                assertThat(outcomeReasonCode?.code).isEqualTo("1501")
                assertThat(court.id).isEqualTo("LEEDYC")
                assertThat(courtEventCharges).extracting<Long> { it.id.offenderCharge.id }.containsExactly(offenderCharge1.id, offenderCharge2.id)
                assertThat(courtEventCharges).extracting<String> { it.resultCode1!!.code }.containsExactly("1501", "1501")
                assertThat(recallResponse.courtEventIds).contains(this.id)
              }
            }
            with(courtCaseRepository.findById(courtCase2.id).orElseThrow()) {
              assertThat(this.courtEvents).hasSize(2)
              with(this.courtEvents.first { it.eventDate == recallRevocationDate }) {
                assertThat(eventDate).isEqualTo(recallRevocationDate)
                assertThat(startTime.toLocalDate()).isEqualTo(recallRevocationDate)
                assertThat(startTime.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
                assertThat(courtEventType.code).isEqualTo("BREACH")
                assertThat(outcomeReasonCode?.code).isEqualTo("1501")
                assertThat(court.id).isEqualTo("LEICYC")
                assertThat(courtEventCharges).extracting<Long> { it.id.offenderCharge.id }.containsExactly(offenderCharge3.id)
                assertThat(courtEventCharges).extracting<String> { it.resultCode1!!.code }.containsExactly("1501")
                assertThat(recallResponse.courtEventIds).contains(this.id)
              }
            }
          }
        }
      }

      @Nested
      inner class SecondFixedTermRecall {
        private lateinit var prisoner: Offender
        private lateinit var booking: OffenderBooking
        private lateinit var staff: Staff
        private lateinit var otherStaff: Staff
        private lateinit var sentence1: OffenderSentence
        private lateinit var sentence2: OffenderSentence
        private lateinit var request: ConvertToRecallRequest

        @BeforeEach
        fun createPrisonerAndSentence() {
          nomisDataBuilder.build {
            staff = staff {
              account(username = "T.SMITH")
            }
            otherStaff = staff {
              account(username = "T.JONES")
            }
            prisoner =
              offender(nomsId = "A1234AB") {
                booking = booking(agencyLocationId = "MDI") {
                  fixedTermRecall(
                    returnToCustodyDate = LocalDate.parse("2022-01-05"),
                    staff = otherStaff,
                    recallLength = 14,
                  )
                  lateinit var courtOrder: CourtOrder
                  courtCase(reportingStaff = staff) {
                    val offenderCharge = offenderCharge(offenceCode = "RT88074")
                    courtEvent {
                      courtEventCharge(offenderCharge = offenderCharge)
                      courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                    }
                    sentence1 = sentence(
                      category = "2020",
                      calculationType = "14FTR_ORA",
                      statusUpdateStaff = staff,
                      courtOrder = courtOrder,
                      status = "I",
                    ) {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                    sentence2 = sentence(
                      category = "2020",
                      calculationType = "14FTR_ORA",
                      statusUpdateStaff = staff,
                      courtOrder = courtOrder,
                      status = "I",
                    ) {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                  }
                }
              }
          }
          request = ConvertToRecallRequest(
            returnToCustody = ReturnToCustodyRequest(
              returnToCustodyDate = LocalDate.parse("2024-01-01"),
              enteredByStaffUsername = "T.SMITH",
              recallLength = 28,
            ),
            sentences = listOf(
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence1.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "FTR_ORA",
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence2.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "FTR_ORA",
              ),

            ),
            recallRevocationDate = LocalDate.now(),
          )
        }

        @Test
        fun `can recall a sentence`() {
          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 14 Day Fixed Term Recall")
            assertThat(status).isEqualTo("I")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 14 Day Fixed Term Recall")
            assertThat(status).isEqualTo("I")
          }

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
        }

        @Test
        fun `will update a return to custody data`() {
          with(offenderFixedTermRecallRepository.findById(booking.bookingId).orElseThrow()) {
            assertThat(returnToCustodyDate).isEqualTo(LocalDate.parse("2022-01-05"))
            assertThat(recallLength).isEqualTo(14L)
            assertThat(staff.id).isEqualTo(otherStaff.id)
          }

          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderFixedTermRecallRepository.findById(booking.bookingId).orElseThrow()) {
            assertThat(returnToCustodyDate).isEqualTo(LocalDate.parse("2024-01-01"))
            assertThat(recallLength).isEqualTo(28L)
            assertThat(staff.id).isEqualTo(staff.id)
          }
        }

        @Test
        fun `will update the imprisonment status`() {
          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(spRepository).imprisonmentStatusUpdate(
            bookingId = eq(booking.bookingId),
            changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
          )
        }

        @Test
        fun `will track telemetry for the recall`() {
          webTestClient.post()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(telemetryClient).trackEvent(
            eq("sentences-recalled"),
            check {
              assertThat(it["bookingId"]).isEqualTo(booking.bookingId.toString())
              assertThat(it["sentenceSequences"]).isEqualTo("${sentence1.id.sequence}, ${sentence2.id.sequence}")
              assertThat(it["offenderNo"]).isEqualTo(prisoner.nomsId)
            },
            isNull(),
          )
        }
      }
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentences/recall")
  inner class UpdateRecallSentences {
    @Nested
    inner class Security {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: UpdateRecallRequest

      @BeforeEach
      fun createPrisonerAndRecallSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
                fixedTermRecall(
                  staff = staff,
                )
              }
            }
        }
        request = UpdateRecallRequest(
          returnToCustody = null,
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "LR",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          recallRevocationDate = LocalDate.now(),
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: UpdateRecallRequest

      @BeforeEach
      fun createPrisonerAndSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
              }
            }
        }

        request = UpdateRecallRequest(
          returnToCustody = null,
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "LR",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          recallRevocationDate = LocalDate.now(),
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceCategory = "1880") })))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `404 when sentence does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = 99)) })))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Not Found: Sentence for booking ${booking.bookingId} and sentence sequence 99 not found")
      }
    }

    @Nested
    inner class UpdateRecallSentencesSuccess {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence1: OffenderSentence
      private lateinit var sentence2: OffenderSentence
      private lateinit var request: UpdateRecallRequest
      private lateinit var breachCourtEvent: CourtEvent

      @Nested
      inner class FirstFixedTermRecall {
        @BeforeEach
        fun createPrisonerAndSentence() {
          nomisDataBuilder.build {
            staff = staff {
              account(username = "T.SMITH")
            }
            prisoner =
              offender(nomsId = "A1234AB") {
                booking = booking(agencyLocationId = "MDI") {
                  lateinit var courtOrder: CourtOrder
                  courtCase(reportingStaff = staff) {
                    val offenderCharge = offenderCharge(offenceCode = "RT88074")
                    courtEvent {
                      courtEventCharge(offenderCharge = offenderCharge)
                      courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                    }
                    sentence1 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                    sentence2 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                    breachCourtEvent = courtEvent(courtEventType = RECALL_BREACH_HEARING, outcomeReasonCode = RECALL_TO_PRISON, eventDateTime = LocalDate.parse("2023-01-01").atStartOfDay()) {
                      courtEventCharge(offenderCharge = offenderCharge, resultCode1 = RECALL_TO_PRISON)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2023-01-01"))
                    }
                  }
                  fixedTermRecall(staff = staff)
                }
              }
          }
          request = UpdateRecallRequest(
            returnToCustody = null,
            beachCourtEventIds = listOf(breachCourtEvent.id),
            recallRevocationDate = LocalDate.parse("2023-06-06"),
            sentences = listOf(
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence1.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "LR",
                active = true,
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence2.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "LR",
                // no sure this will ever happen, but lets test it just in case
                active = false,
              ),

            ),
          )
        }

        @Test
        fun `can update recall a sentence`() {
          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("Licence Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("Licence Recall")
            assertThat(status).isEqualTo("I")
          }
        }

        @Test
        fun `will update the breach court appearance date`() {
          with(courtEventRepository.findByIdOrNull(breachCourtEvent.id)!!) {
            assertThat(eventDate).isEqualTo(LocalDate.parse("2023-01-01"))
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(courtEventRepository.findByIdOrNull(breachCourtEvent.id)!!) {
            assertThat(eventDate).isEqualTo(LocalDate.parse("2023-06-06"))
          }
        }

        @Test
        fun `will remove custody data when absent`() {
          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isPresent

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isNotPresent
        }

        @Test
        fun `will add a return to custody data if supplied`() {
          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isPresent

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                request.copy(
                  returnToCustody = ReturnToCustodyRequest(
                    returnToCustodyDate = LocalDate.parse("2023-01-01"),
                    enteredByStaffUsername = "T.SMITH",
                    recallLength = 28,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isOk

          val returnToCustodyData = offenderFixedTermRecallRepository.findById(booking.bookingId).orElseThrow()
          assertThat(returnToCustodyData.returnToCustodyDate).isEqualTo(LocalDate.parse("2023-01-01"))
          assertThat(returnToCustodyData.recallLength).isEqualTo(28L)
          assertThat(returnToCustodyData.staff.id).isEqualTo(staff.id)
        }

        @Test
        fun `will update the imprisonment status`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(spRepository).imprisonmentStatusUpdate(
            bookingId = eq(booking.bookingId),
            changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
          )
        }

        @Test
        fun `will track telemetry for the recall`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(telemetryClient).trackEvent(
            eq("recall-sentences-updated"),
            check {
              assertThat(it["bookingId"]).isEqualTo(booking.bookingId.toString())
              assertThat(it["sentenceSequences"]).isEqualTo("${sentence1.id.sequence}, ${sentence2.id.sequence}")
              assertThat(it["offenderNo"]).isEqualTo(prisoner.nomsId)
            },
            isNull(),
          )
        }
      }
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentences/recall/restore-original")
  inner class DeleteRecallSentences {
    @Nested
    inner class Security {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: DeleteRecallRequest

      @BeforeEach
      fun createPrisonerAndRecallSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
                fixedTermRecall(
                  staff = staff,
                )
              }
            }
        }
        request = DeleteRecallRequest(
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "ADIMP",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: DeleteRecallRequest

      @BeforeEach
      fun createPrisonerAndSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
              }
            }
        }

        request = DeleteRecallRequest(
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "ADIMP",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceCategory = "1880") })))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `404 when sentence does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = 99)) })))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Not Found: Sentence for booking ${booking.bookingId} and sentence sequence 99 not found")
      }
    }

    @Nested
    inner class UpdateRecallSentencesSuccess {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence1: OffenderSentence
      private lateinit var sentence2: OffenderSentence
      private lateinit var request: DeleteRecallRequest
      private lateinit var breachCourtEvent: CourtEvent
      private lateinit var remandAdjustment: OffenderSentenceAdjustment
      private lateinit var taggedBailAdjustment: OffenderSentenceAdjustment

      @Nested
      inner class FirstFixedTermRecall {
        @BeforeEach
        fun createPrisonerAndSentence() {
          nomisDataBuilder.build {
            staff = staff {
              account(username = "T.SMITH")
            }
            prisoner =
              offender(nomsId = "A1234AB") {
                booking = booking(agencyLocationId = "MDI") {
                  lateinit var courtOrder: CourtOrder
                  courtCase(reportingStaff = staff) {
                    val offenderCharge = offenderCharge(offenceCode = "RT88074")
                    courtEvent {
                      courtEventCharge(offenderCharge = offenderCharge)
                      courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                    }
                    sentence1 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                      remandAdjustment = adjustment(adjustmentTypeCode = "RSR")
                    }
                    sentence2 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                      taggedBailAdjustment = adjustment(adjustmentTypeCode = "RST")
                    }
                    breachCourtEvent = courtEvent(courtEventType = RECALL_BREACH_HEARING, outcomeReasonCode = RECALL_TO_PRISON, eventDateTime = LocalDate.parse("2023-01-01").atStartOfDay()) {
                      courtEventCharge(offenderCharge = offenderCharge, resultCode1 = RECALL_TO_PRISON)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2023-01-01"))
                    }
                  }
                  fixedTermRecall(staff = staff)
                }
              }
          }
          request = DeleteRecallRequest(
            beachCourtEventIds = listOf(breachCourtEvent.id),
            sentences = listOf(
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence1.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "ADIMP",
                active = false,
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence2.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "ADIMP",
                active = false,
              ),

            ),
          )
        }

        @Test
        fun `can update recall a sentence`() {
          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isFalse
            assertThat(calculationType.description).isEqualTo("Sentencing Code Standard Determinate Sentence")
            assertThat(status).isEqualTo("I")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isFalse
            assertThat(calculationType.description).isEqualTo("Sentencing Code Standard Determinate Sentence")
            assertThat(status).isEqualTo("I")
          }
        }

        @Test
        fun `will remove custody data`() {
          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isPresent

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          assertThat(offenderFixedTermRecallRepository.findById(booking.bookingId)).isNotPresent
        }

        @Test
        fun `will remove breach court appearance data`() {
          assertThat(courtEventRepository.findById(breachCourtEvent.id)).isPresent

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          assertThat(courtEventRepository.findById(breachCourtEvent.id)).isNotPresent
        }

        @Test
        fun `will update remand adjustment to recall remand`() {
          with(offenderSentenceAdjustmentRepository.findByIdOrNull(remandAdjustment.id)!!) {
            assertThat(this.sentenceAdjustment.id).isEqualTo("RSR")
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceAdjustmentRepository.findByIdOrNull(remandAdjustment.id)!!) {
            assertThat(this.sentenceAdjustment.id).isEqualTo("RX")
          }
        }

        @Test
        fun `will update tagged bail adjustment to recall tagged bail`() {
          with(offenderSentenceAdjustmentRepository.findByIdOrNull(taggedBailAdjustment.id)!!) {
            assertThat(this.sentenceAdjustment.id).isEqualTo("RST")
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceAdjustmentRepository.findByIdOrNull(taggedBailAdjustment.id)!!) {
            assertThat(this.sentenceAdjustment.id).isEqualTo("S240A")
          }
        }

        @Test
        fun `will update the imprisonment status`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(spRepository).imprisonmentStatusUpdate(
            bookingId = eq(booking.bookingId),
            changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
          )
        }

        @Test
        fun `will track telemetry for the recall`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-original")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(telemetryClient).trackEvent(
            eq("recall-sentences-replaced"),
            check {
              assertThat(it["bookingId"]).isEqualTo(booking.bookingId.toString())
              assertThat(it["sentenceSequences"]).isEqualTo("${sentence1.id.sequence}, ${sentence2.id.sequence}")
              assertThat(it["offenderNo"]).isEqualTo(prisoner.nomsId)
            },
            isNull(),
          )
        }
      }
    }
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/sentences/recall/restore-previous")
  inner class RevertRecallSentences {
    @Nested
    inner class Security {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: RevertRecallRequest

      @BeforeEach
      fun createPrisonerAndRecallSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
                fixedTermRecall(
                  staff = staff,
                )
              }
            }
        }
        request = RevertRecallRequest(
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "ADIMP",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          returnToCustody = null,
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence: OffenderSentence
      private lateinit var request: RevertRecallRequest

      @BeforeEach
      fun createPrisonerAndSentence() {
        nomisDataBuilder.build {
          staff = staff {
            account(username = "T.SMITH")
          }
          prisoner =
            offender(nomsId = "A1234AB") {
              booking = booking(agencyLocationId = "MDI") {
                lateinit var courtOrder: CourtOrder
                courtCase(reportingStaff = staff) {
                  val offenderCharge = offenderCharge(offenceCode = "RT88074")
                  courtEvent {
                    courtEventCharge(offenderCharge = offenderCharge)
                    courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                  }
                  sentence = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "I") {
                    offenderSentenceCharge(offenderCharge = offenderCharge)
                    term(days = 35, sentenceTermType = "IMP")
                  }
                }
              }
            }
        }

        request = RevertRecallRequest(
          sentences = listOf(
            RecallRelatedSentenceDetails(
              sentenceCategory = "2020",
              sentenceCalcType = "ADIMP",
              sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence.id.sequence),
            ),
          ),
          beachCourtEventIds = emptyList(),
        )
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisoner)
        repository.delete(staff)
      }

      @Test
      fun `400 when category code and calc type are individually valid but not a valid combination`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceCategory = "1880") })))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `404 when sentence does not exist`() {
        webTestClient.put()
          .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request.copy(sentences = request.sentences.map { it.copy(sentenceId = SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = 99)) })))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Not Found: Sentence for booking ${booking.bookingId} and sentence sequence 99 not found")
      }
    }

    @Nested
    inner class RevertRecallSentencesSuccess {
      private lateinit var prisoner: Offender
      private lateinit var booking: OffenderBooking
      private lateinit var staff: Staff
      private lateinit var sentence1: OffenderSentence
      private lateinit var sentence2: OffenderSentence
      private lateinit var request: RevertRecallRequest
      private lateinit var breachCourtEvent: CourtEvent
      private lateinit var previousBreachCourtEvent: CourtEvent

      @Nested
      inner class SecondFixedTermRecall {
        @BeforeEach
        fun createPrisonerAndSentence() {
          nomisDataBuilder.build {
            staff = staff {
              account(username = "T.SMITH")
            }
            prisoner =
              offender(nomsId = "A1234AB") {
                booking = booking(agencyLocationId = "MDI") {
                  lateinit var courtOrder: CourtOrder
                  courtCase(reportingStaff = staff) {
                    val offenderCharge = offenderCharge(offenceCode = "RT88074")
                    courtEvent {
                      courtEventCharge(offenderCharge = offenderCharge)
                      courtOrder = courtOrder(courtDate = LocalDate.of(2023, 1, 1))
                    }
                    sentence1 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                    sentence2 = sentence(category = "2020", calculationType = "FTR_ORA", statusUpdateStaff = staff, courtOrder = courtOrder, status = "A") {
                      offenderSentenceCharge(offenderCharge = offenderCharge)
                      term(days = 35, sentenceTermType = "IMP")
                    }
                    breachCourtEvent = courtEvent(courtEventType = RECALL_BREACH_HEARING, outcomeReasonCode = RECALL_TO_PRISON, eventDateTime = LocalDate.parse("2023-01-01").atStartOfDay()) {
                      courtEventCharge(offenderCharge = offenderCharge, resultCode1 = RECALL_TO_PRISON)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2023-01-01"))
                    }

                    previousBreachCourtEvent = courtEvent(courtEventType = RECALL_BREACH_HEARING, outcomeReasonCode = RECALL_TO_PRISON, eventDateTime = LocalDate.parse("2022-01-01").atStartOfDay()) {
                      courtEventCharge(offenderCharge = offenderCharge, resultCode1 = RECALL_TO_PRISON)
                      courtOrder = courtOrder(courtDate = LocalDate.parse("2022-01-01"))
                    }
                  }
                  fixedTermRecall(staff = staff, returnToCustodyDate = LocalDate.parse("2023-01-03"))
                }
              }
          }
          request = RevertRecallRequest(
            beachCourtEventIds = listOf(breachCourtEvent.id),
            returnToCustody = ReturnToCustodyRequest(
              returnToCustodyDate = LocalDate.parse("2022-01-03"),
              enteredByStaffUsername = "T.SMITH",
              recallLength = 28,
            ),
            sentences = listOf(
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence1.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "14FTR_ORA",
                active = true,
              ),
              RecallRelatedSentenceDetails(
                SentenceId(offenderBookingId = booking.bookingId, sentenceSequence = sentence2.id.sequence),
                sentenceCategory = "2020",
                sentenceCalcType = "14FTR_ORA",
                active = true,
              ),
            ),
          )
        }

        @Test
        fun `can update recall to previous sentence`() {
          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 28 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          with(offenderSentenceRepository.findById(sentence1.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 14 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
          with(offenderSentenceRepository.findById(sentence2.id).orElseThrow()) {
            assertThat(calculationType.isRecallSentence()).isTrue
            assertThat(calculationType.description).isEqualTo("ORA 14 Day Fixed Term Recall")
            assertThat(status).isEqualTo("A")
          }
        }

        @Test
        fun `will update custody data`() {
          assertThat(offenderFixedTermRecallRepository.findByIdOrNull(booking.bookingId)!!.returnToCustodyDate).isEqualTo(LocalDate.parse("2023-01-03"))

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          assertThat(offenderFixedTermRecallRepository.findByIdOrNull(booking.bookingId)!!.returnToCustodyDate).isEqualTo(LocalDate.parse("2022-01-03"))
        }

        @Test
        fun `will remove breach court appearance data`() {
          assertThat(courtEventRepository.findById(breachCourtEvent.id)).isPresent
          assertThat(courtEventRepository.findById(previousBreachCourtEvent.id)).isPresent

          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          assertThat(courtEventRepository.findById(previousBreachCourtEvent.id)).isPresent
          assertThat(courtEventRepository.findById(breachCourtEvent.id)).isNotPresent
        }

        @Test
        fun `will update the imprisonment status`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(spRepository).imprisonmentStatusUpdate(
            bookingId = eq(booking.bookingId),
            changeType = eq(ImprisonmentStatusChangeType.UPDATE_SENTENCE.name),
          )
        }

        @Test
        fun `will track telemetry for the recall`() {
          webTestClient.put()
            .uri("/prisoners/${prisoner.nomsId}/sentences/recall/restore-previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk

          verify(telemetryClient).trackEvent(
            eq("recall-sentences-reverted"),
            check {
              assertThat(it["bookingId"]).isEqualTo(booking.bookingId.toString())
              assertThat(it["sentenceSequences"]).isEqualTo("${sentence1.id.sequence}, ${sentence2.id.sequence}")
              assertThat(it["offenderNo"]).isEqualTo(prisoner.nomsId)
            },
            isNull(),
          )
        }
      }
    }
  }
}
