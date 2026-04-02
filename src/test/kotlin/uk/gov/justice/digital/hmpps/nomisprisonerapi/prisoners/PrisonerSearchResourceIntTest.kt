package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@TestInstance(PER_CLASS)
class PrisonerSearchResourceIntTest : IntegrationTestBase() {
  @BeforeAll
  fun tearDown(): Unit = deleteOffenders()

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
    @TestInstance(PER_CLASS)
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0
      private var prisoner4: Long = 0

      @BeforeAll
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

      @AfterAll
      fun tearDown(): Unit = deleteOffenders()

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
  inner class GetAllPrisonersInRange {
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
    @TestInstance(PER_CLASS)
    inner class HappyPath {
      private lateinit var activePrisoner1: String
      private var activePrisoner1Id: Long = 0
      private lateinit var activePrisoner2: String
      private lateinit var inactivePrisoner1: String
      private lateinit var prisoner4: String

      @BeforeAll
      internal fun createPrisoners() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234TT") {
            booking {}
            booking {
              release()
            }
          }.also {
            activePrisoner1 = it.nomsId
            activePrisoner1Id = it.rootOffenderId!!
          }
          activePrisoner2 = offender(nomsId = "A1234SS") {
            alias(lastName = "SMITH")
            alias {
              booking {}
            }
          }.nomsId
          inactivePrisoner1 = offender(nomsId = "A1234WW") {
            alias()
            booking {
              release()
            }
          }.nomsId
          prisoner4 = offender(nomsId = "A1234YY").nomsId
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteOffenders()

      @Test
      fun `will return list of all root offender ids`() {
        webTestClient.get()
          .uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=${Long.MAX_VALUE}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(4)
          .jsonPath("$").value<List<String>> {
            assertThat(it).containsOnly(activePrisoner1, activePrisoner2, inactivePrisoner1, prisoner4)
          }
      }

      @Test
      fun `will return list of all active root offender ids`() {
        webTestClient.get()
          .uri("/search/prisoners/ids?active=true&fromRootOffenderId=0&toRootOffenderId=${Long.MAX_VALUE}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)
          .jsonPath("$").value<List<String>> {
            assertThat(it).containsOnly(activePrisoner1, activePrisoner2)
          }
      }

      @Test
      fun `will return specified list of root offender ids`() {
        webTestClient.get()
          .uri("/search/prisoners/ids?active=false&fromRootOffenderId=0&toRootOffenderId=$activePrisoner1Id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("$").value<List<String>> {
            assertThat(it).containsOnly(activePrisoner1)
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /search/prisoners/{prisonerNumber}/bookings")
  inner class GetAllBookingsForPrisoner {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/search/prisoners/A1234AA/bookings")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/search/prisoners/A1234AA/bookings")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/search/prisoners/A1234AA/bookings")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `prisoner does not exist`() {
        webTestClient.get().uri("/search/prisoners/A1234YZ/bookings")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class HappyPath {
      private val prisonerNumber1: String = "A4567BB"
      private val prisonerNumber2: String = "A4567CC"
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var booking3: OffenderBooking

      @BeforeAll
      internal fun createPrisoners() {
        nomisDataBuilder.build {
          offender(nomsId = prisonerNumber1) {
            booking1 = booking {}
            booking2 = booking {}
            booking3 = booking {}
          }
          offender(nomsId = prisonerNumber2)
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteOffenders()

      @Test
      fun `will return list of all booking ids`() {
        webTestClient.get().uri("/search/prisoners/$prisonerNumber1/bookings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(3)
          .jsonPath("$").value<List<Int>> {
            assertThat(it).containsOnly(
              booking1.bookingId.toInt(),
              booking2.bookingId.toInt(),
              booking3.bookingId.toInt(),
            )
          }
      }

      @Test
      fun `will return empty list if no bookings`() {
        webTestClient.get().uri("/search/prisoners/$prisonerNumber2/bookings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(0)
      }
    }
  }
}
