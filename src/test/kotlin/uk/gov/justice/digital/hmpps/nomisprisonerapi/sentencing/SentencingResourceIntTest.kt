package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate

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

  @DisplayName("GET /prisoners/{offenderNo}/sentencing/court-case/{id}")
  @Nested
  inner class GetCourtCase {
    private lateinit var staff: Staff
    private lateinit var prisonerAtMoorland: Offender
    private lateinit var courtCase: CourtCase
    private lateinit var courtCaseTwo: CourtCase
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
            booking(agencyLocationId = "MDI")
          }
        courtCase = courtCase(
          offender = prisonerAtMoorland,
          reportingStaff = staff,
          beginDate = LocalDate.parse(aDateString),
          statusUpdateDate = LocalDate.parse(aDateString),
          statusUpdateStaff = staff,
        ) {
          val offenderCharge1 = offenderCharge(offenceCode = "M1", plea = "G")
          val offenderCharge2 = offenderCharge()
          courtEvent() {
            courtEventCharge(
              offenderCharge = offenderCharge1,
              plea = "NG", // overrides from the parent offender charge fields
            )
            courtEventCharge(
              offenderCharge = offenderCharge2,
            )
          }
        }
        courtCaseTwo = courtCase(
          offender = prisonerAtMoorland,
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

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCase.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if court case not found`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/11")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 11 not found")
      }

      @Test
      fun `will return 404 if offender not found`() {
        webTestClient.get().uri("/prisoners/XXXX/sentencing/court-case/${courtCase.id}")
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
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("caseSequence").isEqualTo(1)
          .jsonPath("prisonId").isEqualTo("MDI")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("caseType.code").isEqualTo("A")
          .jsonPath("caseType.description").isEqualTo("Adult")
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
          .jsonPath("courtEvents[0].prisonId").isEqualTo("MDI")
          .jsonPath("courtEvents[0].outcomeReasonCode").isEqualTo("1046")
          .jsonPath("courtEvents[0].commentText").isEqualTo("Court event comment")
          .jsonPath("courtEvents[0].orderRequestedFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].holdFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventRequestFlag").isEqualTo(false)
          .jsonPath("courtEvents[0].nextEventDate").isEqualTo(aLaterDateString)
          .jsonPath("courtEvents[0].nextEventStartTime").isEqualTo(aLaterDateTimeString)
          .jsonPath("courtEvents[0].createdDateTime").isNotEmpty
          .jsonPath("courtEvents[0].createdByUsername").isNotEmpty
      }

      @Test
      fun `will return the court case and events with minimal data`() {
        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-case/${courtCaseTwo.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(prisonerAtMoorland.nomsId)
          .jsonPath("caseSequence").isEqualTo(2)
          .jsonPath("prisonId").isEqualTo("MDI")
          .jsonPath("caseStatus.code").isEqualTo("A")
          .jsonPath("caseStatus.description").isEqualTo("Active")
          .jsonPath("caseType.code").isEqualTo("A")
          .jsonPath("caseType.description").isEqualTo("Adult")
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
          .jsonPath("courtEvents[0].prisonId").isEqualTo("MDI")
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
}