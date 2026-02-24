package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class PrisonerSearchResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setup() {
    repository.deleteOffenders()
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /search/prisoners/id-ranges")
  inner class GetAllPrisonersIdRanges {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=true")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=true")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=false")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed for role NOMIS_PRISONER_API__PRISONER_SEARCH_R`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=false&size=1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 if active is not provided`() {
        webTestClient.get().uri("/search/prisoners/id-ranges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0
      private var prisoner4: Long = 0

      @BeforeEach
      internal fun createPrisoners() {
        nomisDataBuilder.build {
          activePrisoner1 = offender(nomsId = "A1234TT") {
            booking {}
            booking {
              release()
            }
          }.rootOffenderId!!
          activePrisoner2 = offender(nomsId = "A1234SS") {
            alias(lastName = "SMITH")
            alias {
              booking {}
            }
          }.rootOffenderId!!
          inactivePrisoner1 = offender(nomsId = "A1234WW") {
            alias()
            booking {
              release()
            }
          }.rootOffenderId!!
          prisoner4 = offender(nomsId = "A1234YY").rootOffenderId!!
        }
      }

      @Test
      fun `will return list of pages of root offender ids ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=false&size=2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(3)
          .jsonPath("$.[0].fromRootOffenderId").isEqualTo(0)
          .jsonPath("$.[0].toRootOffenderId").isEqualTo(activePrisoner2)
          .jsonPath("$.[1].fromRootOffenderId").isEqualTo(activePrisoner2)
          .jsonPath("$.[1].toRootOffenderId").isEqualTo(prisoner4)
          .jsonPath("$.[2].fromRootOffenderId").isEqualTo(prisoner4)
          .jsonPath("$.[2].toRootOffenderId").isEqualTo(Long.MAX_VALUE)
      }

      @Test
      fun `will return list of pages of root offender ids for prisoners with active bookings ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=true&size=2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)
          .jsonPath("$.[0].fromRootOffenderId").isEqualTo(0)
          .jsonPath("$.[0].toRootOffenderId").isEqualTo(activePrisoner2)
          .jsonPath("$.[1].fromRootOffenderId").isEqualTo(activePrisoner2)
          .jsonPath("$.[1].toRootOffenderId").isEqualTo(Long.MAX_VALUE)
      }

      @Test
      fun `will default the size if not provided`() {
        webTestClient.get().uri("/search/prisoners/id-ranges?active=false")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("$.[0].fromRootOffenderId").isEqualTo(0)
          .jsonPath("$.[0].toRootOffenderId").isEqualTo(Long.MAX_VALUE)
      }
    }
  }

  @Nested
  @DisplayName("GET /search/prisoners/ids")
  inner class GetAllPrisonersIds {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/search/prisoners/ids?active=true&fromRootOffenderId=0&toRootOffenderId=100")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/search/prisoners/ids?active=true&fromRootOffenderId=0&toRootOffenderId=100")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=100")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed for role NOMIS_PRISONER_API__PRISONER_SEARCH_R`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=100")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 if active is not provided`() {
        webTestClient.get().uri("/search/prisoners/ids?fromRootOffenderId=0&toRootOffenderId=100")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return 400 if fromRootOffenderId is not provided`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&toRootOffenderId=100")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return 400 if toRootOffenderId is not provided`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&fromRootOffenderId=0")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0
      private var prisoner4: Long = 0

      @BeforeEach
      internal fun createPrisoners() {
        nomisDataBuilder.build {
          activePrisoner1 = offender(nomsId = "A1234TT") {
            booking {}
            booking {
              release()
            }
          }.rootOffenderId!!
          activePrisoner2 = offender(nomsId = "A1234SS") {
            alias(lastName = "SMITH")
            alias {
              booking {}
            }
          }.rootOffenderId!!
          inactivePrisoner1 = offender(nomsId = "A1234WW") {
            alias()
            booking {
              release()
            }
          }.rootOffenderId!!
          prisoner4 = offender(nomsId = "A1234YY").rootOffenderId!!
        }
      }

      @Test
      fun `will return list of all root offender ids ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=${Long.MAX_VALUE}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(4)
          .jsonPath("$.[0]").isEqualTo(activePrisoner1)
          .jsonPath("$.[1]").isEqualTo(activePrisoner2)
          .jsonPath("$.[2]").isEqualTo(inactivePrisoner1)
          .jsonPath("$.[3]").isEqualTo(prisoner4)
      }

      @Test
      fun `will return list of all active root offender ids ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/search/prisoners/ids?active=true&fromRootOffenderId=0&toRootOffenderId=${Long.MAX_VALUE}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)
          .jsonPath("$.[0]").isEqualTo(activePrisoner1)
          .jsonPath("$.[1]").isEqualTo(activePrisoner2)
      }

      @Test
      fun `will return specified list of root offender ids ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=$activePrisoner1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("$.[0]").isEqualTo(activePrisoner1)
      }
    }
  }
}
