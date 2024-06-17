package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
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

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Test
  fun `DSL setup check - latest booking on root`() {
    lateinit var rootOffender: Offender
    lateinit var aliasOffender1: Offender
    lateinit var aliasOffender2: Offender
    lateinit var activeBooking: OffenderBooking
    lateinit var oldBooking: OffenderBooking
    lateinit var aliasBooking: OffenderBooking
    lateinit var oldAliasBooking: OffenderBooking

    nomisDataBuilder.build {
      rootOffender = offender(nomsId = "A1234TT") {
        activeBooking = booking {}
        oldBooking = booking(active = false)

        aliasOffender1 = alias(lastName = "ALIAS1") {
          aliasBooking = booking(active = false)
          oldAliasBooking = booking(active = false)
        }
        aliasOffender2 = alias(lastName = "ALIAS2")
      }
    }
    val root = repository.getOffender(nomsId = "A1234TT")!!
    val alias1 = repository.getOffender(aliasOffender1.id)!!
    val alias2 = repository.getOffender(aliasOffender2.id)!!
    with(root) {
      assertThat(id).isEqualTo(rootOffender.id)
      assertThat(bookings).extracting("bookingId", "offender.id", "rootOffender.id", "bookingSequence", "active")
        .containsExactly(
          Tuple(activeBooking.bookingId, rootOffender.id, rootOffender.id, 1, true),
          Tuple(oldBooking.bookingId, rootOffender.id, rootOffender.id, 2, false),
        )
      assertThat(getAllBookings()).extracting("bookingId")
        .containsExactlyInAnyOrder(
          activeBooking.bookingId,
          oldBooking.bookingId,
          aliasBooking.bookingId,
          oldAliasBooking.bookingId,
        )
    }
    with(alias1) {
      assertThat(id).isEqualTo(aliasOffender1.id)
      assertThat(bookings).extracting("bookingId", "offender.id", "rootOffender.id", "bookingSequence", "active")
        .containsExactly(
          Tuple(aliasBooking.bookingId, aliasOffender1.id, rootOffender.id, 3, false),
          Tuple(oldAliasBooking.bookingId, aliasOffender1.id, rootOffender.id, 4, false),
        )
      assertThat(getAllBookings()).extracting("bookingId")
        .containsExactlyInAnyOrder(
          activeBooking.bookingId,
          oldBooking.bookingId,
          aliasBooking.bookingId,
          oldAliasBooking.bookingId,
        )
    }
    with(alias2) {
      assertThat(id).isEqualTo(aliasOffender2.id)
      assertThat(bookings).isEmpty()
      assertThat(getAllBookings()).extracting("bookingId")
        .containsExactlyInAnyOrder(
          activeBooking.bookingId,
          oldBooking.bookingId,
          aliasBooking.bookingId,
          oldAliasBooking.bookingId,
        )
    }
  }

  @Test
  fun `DSL setup check - latest booking on alias`() {
    lateinit var rootOffender: Offender
    lateinit var aliasOffender1: Offender
    lateinit var activeBooking: OffenderBooking
    lateinit var oldBooking: OffenderBooking

    nomisDataBuilder.build {
      rootOffender = offender(nomsId = "A1234TT") {
        oldBooking = booking(active = false, bookingSequence = 2)
        aliasOffender1 = alias(lastName = "ALIAS1") {
          activeBooking = booking(bookingSequence = 1)
        }
      }
    }
    val root = repository.getOffender(nomsId = "A1234TT")!!
    val alias1 = repository.getOffender(aliasOffender1.id)!!
    with(root) {
      assertThat(id).isEqualTo(rootOffender.id)
      assertThat(bookings).extracting("bookingId", "offender.id", "rootOffender.id", "bookingSequence", "active")
        .containsExactly(
          Tuple(oldBooking.bookingId, rootOffender.id, rootOffender.id, 2, false),
        )
      assertThat(getAllBookings()).extracting("bookingId")
        .containsExactlyInAnyOrder(
          activeBooking.bookingId,
          oldBooking.bookingId,
        )
    }
    with(alias1) {
      assertThat(id).isEqualTo(aliasOffender1.id)
      assertThat(bookings).extracting("bookingId", "offender.id", "rootOffender.id", "bookingSequence", "active")
        .containsExactly(
          Tuple(activeBooking.bookingId, aliasOffender1.id, rootOffender.id, 1, true),
        )
      assertThat(getAllBookings()).extracting("bookingId")
        .containsExactlyInAnyOrder(
          activeBooking.bookingId,
          oldBooking.bookingId,
        )
    }
  }

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
  @DisplayName("GET /prisoners/ids/active")
  inner class GetPrisonerIdsActive {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids/active")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids/active")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids/active")
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

      @Test
      fun `will return count of all active prisoners by default`() {
        webTestClient.get().uri("/prisoners/ids/active?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will return a page of prisoners ordered by booking id ASC`() {
        webTestClient.get().uri("/prisoners/ids/active?page=0")
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
  inner class GetPrisonersIncludingInactive {
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

      @Test
      fun `access allowed for SYNCHRONISATION_REPORTING`() {
        webTestClient.get().uri("/prisoners/ids?active=false&size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `access allowed for NOMIS_ALERTS`() {
        webTestClient.get().uri("/prisoners/ids?active=false&size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0

      @BeforeEach
      internal fun createPrisoners() {
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
          .jsonPath("$.content[1].bookingId").isEqualTo(activePrisoner2)
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234SS")
          .jsonPath("$.content[2].bookingId").isEqualTo(inactivePrisoner1)
          .jsonPath("$.content[2].offenderNo").isEqualTo("A1234WW")
      }
    }
  }

  @Nested
  @DisplayName("GET /prisoners/ids/all")
  inner class GetPrisonersAll {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed for SYNCHRONISATION_REPORTING`() {
        webTestClient.get().uri("/prisoners/ids/all")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `access allowed for NOMIS_ALERTS`() {
        webTestClient.get().uri("/prisoners/ids/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private var activePrisoner1: Long = 0
      private var activePrisoner2: Long = 0
      private var inactivePrisoner1: Long = 0

      @BeforeEach
      internal fun createPrisoners() {
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

      @Test
      fun `will return count of all prisoners even without a booking`() {
        webTestClient.get().uri("/prisoners/ids/all?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(4)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will return a page of prisoners ordered by rootOffenderId ASC`() {
        webTestClient.get().uri("/prisoners/ids/all?page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(4)
          .jsonPath("$.numberOfElements").isEqualTo(4)
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234TT")
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234SS")
          .jsonPath("$.content[2].offenderNo").isEqualTo("A1234WW")
          .jsonPath("$.content[3].offenderNo").isEqualTo("A1234YY")
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
  inner class GetPreviousBooking {
    private var bookingId: Long = 0
    private var previousBookingId: Long = 0
    private var firstEverBookingId: Long = 0
    private var bookingIdOnlyOne: Long = 0
    private var aliasLatestBookingId: Long = 0
    private var aliasPreviousBookingId: Long = 0
    private var aliasFirstEverBookingId: Long = 0

    @BeforeEach
    internal fun createPrisoners() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234TT") {
          bookingId = booking().bookingId
          previousBookingId = booking {
            release()
          }.bookingId
          firstEverBookingId = booking {
            release()
          }.bookingId
        }
        offender(nomsId = "A1234WW") {
          bookingIdOnlyOne = booking().bookingId
        }
        offender(nomsId = "A1234HH") {
          alias {
            aliasLatestBookingId = booking().bookingId
          }
          aliasPreviousBookingId = booking {
            release()
          }.bookingId
          alias {
            aliasFirstEverBookingId = booking {
              release()
            }.bookingId
          }
        }
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/$bookingId/previous")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/$bookingId/previous")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/$bookingId/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 for a prisoner that does not exist`() {
        webTestClient.get().uri("/prisoners/A9999TT/bookings/$bookingId/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 404 for a booking that  does not exist for prisoner`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/9999/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 404 for a booking that has no previous booking`() {
        webTestClient.get().uri("/prisoners/A1234WW/bookings/$bookingIdOnlyOne/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return previous booking for the latest booking`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/$bookingId/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(previousBookingId)
          .jsonPath("bookingSequence").isEqualTo(2)
      }

      @Test
      fun `will return first booking for the middle booking`() {
        webTestClient.get().uri("/prisoners/A1234TT/bookings/$previousBookingId/previous")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(firstEverBookingId)
          .jsonPath("bookingSequence").isEqualTo(3)
      }

      @Nested
      inner class WithAliases {
        @Test
        fun `will return previous booking for the latest booking`() {
          webTestClient.get().uri("/prisoners/A1234HH/bookings/$aliasLatestBookingId/previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("bookingId").isEqualTo(aliasPreviousBookingId)
            .jsonPath("bookingSequence").isEqualTo(2)
        }

        @Test
        fun `will return first booking for the middle booking`() {
          webTestClient.get().uri("/prisoners/A1234HH/bookings/$aliasPreviousBookingId/previous")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("bookingId").isEqualTo(aliasFirstEverBookingId)
            .jsonPath("bookingSequence").isEqualTo(3)
        }
      }
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
