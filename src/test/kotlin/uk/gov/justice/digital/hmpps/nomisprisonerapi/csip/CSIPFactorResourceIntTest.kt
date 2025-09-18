package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDate

class CSIPFactorResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var csip1: CSIPReport
  private lateinit var csip2: CSIPReport

  @BeforeEach
  internal fun createCSIPReports() {
    nomisDataBuilder.build {
      offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
        booking(agencyLocationId = "MDI") {
          csip1 = csipReport(
            staffAssaulted = true,
            staffAssaultedName = "Assaulted Person",
            releaseDate = LocalDate.parse("2028-11-25"),
          ) {
            factor()
            factor(type = "GAN")
          }
          csip2 = csipReport {
            factor(type = "AFL", comment = "Beer in the cell")
          }
        }
      }
    }
  }

  @AfterEach
  internal fun deleteCSIPReports() {
    repository.delete(csip1)
    repository.delete(csip2)
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /csip/factors/{id}")
  inner class GetCSIPFactor {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/csip/factors/${csip1.factors[0].id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/csip/factors/${csip1.factors[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/csip/factors/${csip1.factors[0].id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown csip report should return not found`() {
      webTestClient.get().uri("/csip/factors/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: CSIP Factor with id=999999 does not exist")
        }
    }

    @Test
    fun `will return a csip Factor by Id`() {
      webTestClient.get().uri("/csip/factors/${csip1.factors[0].id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.factors[0].id)
        .jsonPath("type.code").isEqualTo("BUL")
        .jsonPath("type.description").isEqualTo("Bullying")
        .jsonPath("comment").doesNotExist()
    }

    @Test
    fun `will return a csip Factor with with all fields set`() {
      webTestClient.get().uri("/csip/factors/${csip2.factors[0].id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip2.factors[0].id)
        .jsonPath("type.code").isEqualTo("AFL")
        .jsonPath("type.description").isEqualTo("Alcohol/Fermenting Liquid")
        .jsonPath("comment").isEqualTo("Beer in the cell")
    }
  }

  @DisplayName("DELETE /csip/factors/{csipId}")
  @Nested
  inner class DeleteCSIPFactor {
    private lateinit var csipToDelete: CSIPReport

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY", firstName = "Jim", lastName = "Jones") {
          booking(agencyLocationId = "MDI") {
            csipToDelete = csipReport {
              factor(type = "GAN")
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(csipToDelete)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class NoValidation {
      @Test
      fun `return 204 even when does not exist`() {
        webTestClient.delete().uri("/csip/factors/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the csip factor`() {
        webTestClient.get().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isOk
        webTestClient.delete().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isNoContent
        webTestClient.get().uri("/csip/factors/${csipToDelete.factors[0].id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
