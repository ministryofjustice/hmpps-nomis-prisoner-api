package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository

class CorporateResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var corporateRepository: CorporateRepository

  @Nested
  @DisplayName("GET /corporates/{corporateId}")
  inner class GetCorporate {
    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      private lateinit var corporate: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
        }
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if corporate does not exist`() {
        webTestClient.get().uri("/corporates/999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      private lateinit var corporate: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
        }
      }

      @Test
      fun `will find a corporate when it exists`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(corporate.id)
      }
    }
  }
}
