package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.GENERAL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OfficialVisitsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

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
}

private data class VisitIdPageResponse(
  val content: List<VisitIdResponse>,
)
