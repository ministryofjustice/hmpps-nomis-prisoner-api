package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentCommittee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAssessmentRepository
import java.time.LocalDate

class CsraResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var offenderAssessmentRepository: OffenderAssessmentRepository

  private lateinit var booking1: OffenderBooking
  private lateinit var booking2: OffenderBooking
  private lateinit var staff: Staff

  @BeforeEach
  fun init() {
    nomisDataBuilder.build {
      staff = staff(firstName = "BILL", lastName = "STAFF") { account(username = "BILLSTAFF") }
      offender(nomsId = "A1111AA") {
        booking1 = booking()
      }
    }
  }

  @AfterEach
  internal fun deleteData() {
    repository.deleteAssessments()
    repository.deleteOffenders()
    // TODO
    // repository.deleteStaff()
  }

  @DisplayName("POST /prisoners/{offenderNo}/csra")
  @Nested
  inner class CreateCsra {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `no booking`() {
        webTestClient.post().uri("/prisoners/Z9999ZZ/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Not Found: Cannot find latest booking for offender Z9999ZZ")
      }

      @Test
      fun `no placement agency`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ ${requiredFields()}, "placementAgencyId": "DUFF" }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Cannot find placement agency DUFF")
      }

      @Test
      fun `no review placement agency`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ ${requiredFields()}, "reviewPlacementAgencyId": "DUFF" }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Cannot find review placement agency DUFF")
      }

      @Test
      fun `invalid assessment type`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ 
                "assessmentDate": "2025-12-14",
                "score": "1200",
                "status": "A",
                "assessmentStaffId": ${staff.id},
                "createdBy": "BILLSTAFF",
                "type": "DUFF"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Cannot deserialize value of type `uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType`")
            assertThat(it).contains("String \"DUFF\": not one of the values accepted for Enum class")
          }
      }

      @Test
      fun `invalid committee`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ 
                "assessmentDate": "2025-12-14",
                "calculatedLevel": "HI",
                "score": "1200",
                "status": "A",
                "assessmentStaffId": ${staff.id},
                "createdBy": "BILLSTAFF",
                "type": "CSR",
                "createdDateTime": "2025-12-04T12:34:56",
                "committeeCode": "DUFF"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Cannot deserialize value of type `uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentCommittee`")
            assertThat(it).contains("String \"DUFF\": not one of the values accepted for Enum class")
          }
      }

      @Test
      fun `no created by user`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ 
                "assessmentDate": "2025-12-14",
                "calculatedLevel": "HI",
                "type": "CSRF",
                "score": "1200",
                "status": "A",
                "assessmentStaffId": ${staff.id},
                "createdDateTime": "2025-12-04T12:34:56"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("missing (therefore NULL) value for creator parameter createdBy")
          }
      }

      @Test
      fun `invalid created by user`() {
        webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{
                "assessmentDate": "2025-12-14",
                "calculatedLevel": "HI",
                "type": "CSRF",
                "score": "1200",
                "status": "A",
                "assessmentStaffId": ${staff.id},
                "createdDateTime": "2025-12-04T12:34:56",
                "createdBy": "DUFF"
               }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Cannot find user DUFF")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a CSRA with full data`() {
        val created = webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraCreateResponse>()
          .returnResult()
          .responseBody!!

        val data = offenderAssessmentRepository.findByIdOrNull(
          OffenderAssessmentId(booking1, created.sequence),
        )

        with(data!!) {
          assertThat(assessmentDate.toString()).isEqualTo("2025-12-14")
          assertThat(assessmentType).isEqualTo(AssessmentType.CSRF)
          assertThat(calculatedLevel).isEqualTo(AssessmentLevel.HI)
          assertThat(score.toString()).isEqualTo("1200")
          assertThat(assessmentStatus).isEqualTo(AssessmentStatusType.A)
          assertThat(assessmentStaff).isEqualTo(staff)
          assertThat(assessmentCommitteeCode).isEqualTo(AssessmentCommittee.GOV)
          assertThat(nextReviewDate).isEqualTo("2026-12-15")
          assertThat(assessmentComment).isEqualTo("comment")
          assertThat(placementAgency?.id).isEqualTo("LEI")
          assertThat(creationDateTime).isEqualTo("2025-12-04T12:34:56")
          assertThat(creationUser).isEqualTo("BILLSTAFF")
          assertThat(reviewLevel).isEqualTo(AssessmentLevel.MED)
          assertThat(approvedLevel).isEqualTo(AssessmentLevel.LOW)
          assertThat(evaluationDate).isEqualTo("2025-12-16")
          assertThat(evaluationResultCode).isEqualTo(EvaluationResultCode.APP)
          assertThat(reviewCommitteeCode).isEqualTo(AssessmentCommittee.SECUR)
          assertThat(reviewCommitteeComment).isEqualTo("reviewCommitteeComment")
          assertThat(reviewPlacementAgency?.id).isEqualTo("MDI")
          assertThat(reviewComment).isEqualTo("reviewComment")
        }
      }

      @Test
      fun `can create a CSRA with minimal data`() {
        val created = webTestClient.post().uri("/prisoners/A1111AA/csra")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraCreateResponse>()
          .returnResult()
          .responseBody!!

        val data = offenderAssessmentRepository.findByIdOrNull(
          OffenderAssessmentId(booking1, created.sequence),
        )

        with(data!!) {
          assertThat(assessmentDate).isEqualTo("2025-12-14")
          assertThat(assessmentType).isEqualTo(AssessmentType.CSRF)
          assertThat(score.toString()).isEqualTo("1200")
          assertThat(assessmentStatus).isEqualTo(AssessmentStatusType.A)
          assertThat(assessmentStaff).isEqualTo(staff)
        }
      }
    }
  }

  fun validFullCreateJsonRequest(): String =
    """
      { ${requiredFields()},
   "committeeCode": "GOV",
   "nextReviewDate": "2026-12-15",
   "comment": "comment",
   "placementAgencyId": "LEI",
   "reviewLevel": "MED",
   "approvedLevel": "LOW",
   "evaluationDate": "2025-12-16",
   "evaluationResultCode": "APP",
   "reviewCommitteeCode": "SECUR",
   "reviewCommitteeComment": "reviewCommitteeComment",
   "reviewPlacementAgencyId": "MDI",
   "reviewComment": "reviewComment"
      }
    """.trimIndent()

  fun validMinimalCreateJsonRequest(): String = "{ ${requiredFields()} }"

  fun requiredFields() =
    """
      "assessmentDate": "2025-12-14",
      "calculatedLevel": "HI",
      "type": "CSRF",
      "score": "1200",
      "status": "A",
      "assessmentStaffId": ${staff.id},
      "createdBy": "BILLSTAFF",
      "createdDateTime": "2025-12-04T12:34:56"
    """.trimIndent()

  @DisplayName("GET /prisoners/booking-id/{bookingId}/csra/{sequence}")
  @Nested
  inner class GetCsra {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/booking-id/999/csra/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/booking-id/999/csra/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/booking-id/999/csra/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can get a CSRA with full data`() {
        nomisDataBuilder.build {
          offender(nomsId = "A2222BB") {
            booking2 = booking {
              assessment(
                username = "BILLSTAFF",
                assessmentDate = LocalDate.parse("2025-12-29"),
                placementAgency = "BXI",
                assessmentType = AssessmentType.CSR1,
              ) {
                assessmentItem(1, 9923) // Source for current offence?    	I	Inmate/Prisoner
                assessmentItem(2, 9928) // Source for previous convictions?	D	Document
                assessmentItem(3, 9973) // Source for damage to property?	  S	Staff
              }
            }
          }
        }

        val data = webTestClient.get().uri("/prisoners/booking-id/${booking2.bookingId}/csra/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraGetDto>()
          .returnResult()
          .responseBody!!

        with(data) {
          assertThat(assessmentDate).isEqualTo("2025-12-29")
          assertThat(type).isEqualTo(AssessmentType.CSR1)
          assertThat(calculatedLevel).isEqualTo(AssessmentLevel.STANDARD)
          assertThat(score.toString()).isEqualTo("1000")
          assertThat(status).isEqualTo(AssessmentStatusType.I)
          assertThat(assessmentStaffId).isEqualTo(staff.id)
          assertThat(committeeCode).isEqualTo(AssessmentCommittee.SECSTATE)
          assertThat(nextReviewDate).isEqualTo("2026-12-15")
          assertThat(comment).isEqualTo("a-comment")
          assertThat(placementAgencyId).isEqualTo("BXI")
          assertThat(createdDateTime).isEqualTo("2025-12-27T12:34:56")
          assertThat(createdBy).isEqualTo("BILLSTAFF")
          assertThat(reviewLevel).isEqualTo(AssessmentLevel.PEND)
          assertThat(evaluationDate).isEqualTo("2025-12-28")
          assertThat(evaluationResultCode).isEqualTo(EvaluationResultCode.REJ)
          assertThat(reviewCommitteeCode).isEqualTo(AssessmentCommittee.MED)
          assertThat(reviewCommitteeComment).isEqualTo("a-reviewCommitteeComment")
          assertThat(reviewPlacementAgencyId).isNull()
          assertThat(reviewComment).isEqualTo("a-reviewComment")
          assertThat(sections).hasSize(2)
          with(sections[0]) {
            assertThat(code).isEqualTo("2a")
            assertThat(description).isEqualTo("Section 2A : Questions")
            assertThat(questions).hasSize(2)
            with(questions[0]) {
              assertThat(code).isEqualTo("SRCCURR")
              assertThat(description).isEqualTo("Source for current offence?")
              assertThat(responses).hasSize(1)
              with(responses[0]) {
                assertThat(code).isEqualTo("I")
                assertThat(answer).isEqualTo("Inmate/Prisoner")
                assertThat(comment).isEqualTo("Item comment for sequence 1, item 1")
              }
            }
            with(questions[1]) {
              assertThat(code).isEqualTo("SRCPREV")
              assertThat(description).isEqualTo("Source for previous convictions?")
              assertThat(responses).hasSize(1)
              with(responses[0]) {
                assertThat(code).isEqualTo("D")
                assertThat(answer).isEqualTo("Document")
                assertThat(comment).isEqualTo("Item comment for sequence 1, item 2")
              }
            }
          }
          with(sections[1]) {
            assertThat(code).isEqualTo("2b")
            assertThat(description).isEqualTo("Section 2B: Indicators")
            assertThat(questions).hasSize(1)
            with(questions[0]) {
              assertThat(code).isEqualTo("SRCDAM")
              assertThat(description).isEqualTo("Source for damage to property")
              assertThat(responses).hasSize(1)
              with(responses[0]) {
                assertThat(code).isEqualTo("S")
                assertThat(answer).isEqualTo("Staff")
                assertThat(comment).isEqualTo("Item comment for sequence 1, item 3")
              }
            }
          }
        }
      }
    }
  }
}
