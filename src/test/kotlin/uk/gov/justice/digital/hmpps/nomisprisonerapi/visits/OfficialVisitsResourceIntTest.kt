package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.GENERAL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OfficialVisitsResourceIntTest(@Autowired private val visitVisitorRepository: VisitVisitorRepository) : IntegrationTestBase() {
  @Autowired
  private lateinit var visitRepository: VisitRepository

  @Autowired
  private lateinit var visitOrderRepository: VisitOrderRepository

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @Autowired
  private lateinit var personRepository: PersonRepository

  @Autowired
  private lateinit var agencyInternalLocationRepository: AgencyInternalLocationRepository

  @Autowired
  private lateinit var agencyVisitDayRepository: AgencyVisitDayRepository

  @Autowired
  private lateinit var agencyVisitTimeRepository: AgencyVisitTimeRepository

  @DisplayName("GET /official-visits/ids")
  @Nested
  inner class GetOfficialVisitIds {
    lateinit var visitIds: MutableList<Long>

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        val visitor = person { }

        offender(nomsId = "A1234TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            visitIds = (1..30).map { visit(visitTypeCode = "OFFI", startDateTimeString = "2023-01-01T10:00:00").id }.toMutableList()
            visit(visitTypeCode = "SCON")
          }
        }
        offender(nomsId = "A4321TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-01-01T10:00:00"), agyLocId = "BXI")
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-02-01T10:00:00"), agyLocId = "BXI")
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-03-01T10:00:00"), agyLocId = "LEI")
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/official-visits/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/official-visits/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/official-visits/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 visits`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(33)
          .jsonPath("content.size()").isEqualTo(20)
          .jsonPath("page.number").isEqualTo(0)
          .jsonPath("page.totalPages").isEqualTo(2)
          .jsonPath("page.size").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(33)
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("page.number").isEqualTo(0)
          .jsonPath("page.totalPages").isEqualTo(33)
          .jsonPath("page.size").isEqualTo(1)
      }

      @Test
      fun `can filter by one or more prisons`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("prisonIds", "LEI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(1)

        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("prisonIds", "LEI")
            .queryParam("prisonIds", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(3)
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("prisonIds", "LEI")
            .queryParam("prisonIds", "BXI")
            .queryParam("prisonIds", "MDI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(33)
      }

      @Test
      fun `can filter by from date`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("fromDate", "2023-03-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(31)
      }

      @Test
      fun `can filter by to date`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("toDate", "2023-03-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(3)
      }

      @Test
      fun `can filter by from and to dates`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("toDate", "2023-03-01")
            .queryParam("fromDate", "2023-02-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(2)
      }

      @Test
      fun `can filter by from and to dates and prison`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("toDate", "2023-03-01")
            .queryParam("fromDate", "2023-02-01")
            .queryParam("prisonIds", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(1)
      }

      @Test
      fun `id just contains visit id`() {
        val pageResponse: VisitIdPageResponse = webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(pageResponse.content).hasSize(2)
        assertThat(pageResponse.content[0].visitId).isEqualTo(visitIds[0])
        assertThat(pageResponse.content[1].visitId).isEqualTo(visitIds[1])
      }
    }
  }

  @DisplayName("GET /official-visits/ids/all-from-id")
  @Nested
  inner class GetOfficialVisitIdsFromId {
    lateinit var visitIds: MutableList<Long>

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        val visitor = person { }

        offender(nomsId = "A1234TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            visitIds = (1..30).map { visit(visitTypeCode = "OFFI", startDateTimeString = "2023-01-01T10:00:00").id }.toMutableList()
            visit(visitTypeCode = "SCON")
          }
        }
        offender(nomsId = "A4321TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-01-01T10:00:00"), agyLocId = "BXI")
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-02-01T10:00:00"), agyLocId = "BXI")
            visit(visitTypeCode = "OFFI", createdDatetime = LocalDateTime.parse("2023-03-01T10:00:00"), agyLocId = "LEI")
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/official-visits/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/official-visits/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/official-visits/ids/all-from-id")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 visits`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(1)
      }

      @Test
      fun `can filter by one or more prisons`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("prisonIds", "LEI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(1)

        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("prisonIds", "LEI")
            .queryParam("prisonIds", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(3)
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("prisonIds", "LEI")
            .queryParam("prisonIds", "BXI")
            .queryParam("prisonIds", "MDI")
            .queryParam("size", "100")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(33)
      }

      @Test
      fun `can filter by from date`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("fromDate", "2023-03-01")
            .queryParam("size", "100")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(31)
      }

      @Test
      fun `can filter by to date`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("toDate", "2023-03-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(3)
      }

      @Test
      fun `can filter by from and to dates`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("toDate", "2023-03-01")
            .queryParam("fromDate", "2023-02-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(2)
      }

      @Test
      fun `can filter by from and to dates and prison`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("toDate", "2023-03-01")
            .queryParam("fromDate", "2023-02-01")
            .queryParam("prisonIds", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(1)
      }

      @Test
      fun `id just contains visit id`() {
        val pageResponse: VisitIdsPage = webTestClient.get().uri {
          it.path("/official-visits/ids/all-from-id")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(pageResponse.ids).hasSize(2)
        assertThat(pageResponse.ids[0].visitId).isEqualTo(visitIds[0])
        assertThat(pageResponse.ids[1].visitId).isEqualTo(visitIds[1])
      }
    }
  }

  @DisplayName("POST /prisoner/{offenderNo}/official-visits")
  @Nested
  inner class CreateOfficialVisit {
    lateinit var visitHall: AgencyInternalLocation
    var visitSlotId: Long = 0
    val prisonId = "BXI"
    private lateinit var person: Person
    private lateinit var contact: OffenderContactPerson
    private var latestBookingId = 0L
    private val offenderNo = "A1234KT"

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }

        visitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-1",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        agencyVisitDay(prisonerId = prisonId, weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlotId = visitSlot(
              agencyInternalLocation = visitHall,
              maxGroups = 10,
              maxAdults = 20,
            ).id
          }
        }

        person = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            contact = contact(
              person = person,
              contactType = "O",
              relationshipType = "DR",
            )
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(visitHall)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `when prisoner not found`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", "A9999ZZ")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `when prisonId is not valid`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = "ZZZZ",
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Prison ZZZZ does not exist")
      }

      @Test
      fun `when location room is not valid`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = 9999,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Internal location 9999 does not exist")
      }

      @Test
      fun `when visit slot is not valid`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = 99,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Visit slot 99 does not exist")
      }

      @Test
      fun `when visit status is not valid`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "ZZZ",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Visit status code ZZZ does not exist")
      }

      @Test
      fun `when search type status is not valid`() {
        webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
              prisonerSearchTypeCode = "ZZZ",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Search Level code ZZZ does not exist")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create visit only mandatory data`() {
        val createdVisitId: Long = webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectBodyResponse<OfficialVisitResponse>().visitId

        nomisDataBuilder.runInTransaction {
          with(visitRepository.findByIdOrNull(createdVisitId)!!) {
            assertThat(id).isEqualTo(createdVisitId)
            assertThat(agencyVisitSlot!!.id).isEqualTo(visitSlotId)
            assertThat(offenderBooking.bookingId).isEqualTo(latestBookingId)
            assertThat(location.id).isEqualTo(prisonId)
            assertThat(agencyInternalLocation!!.locationCode).isEqualTo("BXI-VISIT-1")
            assertThat(startDateTime).isEqualTo(LocalDateTime.parse("2024-01-01T10:00:00"))
            assertThat(endDateTime).isEqualTo(LocalDateTime.parse("2024-01-01T11:00:00"))
            assertThat(visitDate).isEqualTo(LocalDate.parse("2024-01-01"))
            assertThat(visitStatus.description).isEqualTo("Scheduled")
            assertThat(visitType.description).isEqualTo("Official Visit")

            assertThat(commentText).isNull()
            assertThat(visitorConcernText).isNull()
            assertThat(searchLevel).isNull()
            assertThat(visitOrder).isNull()
            assertThat(visitors).hasSize(1)
            assertThat(overrideBanStaff).isNull()
          }
        }
      }

      @Test
      fun `will create visit with all data`() {
        val createdVisitId: Long = webTestClient.post().uri("/prisoner/{offenderNo}/official-visits", offenderNo)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitRequest(
              visitSlotId = visitSlotId,
              prisonId = prisonId,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = visitHall.locationId,
              visitStatusCode = "SCH",
              visitOutcomeCode = "CANC",
              prisonerAttendanceCode = "ATT",
              prisonerSearchTypeCode = "FULL",
              visitorConcernText = "Quite concerned",
              commentText = "First visit",
              overrideBanStaffUsername = "KOFEADDY_GEN",
            ),
          )
          .exchange()
          .expectBodyResponse<OfficialVisitResponse>().visitId

        nomisDataBuilder.runInTransaction {
          with(visitRepository.findByIdOrNull(createdVisitId)!!) {
            assertThat(id).isEqualTo(createdVisitId)
            assertThat(agencyVisitSlot!!.id).isEqualTo(visitSlotId)
            assertThat(offenderBooking.bookingId).isEqualTo(latestBookingId)
            assertThat(location.id).isEqualTo(prisonId)
            assertThat(agencyInternalLocation!!.locationCode).isEqualTo("BXI-VISIT-1")
            assertThat(startDateTime).isEqualTo(LocalDateTime.parse("2024-01-01T10:00:00"))
            assertThat(endDateTime).isEqualTo(LocalDateTime.parse("2024-01-01T11:00:00"))
            assertThat(visitDate).isEqualTo(LocalDate.parse("2024-01-01"))
            assertThat(visitStatus.description).isEqualTo("Scheduled")
            assertThat(visitType.description).isEqualTo("Official Visit")

            assertThat(commentText).isEqualTo("First visit")
            assertThat(visitorConcernText).isEqualTo("Quite concerned")
            assertThat(searchLevel!!.description).isEqualTo("Full Search")
            assertThat(visitOrder).isNull()
            assertThat(visitors.filter { it.person != null }).isEmpty()
            assertThat(visitors.filter { it.offenderBooking != null }).hasSize(1)
            with(visitors.first { it.offenderBooking != null }) {
              assertThat(offenderBooking!!.bookingId).isEqualTo(latestBookingId)
              assertThat(eventOutcome!!.description).isEqualTo("Attended")
              assertThat(eventStatus!!.description).isEqualTo("Scheduled (Approved)")
              assertThat(eventId).isNotNull()
            }
            assertThat(overrideBanStaff!!.firstName).isEqualTo("KOFE")
          }
        }
      }
    }
  }

  @DisplayName("GET /official-visits/{visitId}")
  @Nested
  inner class GetOfficialVisit {
    var officialVisitId = 0L
    var officialVisitReferenceDataIssue1Id = 0L
    var officialVisitReferenceDataIssue2Id = 0L
    var socialVisitId = 0L
    var bookingId = 0L
    lateinit var visitSlot: AgencyVisitSlot
    lateinit var room: AgencyInternalLocation
    lateinit var staff: Staff
    lateinit var johnDupont: Person
    lateinit var janeDoe: Person
    var janeDoeVisitorId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account("T_SMITH_ADM", type = ADMIN)
          account("T_SMITH_GEN", type = GENERAL)
        }
        room = agencyInternalLocation(
          locationCode = "BXI-VISIT-1",
          locationType = "VISIT",
          prisonId = "BXI",
        )
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = room,
              maxGroups = null,
              maxAdults = null,
            )
          }
        }

        johnDupont = person(firstName = "JOHN", lastName = "DUPONT")
        janeDoe = person(firstName = "JANE", lastName = "DOE", dateOfBirth = "1965-07-19")

        offender(nomsId = "A1234TT") {
          bookingId = booking {
            visitBalance { }
            contact(
              person = johnDupont,
              contactType = "O",
              relationshipType = "DR",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            contact(
              person = johnDupont,
              contactType = "S",
              relationshipType = "BRO",
              whenCreated = LocalDateTime.parse("2023-01-01T10:00"),
            )
            contact(
              person = johnDupont,
              contactType = "O",
              relationshipType = "OFS",
              whenCreated = LocalDateTime.parse("2021-01-01T10:00"),
            )
            contact(
              person = janeDoe,
              contactType = "O",
              relationshipType = "POL",
            )
            val visitOrder = visitOrder(
              orderNumber = 654321,
            )
            officialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
              overrideBanStaff = staff,
              comment = "Next tuesday",
              visitorConcern = "Big concerns",
              prisonerSearchTypeCode = "FULL",
              visitOrder = visitOrder,
            ) {
              visitOutcome(outcomeReasonCode = "ADMIN", eventOutcomeCode = "ABS", eventStatusCode = "CANC")
              visitor(
                person = johnDupont,
                eventOutcomeCode = "ABS",
                outcomeReasonCode = "OFFCANC",
                eventStatusCode = "CANC",
              )
              janeDoeVisitorId = visitor(
                person = janeDoe,
                groupLeader = true,
                assistedVisit = true,
                eventStatusCode = "COMP",
                eventOutcomeCode = "ATT",
                outcomeReasonCode = null,
                comment = "First time visit",
              ).id
            }.id
            officialVisitReferenceDataIssue1Id = officialVisit(
              visitSlot = visitSlot,
              visitStatusCode = "CANC",
            ) {
              visitOutcome(outcomeReasonCode = "ADMIN_CANCEL")
              visitor(person = johnDupont)
            }.id
            officialVisitReferenceDataIssue2Id = officialVisit(
              visitSlot = visitSlot,
              visitStatusCode = "CANC",
            ) {
              visitOutcome(outcomeReasonCode = "BATCH_CANC")
              visitor(person = johnDupont)
            }.id
            socialVisitId = visit(visitTypeCode = "SCON").id
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      visitOrderRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(room)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 bad request if visit is a social visit`() {
        webTestClient.get().uri("/official-visits/{visitId}", socialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return 404 if visit does not exist`() {
        webTestClient.get().uri("/official-visits/{visitId}", 9999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the visit prisoner data`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.offenderNo).isEqualTo("A1234TT")
        assertThat(visit.bookingId).isEqualTo(bookingId)
        assertThat(visit.currentTerm).isTrue
      }

      @Test
      fun `will return the visit location data`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.visitSlotId).isEqualTo(visitSlot.id)
        assertThat(visit.prisonId).isEqualTo("BXI")
        assertThat(visit.internalLocationId).isEqualTo(room.locationId)
      }

      @Test
      fun `will return the visit status data`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.visitStatus.description).isEqualTo("Scheduled")
        assertThat(visit.cancellationReason?.description).isEqualTo("Administrative Cancellation")
        assertThat(visit.visitOutcome?.description).isEqualTo("Cancelled")
        assertThat(visit.prisonerAttendanceOutcome?.description).isEqualTo("Absence")
      }

      @Test
      fun `will return the visit status data for statuses with missing reference data`() {
        val visitAdminCancel: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitReferenceDataIssue1Id)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visitAdminCancel.cancellationReason?.code).isEqualTo("ADMIN_CANCEL")
        assertThat(visitAdminCancel.cancellationReason?.description).isEqualTo("ADMIN_CANCEL")

        val visitBatchCancel: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitReferenceDataIssue2Id)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visitBatchCancel.cancellationReason?.code).isEqualTo("BATCH_CANC")
        assertThat(visitBatchCancel.cancellationReason?.description).isEqualTo("BATCH_CANC")
      }

      @Test
      fun `will return the date and times`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T10:00"))
        assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T11:00"))
      }

      @Test
      fun `will return attributes of the visit`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.commentText).isEqualTo("Next tuesday")
        assertThat(visit.visitorConcernText).isEqualTo("Big concerns")
        assertThat(visit.overrideBanStaffUsername).isEqualTo("T_SMITH_GEN")
        assertThat(visit.prisonerSearchType?.description).isEqualTo("Full Search")
        assertThat(visit.visitOrder?.number).isEqualTo(654321)
      }

      @Test
      fun `will return just the visitors excluding the prisoner`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(visit.visitors).hasSize(2)
      }

      @Test
      fun `will return key information about each visitor`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        val janeDoeVisitor = visit.visitors.find { it.personId == janeDoe.id }!!
        val johnDupontVisitor = visit.visitors.find { it.personId == johnDupont.id }!!

        assertThat(janeDoeVisitor.id).isEqualTo(janeDoeVisitorId)
        assertThat(janeDoeVisitor.personId).isEqualTo(janeDoe.id)
        assertThat(janeDoeVisitor.firstName).isEqualTo("JANE")
        assertThat(janeDoeVisitor.lastName).isEqualTo("DOE")
        assertThat(janeDoeVisitor.dateOfBirth).isEqualTo(LocalDate.parse("1965-07-19"))
        assertThat(janeDoeVisitor.leadVisitor).isTrue
        assertThat(johnDupontVisitor.leadVisitor).isFalse
        assertThat(janeDoeVisitor.assistedVisit).isTrue
        assertThat(johnDupontVisitor.assistedVisit).isFalse
        assertThat(janeDoeVisitor.commentText).isEqualTo("First time visit")
      }

      @Test
      fun `will return status about each visitor`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        val janeDoeVisitor = visit.visitors.find { it.personId == janeDoe.id }!!
        val johnDupontVisitor = visit.visitors.find { it.personId == johnDupont.id }!!

        assertThat(janeDoeVisitor.eventStatus?.description).isEqualTo("Completed")
        assertThat(janeDoeVisitor.cancellationReason?.description).isNull()
        assertThat(janeDoeVisitor.visitorAttendanceOutcome?.description).isEqualTo("Attended")

        assertThat(johnDupontVisitor.eventStatus?.description).isEqualTo("Cancelled")
        assertThat(johnDupontVisitor.cancellationReason?.description).isEqualTo("Offender Cancelled")
        assertThat(johnDupontVisitor.visitorAttendanceOutcome?.description).isEqualTo("Absence")
      }

      @Test
      fun `will return ordered list of relationships with most relevant first`() {
        val visit: OfficialVisitResponse = webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        val johnDupontVisitor = visit.visitors.find { it.personId == johnDupont.id }!!

        assertThat(johnDupontVisitor.relationships).hasSize(3)
        assertThat(johnDupontVisitor.relationships[0].relationshipType.description).isEqualTo("Offender Supervisor")
        assertThat(johnDupontVisitor.relationships[0].contactType.description).isEqualTo("Official")
        assertThat(johnDupontVisitor.relationships[2].relationshipType.description).isEqualTo("Brother")
        assertThat(johnDupontVisitor.relationships[2].contactType.description).isEqualTo("Social/Family")
      }
    }
  }

  @DisplayName("GET /prisoner/{offenderNo}/official-visits")
  @Nested
  inner class GetOfficialVisitsForPrisoner {

    lateinit var visitSlot: AgencyVisitSlot
    lateinit var room: AgencyInternalLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        room = agencyInternalLocation(
          locationCode = "BXI-VISIT-2",
          locationType = "VISIT",
          prisonId = "BXI",
        )
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = room,
              maxGroups = null,
              maxAdults = null,
            )
          }
        }

        val visitor = person { }

        offender(nomsId = "A1234TT") {
          booking {
            contact(person = visitor)
            officialVisit(
              visitDate = LocalDate.parse("2024-12-31"),
              visitSlot = visitSlot,
            )
            officialVisit(
              visitDate = LocalDate.parse("2025-01-01"),
              visitSlot = visitSlot,
            )
            officialVisit(
              visitDate = LocalDate.parse("2025-01-02"),
              visitSlot = visitSlot,
            )
            visit(visitTypeCode = "SCON", startDateTimeString = "2025-01-01T10:00")
          }
          booking {
            release(date = LocalDateTime.parse("2024-01-01T10:00"))
            contact(person = visitor)
            officialVisit(
              visitDate = LocalDate.parse("2025-01-01"),
              visitSlot = visitSlot,
            )
          }
        }
        offender(nomsId = "A4321TT") {
          booking {
            contact(person = visitor)
            officialVisit(
              visitDate = LocalDate.parse("2025-01-01"),
              visitSlot = visitSlot,
            )
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      visitOrderRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(room)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoner/A1234TT/official-visits")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoner/A1234TT/official-visits")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoner/A1234TT/official-visits")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return all visits for prisoner including old bookings `() {
        webTestClient.get().uri("/prisoner/A1234TT/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(4)
      }

      @Test
      fun `can filter by from date`() {
        webTestClient.get().uri {
          it.path("/prisoner/A1234TT/official-visits")
            .queryParam("fromDate", "2025-01-02")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
      }

      @Test
      fun `can filter by to date`() {
        webTestClient.get().uri {
          it.path("/prisoner/A1234TT/official-visits")
            .queryParam("toDate", "2025-01-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(3)
      }

      @Test
      fun `can filter by from and to dates`() {
        webTestClient.get().uri {
          it.path("/prisoner/A1234TT/official-visits")
            .queryParam("fromDate", "2025-01-01")
            .queryParam("toDate", "2025-01-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
      }
    }
  }

  @Nested
  @DisplayName("PUT /official-visits/{visitId}")
  inner class UpdateOfficialVisit {
    private var officialVisitId: Long = 0
    lateinit var visitHall: AgencyInternalLocation
    lateinit var closedVisitHall: AgencyInternalLocation
    lateinit var visitSlot1: AgencyVisitSlot
    lateinit var visitSlot2: AgencyVisitSlot
    val prisonId = "BXI"
    private lateinit var personContact: Person
    private lateinit var contact: OffenderContactPerson
    private val offenderNo = "A1234KT"

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }
        staff(firstName = "JIM", lastName = "BOB") {
          account(username = "JIMBOB_GEN", type = GENERAL)
        }

        visitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-1",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        closedVisitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-2",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        agencyVisitDay(prisonerId = prisonId, weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot1 = visitSlot(
              agencyInternalLocation = visitHall,
              maxGroups = 10,
              maxAdults = 20,
            )
            visitSlot2 = visitSlot(
              agencyInternalLocation = closedVisitHall,
              maxGroups = 10,
              maxAdults = 20,
            )
          }
        }

        personContact = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          booking {
            contact = contact(
              person = personContact,
              contactType = "O",
              relationshipType = "DR",
            )
            officialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot1,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )

              visitor(
                person = personContact,
                groupLeader = true,
                assistedVisit = true,
                eventOutcomeCode = "ATT",
                comment = "First visit",
              ).id
            }.id
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(visitHall)
      agencyInternalLocationRepository.delete(closedVisitHall)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(
            UpdateOfficialVisitRequest(
              visitSlotId = visitSlot2.id,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = closedVisitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .bodyValue(
            UpdateOfficialVisitRequest(
              visitSlotId = visitSlot2.id,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = closedVisitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/official-visits/{visitId}", officialVisitId)
          .bodyValue(
            UpdateOfficialVisitRequest(
              visitSlotId = visitSlot2.id,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = closedVisitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `when visit not found`() {
        webTestClient.put().uri("/official-visits/{visitId}", 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitRequest(
              visitSlotId = visitSlot2.id,
              startDateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-01T11:00:00"),
              internalLocationId = closedVisitHall.locationId,
              visitStatusCode = "SCH",
            ),
          )
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update visit`() {
        nomisDataBuilder.runInTransaction {
          with(visitRepository.findByIdOrNull(officialVisitId)!!) {
            assertThat(id).isEqualTo(officialVisitId)
            assertThat(agencyVisitSlot).isEqualTo(visitSlot1)
            assertThat(location.id).isEqualTo(prisonId)
            assertThat(agencyInternalLocation!!.locationCode).isEqualTo("BXI-VISIT-1")
            assertThat(startDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T10:00:00"))
            assertThat(endDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T11:00:00"))
            assertThat(visitDate).isEqualTo(LocalDate.parse("2023-01-01"))
            assertThat(visitStatus.description).isEqualTo("Scheduled")
            assertThat(visitType.description).isEqualTo("Official Visit")
            assertThat(commentText).isNull()
            assertThat(visitorConcernText).isNull()
            assertThat(searchLevel).isNull()
            assertThat(overrideBanStaff).isNull()
            assertThat(visitors.filter { it.person != null }).hasSize(1)
            assertThat(visitors.filter { it.offenderBooking != null }).hasSize(1)
            with(visitors.first { it.offenderBooking != null }) {
              assertThat(eventOutcome!!.description).isEqualTo("Attended")
              assertThat(eventStatus!!.description).isEqualTo("Scheduled (Approved)")
              assertThat(outcomeReason).isNull()
            }
          }
        }

        webTestClient.put().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitRequest(
              visitSlotId = visitSlot2.id,
              startDateTime = LocalDateTime.parse("2024-01-07T10:00:00"),
              endDateTime = LocalDateTime.parse("2024-01-07T11:00:00"),
              internalLocationId = closedVisitHall.locationId,
              visitStatusCode = "CANC",
              visitOutcomeCode = "HMP",
              prisonerAttendanceCode = "ABS",
              prisonerSearchTypeCode = "RUB_A",
              visitorConcernText = "No concerns",
              commentText = "Cancelled due to issues",
              overrideBanStaffUsername = "JIMBOB_GEN",
            ),
          )
          .exchange()
          .expectStatus().isNoContent

        nomisDataBuilder.runInTransaction {
          with(visitRepository.findByIdOrNull(officialVisitId)!!) {
            assertThat(id).isEqualTo(officialVisitId)
            assertThat(agencyVisitSlot).isEqualTo(visitSlot2)
            assertThat(location.id).isEqualTo(prisonId)
            assertThat(agencyInternalLocation!!.locationCode).isEqualTo("BXI-VISIT-2")
            assertThat(startDateTime).isEqualTo(LocalDateTime.parse("2024-01-07T10:00:00"))
            assertThat(endDateTime).isEqualTo(LocalDateTime.parse("2024-01-07T11:00:00"))
            assertThat(visitDate).isEqualTo(LocalDate.parse("2024-01-07"))
            assertThat(visitStatus.description).isEqualTo("Cancelled")
            assertThat(visitType.description).isEqualTo("Official Visit")
            assertThat(commentText).isEqualTo("Cancelled due to issues")
            assertThat(visitorConcernText).isEqualTo("No concerns")
            assertThat(searchLevel!!.description).isEqualTo("Rubdown Level A")
            assertThat(overrideBanStaff!!.firstName).isEqualTo("JIM")
            assertThat(visitors.filter { it.person != null }).hasSize(1)
            assertThat(visitors.filter { it.offenderBooking != null }).hasSize(1)
            with(visitors.first { it.offenderBooking != null }) {
              assertThat(eventOutcome!!.description).isEqualTo("Absence")
              assertThat(eventStatus!!.description).isEqualTo("Cancelled")
              assertThat(outcomeReason!!.description).isEqualTo("Operational Reasons-All Visits Cancelled")
            }
          }
        }
      }
    }
  }

  @DisplayName("POST /official-visits/{visitId}/official-visitor")
  @Nested
  inner class CreateOfficialVisitor {
    lateinit var visitHall: AgencyInternalLocation
    lateinit var visitSlot: AgencyVisitSlot
    val prisonId = "BXI"
    private lateinit var personContact: Person
    private lateinit var contact: OffenderContactPerson
    private var latestBookingId = 0L
    private val offenderNo = "A1234KT"
    private var officialVisitId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }

        visitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-1",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        agencyVisitDay(prisonerId = prisonId, weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = visitHall,
              maxGroups = 10,
              maxAdults = 20,
            )
          }
        }

        personContact = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            contact = contact(
              person = personContact,
              contactType = "O",
              relationshipType = "DR",
            )
            officialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )
            }.id
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(visitHall)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = personContact.id,
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = personContact.id,
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = personContact.id,
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `when visit not found`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = personContact.id,
            ),
          )
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `when person is not valid`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = 9999,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Person with id 9999 not found")
      }

      @Test
      fun `when attendance status is not valid`() {
        webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            CreateOfficialVisitorRequest(
              personId = personContact.id,
              visitorAttendanceOutcomeCode = "ZZZ",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Event Outcome code ZZZ does not exist")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create visitor`() {
        val createdVisitor: OfficialVisitResponse.OfficialVisitor =
          webTestClient.post().uri("/official-visits/{visitId}/official-visitor", officialVisitId)
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .bodyValue(
              CreateOfficialVisitorRequest(
                personId = personContact.id,
                visitorAttendanceOutcomeCode = "ATT",
                assistedVisit = true,
                commentText = "First visit",
              ),
            )
            .exchange()
            .expectBodyResponse()

        assertThat(createdVisitor.id).isNotNull()
        nomisDataBuilder.runInTransaction {
          with(visitVisitorRepository.findByIdOrNull(createdVisitor.id)!!) {
            assertThat(this.person!!.id).isEqualTo(personContact.id)
            assertThat(this.offenderBooking).isNull()
            assertThat(this.visit).isNotNull
            assertThat(this.groupLeader).isFalse
            assertThat(this.assistedVisit).isTrue
            assertThat(this.commentText).isEqualTo("First visit")
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("PUT /official-visits/{visitId}/official-visitor/{visitorId}")
  inner class UpdateOfficialVisitor {
    private var visitorId: Long = 0
    private var officialVisitId: Long = 0
    private var anotherOfficialVisitId: Long = 0
    lateinit var visitHall: AgencyInternalLocation
    lateinit var visitSlot: AgencyVisitSlot
    val prisonId = "BXI"
    private lateinit var personContact: Person
    private lateinit var contact: OffenderContactPerson
    private var latestBookingId = 0L
    private val offenderNo = "A1234KT"

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }

        visitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-1",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        agencyVisitDay(prisonerId = prisonId, weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = visitHall,
              maxGroups = 10,
              maxAdults = 20,
            )
          }
        }

        personContact = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            contact = contact(
              person = personContact,
              contactType = "O",
              relationshipType = "DR",
            )
            officialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )

              visitorId = visitor(
                person = personContact,
                groupLeader = true,
                assistedVisit = true,
                eventOutcomeCode = "ATT",
                comment = "First visit",
              ).id
            }.id
            anotherOfficialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )
              visitor(
                person = personContact,
              )
            }.id
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(visitHall)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `when visit not found`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", 999, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `when visitor not found`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, 9999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `when visitor does not belong to visit`() {
        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", anotherOfficialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = true,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Visitor with id $visitorId does not belong to visit with id $anotherOfficialVisitId")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update visitor`() {
        with(visitVisitorRepository.findByIdOrNull(visitorId)!!) {
          assertThat(assistedVisit).isTrue
          assertThat(groupLeader).isTrue
          assertThat(eventOutcome?.code).isEqualTo("ATT")
          assertThat(commentText).isEqualTo("First visit")
        }

        webTestClient.put().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            UpdateOfficialVisitorRequest(
              leadVisitor = false,
              assistedVisit = false,
              visitorAttendanceOutcomeCode = "ABS",
              commentText = "Second visit",
            ),
          )
          .exchange()
          .expectStatus().isNoContent

        with(visitVisitorRepository.findByIdOrNull(visitorId)!!) {
          assertThat(assistedVisit).isFalse
          assertThat(groupLeader).isFalse
          assertThat(eventOutcome?.code).isEqualTo("ABS")
          assertThat(commentText).isEqualTo("Second visit")
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /official-visits/{visitId}/official-visitor/{visitorId}")
  inner class DeleteOfficialVisitor {
    private var visitorId: Long = 0
    private var officialVisitId: Long = 0
    private var anotherOfficialVisitId: Long = 0
    lateinit var visitHall: AgencyInternalLocation
    lateinit var visitSlot: AgencyVisitSlot
    val prisonId = "BXI"
    private lateinit var personContact: Person
    private lateinit var contact: OffenderContactPerson
    private var latestBookingId = 0L
    private val offenderNo = "A1234KT"

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }

        visitHall = agencyInternalLocation(
          locationCode = "$prisonId-VISIT-1",
          locationType = "VISIT",
          prisonId = prisonId,
        )
        agencyVisitDay(prisonerId = prisonId, weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = visitHall,
              maxGroups = 10,
              maxAdults = 20,
            )
          }
        }

        personContact = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            contact = contact(
              person = personContact,
              contactType = "O",
              relationshipType = "DR",
            )
            officialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )

              visitorId = visitor(
                person = personContact,
              ).id
            }.id
            anotherOfficialVisitId = officialVisit(
              visitDate = LocalDate.parse("2023-01-01"),
              visitSlot = visitSlot,
              visitStatusCode = "SCH",
            ) {
              visitOutcome(
                eventOutcomeCode = "ATT",
                eventStatusCode = "SCH",
                outcomeReasonCode = null,
              )
              visitor(
                person = personContact,
              )
            }.id
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(visitHall)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `when visit not found`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", 999, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `when visitor not found`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, 9999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `when visitor does not belong to visit`() {
        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", anotherOfficialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.developerMessage").isEqualTo("Visitor with id $visitorId does not belong to visit with id $anotherOfficialVisitId")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete visitor`() {
        assertThat(visitVisitorRepository.findByIdOrNull(visitorId)).isNotNull()

        webTestClient.delete().uri("/official-visits/{visitId}/official-visitor/{visitorId}", officialVisitId, visitorId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(visitVisitorRepository.findByIdOrNull(visitorId)).isNull()
      }
    }
  }
}

private data class VisitIdPageResponse(
  val content: List<VisitIdResponse>,
)
