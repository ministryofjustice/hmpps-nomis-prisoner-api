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
    private lateinit var personMinimal: Person
    private lateinit var personFull: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        personMinimal = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
        }
        personFull = person(
          firstName = "JANE",
          lastName = "NARK",
          middleName = "LIZ",
          dateOfBirth = "1999-12-22",
          gender = "F",
          title = "DR",
          language = "VIE",
          interpreterRequired = true,
          domesticStatus = "M",
          deceasedDate = "2023-12-22",
          isStaff = true,
          isRemitter = true,
          keepBiometrics = true,
        ) {
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
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
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
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("personId").isEqualTo(personMinimal.id)
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("lastName").isEqualTo("BOG")
          .jsonPath("middleName").doesNotExist()
          .jsonPath("dateOfBirth").doesNotExist()
          .jsonPath("gender").doesNotExist()
          .jsonPath("title").doesNotExist()
          .jsonPath("language").doesNotExist()
          .jsonPath("interpreterRequired").isEqualTo(false)
          .jsonPath("domesticStatus").doesNotExist()
          .jsonPath("deceasedDate").doesNotExist()
          .jsonPath("isStaff").doesNotExist()
          .jsonPath("isRemitter").doesNotExist()
          .jsonPath("keepBiometrics").isEqualTo(false)
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }

      @Test
      fun `will return full person data`() {
        webTestClient.get().uri("/persons/${personFull.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("personId").isEqualTo(personFull.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("middleName").isEqualTo("LIZ")
          .jsonPath("dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("gender.code").isEqualTo("F")
          .jsonPath("gender.description").isEqualTo("Female")
          .jsonPath("title.code").isEqualTo("DR")
          .jsonPath("title.description").isEqualTo("Dr")
          .jsonPath("language.code").isEqualTo("VIE")
          .jsonPath("language.description").isEqualTo("Vietnamese")
          .jsonPath("interpreterRequired").isEqualTo(true)
          .jsonPath("domesticStatus.code").isEqualTo("M")
          .jsonPath("domesticStatus.description").isEqualTo("Married or in civil partnership")
          .jsonPath("deceasedDate").isEqualTo("2023-12-22")
          .jsonPath("isStaff").isEqualTo(true)
          .jsonPath("isRemitter").isEqualTo(true)
          .jsonPath("keepBiometrics").isEqualTo(true)
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }
    }
  }
}