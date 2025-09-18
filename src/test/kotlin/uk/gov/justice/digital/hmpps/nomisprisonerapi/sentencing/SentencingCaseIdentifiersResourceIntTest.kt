package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class SentencingCaseIdentifiersResourceIntTest : IntegrationTestBase() {
  private val aDateString = "2023-01-01"
  private val aLaterDateString = "2023-01-05"

  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = 0L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
  }

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
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
                statusUpdateStaff = staff,
              ) {
                offenderCaseIdentifier(reference = "caseRef1")
                offenderCaseIdentifier(reference = "caseRef2")
                offenderCaseIdentifier(reference = "caseRef3", type = "notOne")
                offenderCaseIdentifier(reference = "caseRef4", type = "notOne")
                offenderCaseIdentifier(reference = "caseRef5")
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
        webTestClient.post()
          .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseIdentifierRequest(caseIdentifiers = listOf()),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseIdentifierRequest(caseIdentifiers = listOf()),
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post()
          .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.post()
          .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseIdentifierRequest(caseIdentifiers = listOf()),
            ),
          )
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if court case not found`() {
        webTestClient.post().uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/111/case-identifiers")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseIdentifierRequest(caseIdentifiers = listOf()),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court case 111 not found")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can add a new case reference`() {
        webTestClient.post()
          .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseIdentifierRequest(
                caseIdentifiers = listOf(
                  CaseIdentifier("caseRef1", LocalDateTime.now()),
                  CaseIdentifier("caseRef2", LocalDateTime.now().plusHours(1)),
                  CaseIdentifier("caseRef5", LocalDateTime.now().plusHours(2)),
                  CaseIdentifier("newRef6", LocalDateTime.now().plusHours(2)),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseInfoNumbers.size()").isEqualTo(4)
          .jsonPath("caseInfoNumbers[0].reference").isEqualTo("caseRef1")
          .jsonPath("caseInfoNumbers[1].reference").isEqualTo("caseRef2")
          .jsonPath("caseInfoNumbers[2].reference").isEqualTo("caseRef5")
          .jsonPath("caseInfoNumbers[3].reference").isEqualTo("newRef6")
          .jsonPath("primaryCaseInfoNumber").isEqualTo("caseRef1")

        @Test
        fun `can remove case references`() {
          webTestClient.post()
            .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                CaseIdentifierRequest(
                  caseIdentifiers = listOf(
                    CaseIdentifier("caseRef1", LocalDateTime.now()),
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("caseInfoNumbers.size()").isEqualTo(1)
            .jsonPath("caseInfoNumbers[0].reference").isEqualTo("caseRef1")
            .jsonPath("primaryCaseInfoNumber").isEqualTo("caseRef1")
        }

        @Test
        fun `case references can be added and removed`() {
          webTestClient.post()
            .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                CaseIdentifierRequest(
                  caseIdentifiers = listOf(
                    CaseIdentifier("caseRef1", LocalDateTime.now()),
                    CaseIdentifier("newRef1", LocalDateTime.now().plusHours(1)),
                    CaseIdentifier("newRef2", LocalDateTime.now().plusHours(2)),
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("caseInfoNumbers.size()").isEqualTo(3)
            .jsonPath("caseInfoNumbers[0].reference").isEqualTo("caseRef1")
            .jsonPath("caseInfoNumbers[1].reference").isEqualTo("newRef1")
            .jsonPath("caseInfoNumbers[2].reference").isEqualTo("newRef2")
        }

        @Test
        fun `if case info is removed use next case info in the list on the case primary column`() {
          webTestClient.post()
            .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                CaseIdentifierRequest(
                  caseIdentifiers = listOf(
                    CaseIdentifier("newRef1", LocalDateTime.now().plusHours(1)),
                    CaseIdentifier("newRef2", LocalDateTime.now().plusHours(2)),
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("caseInfoNumbers.size()").isEqualTo(2)
            .jsonPath("caseInfoNumbers[1].reference").isEqualTo("newRef1")
            .jsonPath("caseInfoNumbers[2].reference").isEqualTo("newRef2")
            .jsonPath("primaryCaseInfoNumber").isEqualTo("newRef1")
        }

        @Test
        fun `if all case info numbers are removed then clear the case column`() {
          webTestClient.post()
            .uri("/prisoners/$prisonerAtMoorland/sentencing/court-cases/${courtCase.id}/case-identifiers")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                CaseIdentifierRequest(
                  caseIdentifiers = emptyList(),
                ),
              ),
            )
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("/prisoners/${prisonerAtMoorland.nomsId}/sentencing/court-cases/${courtCase.id}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("caseInfoNumbers.size()").isEqualTo(0)
            .jsonPath("primaryCaseInfoNumber").doesNotExist()
        }
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(prisonerAtMoorland)
        repository.delete(staff)
      }
    }
  }
}
