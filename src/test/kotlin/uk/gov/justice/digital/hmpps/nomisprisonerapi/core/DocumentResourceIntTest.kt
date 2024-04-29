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
  private var bookingId: Long = 0L
  private var document1Id: Long = 0L
  private var document2Id: Long = 0L

  @BeforeEach
  internal fun createDocuments() {
    nomisDataBuilder.build {
      template1 = template(name = "CSIPA1_FNP", description = "CSIP first template")
      template2 = template(name = "ANY1_WM", description = "Second template")
      offender = offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
        bookingId = booking(agencyLocationId = "MDI") {
          document(fileName = "firstDoc.txt", body = "This is a test file.", template = template1)
          document(fileName = "secondDoc.txt", template = template2)
        }.bookingId
      }
    }
    document1Id = offender.bookings[0].documents[0].id
    document2Id = offender.bookings[0].documents[1].id
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
        webTestClient.get().uri("/documents/$document1Id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/documents/$document1Id")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/documents/$document1Id")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `document with no body should return not found`() {
      webTestClient.get().uri("/documents/$document2Id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Document with id $document2Id does not exist")
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
      webTestClient.get().uri("/documents/$document1Id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(String::class.java).isEqualTo("This is a test file.")
    }
  }

  @Nested
  @DisplayName("GET /documents/booking/{bookingId}")
  inner class GetDocumentByBookingIdAndTemplateName {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/documents/booking/$bookingId?templateName=CSIPA1_FNP")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/documents/booking/$bookingId?templateName=CSIPA1_FNP")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/documents/booking/$bookingId?templateName=CSIPA1_FNP")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `document with no body should return data`() {
      webTestClient.get().uri("/documents/booking/$bookingId?templateName=ANY1_WM")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("length()").isEqualTo(1)
        .jsonPath("[0].documentId").isEqualTo(document2Id)
    }

    @Test
    fun `unknown bookingId should return empty results`() {
      webTestClient.get().uri("/documents/booking/-99999?templateName=CSIPA1_FNP")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("length()").isEqualTo(0)
    }

    @Test
    fun `unknown template name should return empty results`() {
      webTestClient.get().uri("/documents/booking/$bookingId?templateName=NOT_FOUND")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("length()").isEqualTo(0)
    }

    @Test
    fun `will return error if only bookingId supplied`() {
      webTestClient.get().uri("/documents/booking/$bookingId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `will return a document id filtered by bookingId and template name`() {
      webTestClient.get().uri("/documents/booking/$bookingId?templateName=CSIPA1_FNP")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DOCUMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("length()").isEqualTo(1)
        .jsonPath("[0].documentId").isEqualTo(document1Id)
    }
  }
}
