package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

class ContactPersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var personRepository: PersonRepository

  @DisplayName("GET /persons/{personId}")
  @Nested
  inner class GetPerson {
    private lateinit var person: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        person = person(firstName = "JANE", lastName = "NARK") {
        }
      }
    }

    @AfterEach
    fun tearDown() {
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/persons/${person.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when person not found`() {
        webTestClient.get().uri("/persons/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic person data`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("personId").isEqualTo(person.id)
      }
    }
  }
}
