package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender

class DocumentResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var template1: IWPTemplate
  private lateinit var template2: IWPTemplate
  private lateinit var offender: Offender

  @BeforeEach
  internal fun createDocuments() {
    nomisDataBuilder.build {
      template1 = template(name = "CSIPA1_FNP", description = "CSIP first template")
      template2 = template(name = "ANY1_WM", description = "Second template")
      offender = offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
        booking(agencyLocationId = "MDI") {
          document(fileName = "firstDoc.txt", body = "This is a test file.", template = template1)
          document(fileName = "secondDoc.txt", template = template2)
        }
      }
    }
  }

  @AfterEach
  internal fun deleteTestData() {
    repository.deleteOffenders()
    repository.deleteTemplates()
  }

  @Nested
  @DisplayName("GET /documents/{id}")
  inner class GetDocumentById {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/documents/${offender.bookings[0].documents[0].id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/documents/${offender.bookings[0].documents[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/documents/${offender.bookings[0].documents[0].id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown document should return not found`() {
      webTestClient.get().uri("/documents/-99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Document with id -99999 does not exist")
        }
    }

    @Test
    fun `will return a document by id`() {
      webTestClient.get().uri("/documents/${offender.bookings[0].documents[0].id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(String::class.java).isEqualTo("This is a test file.")
    }
  }
}
