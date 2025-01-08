package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate

class CorePersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /core-person/{prisonNumber}")
  @Nested
  inner class GetOffender {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offenderMinimal = offender(
          nomsId = "A1234BC",
          firstName = "JOHN",
          lastName = "BOG",
          birthDate = null,
        ) {
        }
        offenderFull = offender(
          nomsId = "B1234CD",
          firstName = "JANE",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
          genderCode = "F",
        ) {
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic offender data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderMinimal.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderMinimal.id)
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("lastName").isEqualTo("BOG")
          .jsonPath("dateOfBirth").doesNotExist()
          .jsonPath("sex.code").isEqualTo("M")
          .jsonPath("sex.description").isEqualTo("Male")
      }

      @Test
      fun `will return full offender data`() {
        webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderFull.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderFull.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("sex.code").isEqualTo("F")
          .jsonPath("sex.description").isEqualTo("Female")
      }
    }
  }
}
