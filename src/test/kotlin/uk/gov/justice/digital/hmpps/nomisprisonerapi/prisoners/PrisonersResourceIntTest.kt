package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

class PrisonersResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Nested
  @DisplayName("GET /prisoners/ids")
  inner class GetPrisonersActiveOnly {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      private lateinit var activePrisoner1: Offender
      private lateinit var activePrisoner2: Offender
      private lateinit var inactivePrisoner1: Offender

      @BeforeEach
      internal fun createPrisoners() {
        deletePrisoners()

        nomisDataBuilder.build {
          activePrisoner1 =
            offender(nomsId = "A1234TT") {
              booking {}
            }
          activePrisoner2 = offender(nomsId = "A1234SS") {
            booking {}
          }

          inactivePrisoner1 = offender(nomsId = "A1234WW") {
            booking(active = false)
          }
        }
      }

      @AfterEach
      fun deletePrisoners() {
        repository.deleteOffenders()
      }

      @Test
      fun `will return count of all active prisoners by default`() {
        webTestClient.get().uri("/prisoners/ids?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will return a page of prisoners ordered by booking id ASC`() {
        webTestClient.get().uri("/prisoners/ids?page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(2)
          .jsonPath("$.content[0].bookingId").isNumber
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234TT")
          .jsonPath("$.content[1].bookingId").isNumber
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234SS")
      }
    }
  }

  @Nested
  @DisplayName("GET /prisoners/ids?active=false")
  inner class GetPrisonersAll {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids?active=false")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids?active=false")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids?active=false")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0

      @BeforeEach
      internal fun createPrisoners() {
        deletePrisoners()

        nomisDataBuilder.build {
          offender(nomsId = "A1234TT") {
            activePrisoner1 = booking {}.bookingId
            booking {
              release()
            }
          }
          offender(nomsId = "A1234SS") {
            alias(lastName = "SMITH")
            alias {
              activePrisoner2 = booking {}.bookingId
            }
          }
          offender(nomsId = "A1234WW") {
            alias()
            inactivePrisoner1 = booking {
              release()
            }.bookingId
          }
          offender(nomsId = "A1234YY")
        }
      }

      @AfterEach
      fun deletePrisoners() {
        repository.deleteOffenders()
      }

      @Test
      fun `will return count of all prisoners with bookings`() {
        webTestClient.get().uri("/prisoners/ids?active=false&size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(3)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will return a page of prisoners ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/prisoners/ids?active=false&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(3)
          .jsonPath("$.numberOfElements").isEqualTo(3)
          .jsonPath("$.content[0].bookingId").isEqualTo(activePrisoner1)
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234TT")
          .jsonPath("$.content[0].status").isEqualTo("ACTIVE IN")
          .jsonPath("$.content[1].bookingId").isEqualTo(activePrisoner2)
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234SS")
          .jsonPath("$.content[1].status").isEqualTo("ACTIVE IN")
          .jsonPath("$.content[2].bookingId").isEqualTo(inactivePrisoner1)
          .jsonPath("$.content[2].offenderNo").isEqualTo("A1234WW")
          .jsonPath("$.content[2].status").isEqualTo("INACTIVE OUT")
      }
    }
  }

  @Nested
  inner class GetPrisonerBookings {
    private lateinit var bookingBxi: OffenderBooking
    private lateinit var bookingOut: OffenderBooking
    private lateinit var bookingTrn: OffenderBooking

    @BeforeEach
    internal fun createPrisoners() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234TT") {
          bookingBxi = booking(agencyLocationId = "BXI")
        }
        offender(nomsId = "A1234WW") {
          bookingOut = booking(active = false, agencyLocationId = "OUT")
        }
        offender(nomsId = "A1234XX") {
          bookingTrn = booking(active = false, agencyLocationId = "TRN")
        }
      }
    }

    @AfterEach
    fun deletePrisoners() {
      repository.deleteOffenders()
    }

    @Test
    fun `should return unauthorised with no auth token`() {
      webTestClient.post().uri("/prisoners/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when no role`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return empty list if not found`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [99999] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should get prisoner details`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [${bookingBxi.bookingId}] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].location").isEqualTo("BXI")
        .jsonPath("$[0].bookingId").isEqualTo(bookingBxi.bookingId)
        .jsonPath("$[0].offenderNo").isEqualTo("A1234TT")
    }

    @Test
    fun `should get inactive prisoner details`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [${bookingOut.bookingId}, ${bookingTrn.bookingId}] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].location").isEqualTo("OUT")
        .jsonPath("$[0].bookingId").isEqualTo(bookingOut.bookingId)
        .jsonPath("$[0].offenderNo").isEqualTo("A1234WW")
        .jsonPath("$[1].location").isEqualTo("TRN")
        .jsonPath("$[1].bookingId").isEqualTo(bookingTrn.bookingId)
        .jsonPath("$[1].offenderNo").isEqualTo("A1234XX")
    }
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/merges")
  inner class GetPrisonersMerges {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AK/merges")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AK/merges")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AK/merges")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      internal fun createMergeTransactions() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AK") {
            booking()
          }
          offender(nomsId = "A1234ZG") {
            booking()
          }
          mergeTransaction(
            requestDate = LocalDateTime.parse("2002-01-01T12:00:00"),
            nomsId1 = "A1234AK",
            rootOffenderId1 = 1,
            offenderBookId1 = 101,
            nomsId2 = "A1234AL",
            rootOffenderId2 = 2,
            offenderBookId2 = 102,
          )
          mergeTransaction(
            requestDate = LocalDateTime.parse("2024-01-01T12:00:00"),
            nomsId1 = "A1234AK",
            rootOffenderId1 = 3,
            offenderBookId1 = 103,
            nomsId2 = "A1234TL",
            rootOffenderId2 = 4,
            offenderBookId2 = 104,
          )
          mergeTransaction(
            requestDate = LocalDateTime.parse("2024-01-01T12:00:00"),
            nomsId1 = "A1234ZK",
            rootOffenderId1 = 4,
            offenderBookId1 = 104,
            nomsId2 = "A1234ZL",
            rootOffenderId2 = 5,
            offenderBookId2 = 105,
          )
          mergeTransaction(
            nomsId1 = "A1234ZZ",
            rootOffenderId1 = 6,
            offenderBookId1 = 106,
            nomsId2 = "A1234ZG",
            rootOffenderId2 = 7,
            offenderBookId2 = 107,
          )
        }
      }

      @AfterEach
      fun deletePrisoners() {
        repository.deleteMergeTransactions()
      }

      @Test
      fun `will return old merge and new merge with no filter`() {
        webTestClient.get().uri("/prisoners/A1234AK/merges")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
          .jsonPath("[0].deletedOffenderNo").isEqualTo("A1234AL")
          .jsonPath("[0].activeBookingId").isEqualTo(102)
          .jsonPath("[0].retainedOffenderNo").isEqualTo("A1234AK")
          .jsonPath("[0].previousBookingId").isEqualTo(101)
          .jsonPath("[0].requestDateTime").isEqualTo("2002-01-01T12:00:00")
          .jsonPath("[1].deletedOffenderNo").isEqualTo("A1234TL")
          .jsonPath("[1].activeBookingId").isEqualTo(104)
          .jsonPath("[1].retainedOffenderNo").isEqualTo("A1234AK")
          .jsonPath("[1].previousBookingId").isEqualTo(103)
          .jsonPath("[1].requestDateTime").isEqualTo("2024-01-01T12:00:00")
      }

      @Test
      fun `will return merges after filter date`() {
        webTestClient.get().uri("/prisoners/A1234AK/merges?fromDate=2023-01-01")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("[0].deletedOffenderNo").isEqualTo("A1234TL")
          .jsonPath("[0].activeBookingId").isEqualTo(104)
          .jsonPath("[0].retainedOffenderNo").isEqualTo("A1234AK")
          .jsonPath("[0].previousBookingId").isEqualTo(103)
          .jsonPath("[0].requestDateTime").isEqualTo("2024-01-01T12:00:00")
      }

      @Test
      fun `will return empty list when prisoner not found with a merge transaction`() {
        webTestClient.get().uri("/prisoners/A9898AK/merges?fromDate=2024-04-01")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `will return merge when retained ID is in the number 2 field`() {
        webTestClient.get().uri("/prisoners/A1234ZG/merges")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("[0].deletedOffenderNo").isEqualTo("A1234ZZ")
          .jsonPath("[0].activeBookingId").isEqualTo(107)
          .jsonPath("[0].retainedOffenderNo").isEqualTo("A1234ZG")
          .jsonPath("[0].previousBookingId").isEqualTo(106)
      }
    }
  }
}
