package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
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
          .body(BodyInserters.fromValue(validCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validCreateJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/csra/A1111AA")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validCreateJsonRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a csra`() {
        val created = webTestClient.post().uri("/csra/A1111AA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validCreateJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody(CsraCreateResponse::class.java)
          .returnResult()
          .responseBody!! // NPE ??

        val data = webTestClient.get().uri("/csra/booking/${created.bookingId}/sequence/${created.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isOk
          .expectBody(CsraDto::class.java)
          .returnResult()
          .responseBody!!

        with(data) {
          assertThat(bookingId).isEqualTo(created.bookingId)
          assertThat(sequence).isEqualTo(created.sequence)
          assertThat(assessmentDate).isEqualTo("2025-12-14")
          assertThat(calculatedLevel).isEqualTo("HI")
          assertThat(score.toString()).isEqualTo("1200")
          assertThat(status).isEqualTo(AssessmentStatusType.A)
          assertThat(assessmentStaffId).isEqualTo(12345)
          assertThat(calculatedLevel).isEqualTo("xxxxx")
          assertThat(calculatedLevel).isEqualTo("xxxxx")
          assertThat(calculatedLevel).isEqualTo("xxxxx")
          assertThat(calculatedLevel).isEqualTo("xxxxx")
        }
      }
    }
  }

  fun validCreateJsonRequest() : String =
    """
      {
   "assessmentDate": "2025-12-14",
   "type": "CSRF",
   "calculatedLevel": "HI",
   "score": "1200",
   "status": "A",
   "assessmentStaffId": ${staff.id},
   "committeeCode": "GOV",
   "nextReviewDate": "2026-12-15",
   "comment": "comment",
   "placementAgencyId": "LEI",
   "createdDateTime": "2025-12-04T12:34:56",
   "createdBy": "BILLSTAFF",
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
  }
}
