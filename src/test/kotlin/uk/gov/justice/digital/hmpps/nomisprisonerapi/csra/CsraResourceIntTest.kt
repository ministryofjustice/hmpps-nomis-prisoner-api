package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class CsraResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  private lateinit var prisoner: Offender
  private lateinit var staff: Staff

  @BeforeEach
  fun init() {
    nomisDataBuilder.build {
      staff = staff(firstName = "BILL", lastName = "STAFF") { account(username = "BILLSTAFF") }
      prisoner = offender(nomsId = "A1111AA") {
        booking {
        }
      }
    }
  }

  @AfterEach
  internal fun deleteData() {
    repository.deleteAssessments()
    repository.deleteOffenders()
    repository.deleteStaff()
  }

  @DisplayName("POST /csra/{offenderNo}")
  @Nested
  inner class CreateCsra {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/csra/A1111AA")
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
        webTestClient.post().uri("/csra/Z9999ZZ")
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
        webTestClient.post().uri("/csra/A1111AA")
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
        webTestClient.post().uri("/csra/A1111AA")
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
      fun `no assessment type`() {
        webTestClient.post().uri("/csra/A1111AA")
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
            assertThat(it).contains("String \"DUFF\": not one of the values accepted for Enum class")
          }
      }

      @Test
      fun `no created by user`() {
        webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ 
          "assessmentDate": "2025-12-14",
          "type": "CSRF",
          "score": "1200",
          "status": "A",
          "assessmentStaffId": ${staff.id}
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: createdBy field is required")
      }

      @Test
      fun `invalid created by user`() {
        webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{
              "assessmentDate": "2025-12-14",
              "type": "CSRF",
              "score": "1200",
              "status": "A",
              "assessmentStaffId": ${staff.id},
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
        val created = webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraCreateResponse>()
          .returnResult()
          .responseBody!!

        val data = webTestClient.get().uri("/csra/booking/${created.bookingId}/sequence/${created.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraDto>()
          .returnResult()
          .responseBody!!

        with(data) {
          assertThat(assessmentDate).isEqualTo("2025-12-14")
          assertThat(type).isEqualTo(AssessmentType.CSRF)
          assertThat(calculatedLevel).isEqualTo("HI")
          assertThat(score.toString()).isEqualTo("1200")
          assertThat(status).isEqualTo(AssessmentStatusType.A)
          assertThat(assessmentStaffId).isEqualTo(staff.id)
          assertThat(committeeCode).isEqualTo("GOV")
          assertThat(nextReviewDate).isEqualTo("2026-12-15")
          assertThat(comment).isEqualTo("comment")
          assertThat(placementAgencyId).isEqualTo("LEI")
          assertThat(createdDateTime).isEqualTo("2025-12-04T12:34:56")
          assertThat(createdBy).isEqualTo("BILLSTAFF")
          assertThat(reviewLevel).isEqualTo("MED")
          assertThat(approvedLevel).isEqualTo("LOW")
          assertThat(evaluationDate).isEqualTo("2025-12-16")
          assertThat(evaluationResultCode).isEqualTo(EvaluationResultCode.APP)
          assertThat(reviewCommitteeCode).isEqualTo("CODE")
          assertThat(reviewCommitteeComment).isEqualTo("reviewCommitteeComment")
          assertThat(reviewPlacementAgencyId).isEqualTo("MDI")
          assertThat(reviewComment).isEqualTo("reviewComment")
        }
      }

      @Test
      fun `can create a CSRA with minimal data`() {
        val created = webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraCreateResponse>()
          .returnResult()
          .responseBody!!

        val data = webTestClient.get().uri("/csra/booking/${created.bookingId}/sequence/${created.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<CsraDto>()
          .returnResult()
          .responseBody!!

        with(data) {
          assertThat(assessmentDate).isEqualTo("2025-12-14")
          assertThat(type).isEqualTo(AssessmentType.CSRF)
          assertThat(score.toString()).isEqualTo("1200")
          assertThat(status).isEqualTo(AssessmentStatusType.A)
          assertThat(assessmentStaffId).isEqualTo(staff.id)
        }
      }
    }
  }

  fun validFullCreateJsonRequest(): String =
    """
      { ${requiredFields()},
   "calculatedLevel": "HI",
   "committeeCode": "GOV",
   "nextReviewDate": "2026-12-15",
   "comment": "comment",
   "placementAgencyId": "LEI",
   "createdDateTime": "2025-12-04T12:34:56",
   "reviewLevel": "MED",
   "approvedLevel": "LOW",
   "evaluationDate": "2025-12-16",
   "evaluationResultCode": "APP",
   "reviewCommitteeCode": "CODE",
   "reviewCommitteeComment": "reviewCommitteeComment",
   "reviewPlacementAgencyId": "MDI",
   "reviewComment": "reviewComment"
      }
    """.trimIndent()

  fun validMinimalCreateJsonRequest(): String = "{ ${requiredFields()} }"

  fun requiredFields() =
    """
      "assessmentDate": "2025-12-14",
      "type": "CSRF",
      "score": "1200",
      "status": "A",
      "assessmentStaffId": ${staff.id},
      "createdBy": "BILLSTAFF"
    """.trimIndent()

  @DisplayName("GET /csra/booking/{bookingId}/sequence/{sequence}")
  @Nested
  inner class GetCsra {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/csra/booking/999/sequence/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/csra/booking/999/sequence/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/csra/booking/999/sequence/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    // TODO along with DSL functions to support
  }
}
