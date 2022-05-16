package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderContactBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonAddressBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.VisitBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.VisitVisitorBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val prisonId = "BXI"

private val createVisit: (visitorPersonIds: List<Long>) -> CreateVisitRequest =
  { visitorPersonIds ->
    CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = prisonId,
      visitorPersonIds = visitorPersonIds,
      issueDate = LocalDate.parse("2021-11-02"),
    )
  }

class VisitResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var repository: Repository

  lateinit var offenderNo: String
  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtLeeds: Offender
  lateinit var offenderAtBrixton: Offender
  private var offenderBookingId: Long = 0L
  private val threePeople = mutableListOf<Person>()
  private val createVisitWithPeople: () -> CreateVisitRequest = { createVisit(threePeople.map { it.id }) }

  internal fun createPrisoner() {
    threePeople.addAll(
      (1..3).map {
        repository.save(PersonBuilder())
      }
    )
    offenderAtMoorlands = repository.save(
      OffenderBuilder()
        .withBooking(
          OffenderBookingBuilder()
            .withVisitBalance()
            .withContacts(*threePeople.map { OffenderContactBuilder(it) }.toTypedArray())
        )
    )

    offenderNo = offenderAtMoorlands.nomsId
    offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
  }

  @DisplayName("Create")
  @Nested
  inner class CreateVisitRequestTest {
    @BeforeEach
    internal fun setUpData() {
      createPrisoner()
    }

    @AfterEach
    internal fun deleteData() {
      repository.delete(offenderAtMoorlands)
      repository.delete(threePeople)
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .body(BodyInserters.fromValue(createVisitWithPeople()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createVisitWithPeople()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createVisitWithPeople()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit with offender not found`() {
      webTestClient.post().uri("/prisoners/Z9999ZZ/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createVisitWithPeople()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `create visit with invalid person`() {
      val error = webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createVisitWithPeople().copy(visitorPersonIds = listOf(-7L, -99L))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(error?.userMessage).isEqualTo("Bad request: Person with id=-99 does not exist")
    }

    @Test
    fun `create visit with invalid offenderNo`() {
      webTestClient.post().uri("/prisoners/ZZ000ZZ/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createVisitWithPeople()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
    }

    @Test
    fun `create visit success`() {
      val personIds: String = threePeople.map { it.id }.joinToString(",")
      val response = webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "visitType"         : "SCON",
            "startDateTime"     : "2021-11-04T12:05",
            "endTime"           : "13:04",
            "prisonId"          : "$prisonId",
            "visitorPersonIds"  : [$personIds],
            "visitRoomId"       : "VISIT",
            "issueDate"         : "2021-11-02"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      // Spot check that the database has been populated.
      val visit = repository.lookupVisit(response?.visitId)

      assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
      assertThat(visit.offenderBooking.bookingId).isEqualTo(offenderBookingId)
      assertThat(visit.visitors).extracting("person.id", "eventStatus.code").containsExactly(
        Tuple.tuple(null, "SCH"),
        Tuple.tuple(threePeople[0].id, "SCH"),
        Tuple.tuple(threePeople[1].id, "SCH"),
        Tuple.tuple(threePeople[2].id, "SCH"),
      )
      assertThat(visit.visitors[0].eventId).isGreaterThan(0)

      val balanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll()

      assertThat(balanceAdjustment).extracting("offenderBooking.bookingId", "remainingPrivilegedVisitOrders")
        .containsExactly(
          Tuple.tuple(offenderBookingId, -1),
        )
    }
  }

  @DisplayName("Cancel")
  @Nested
  inner class CancelVisitRequest {
    @BeforeEach
    internal fun setUpData() {
      createPrisoner()
    }

    @AfterEach
    internal fun deleteData() {
      repository.delete(offenderAtMoorlands)
      repository.delete(threePeople)
    }

    @Test
    fun `cancel visit success`() {
      val visitId = createVisit()

      webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "outcome" : "VISCANC"
          }"""
          )
        )
        .exchange()
        .expectStatus().isOk
        .expectBody().isEmpty

      // Spot check that the database has been updated correctly.
      val visit = repository.lookupVisit(visitId)

      assertThat(visit.visitStatus.code).isEqualTo("CANC")
      assertThat(visit.offenderBooking.bookingId).isEqualTo(offenderBookingId)
      assertThat(visit.visitors).extracting("eventOutcome.code", "eventStatus.code", "outcomeReason.code")
        .containsExactly(
          Tuple.tuple("ABS", "CANC", "VISCANC"),
          Tuple.tuple("ABS", "CANC", "VISCANC"),
          Tuple.tuple("ABS", "CANC", "VISCANC"),
          Tuple.tuple("ABS", "CANC", "VISCANC"),
        )
      assertThat(visit.visitOrder?.status?.code).isEqualTo("CANC")
      assertThat(visit.visitOrder?.outcomeReason?.code).isEqualTo("VISCANC")
      assertThat(visit.visitOrder?.expiryDate).isEqualTo(LocalDate.now())

      val balanceAdjustments = offenderVisitBalanceAdjustmentRepository.findAll()

      assertThat(balanceAdjustments).extracting("offenderBooking.bookingId", "remainingPrivilegedVisitOrders")
        .containsExactly(
          Tuple.tuple(offenderBookingId, -1),
          Tuple.tuple(offenderBookingId, 1),
        )
    }

    @Test
    fun `cancel visit with visit id not found`() {
      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/9999/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue("""{ "outcome" : "VISCANC" }""")
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Not Found: Nomis visit id 9999 not found")
    }

    @Test
    fun `cancel visit with invalid cancellation reason`() {
      val visitId = createVisit()

      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{ "outcome" : "NOT-A-CANC-REASON" }"""))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Bad request: Invalid cancellation reason: NOT-A-CANC-REASON")
    }

    @Test
    fun `cancel already cancelled or other status`() {
      val visitId = createVisit()

      webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "outcome" : "VISCANC" }"""))
        .exchange()
        .expectStatus().isOk

      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{ "outcome" : "OFFCANC" }"""))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Visit already cancelled, with outcome VISCANC")

      repository.changeVisitStatus(visitId)

      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{ "outcome" : "VISCANC" }"""))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Visit status is not scheduled but is NORM")
    }

    @Test
    fun `Offender does not match`() {
      val visitId = createVisit()

      assertThat(
        webTestClient.put().uri("/prisoners/B1234BB/visits/$visitId/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{ "outcome" : "VISCANC" }"""))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Bad request: Visit's offenderNo = A5194DY does not match argument = B1234BB")
    }
  }

  private fun createVisit() = webTestClient.post().uri("/prisoners/$offenderNo/visits")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
    .contentType(MediaType.APPLICATION_JSON)
    .body(BodyInserters.fromValue(createVisitWithPeople()))
    .exchange()
    .expectStatus().isCreated
    .expectBody(CreateVisitResponse::class.java)
    .returnResult().responseBody?.visitId

  @DisplayName("Get Visit")
  @Nested
  inner class GetVisitRequest {
    @DisplayName("With no lead visitor and no outcome")
    @Nested
    inner class NoOutcomeNoLeadVisitor {

      @BeforeEach
      internal fun createPrisonerWithVisit() {
        val person1 = repository.save(PersonBuilder())
        val person2 = repository.save(PersonBuilder())
        offenderAtMoorlands = repository.save(
          OffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder().withVisitors(
                    VisitVisitorBuilder(person1, leadVisitor = false),
                    VisitVisitorBuilder(person2, leadVisitor = false)
                  )
                )
            )
        )

        offenderNo = offenderAtMoorlands.nomsId
        offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(offenderAtMoorlands)
      }

      @Test
      fun `get visit success`() {
        val visitId = offenderAtMoorlands.bookings[0].visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.visitStatus.code).isEqualTo("SCH")
        assertThat(visit.visitType.code).isEqualTo("SCON")
        assertThat(visit.visitOutcome).isNull()
        assertThat(visit.offenderNo).isEqualTo("A1234TT")
        assertThat(visit.prisonId).isEqualTo("MDI")
        assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T12:05"))
        assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T13:05"))
        assertThat(visit.leadVisitor).isNull()
        assertThat(visit.visitors).extracting("leadVisitor")
          .containsExactly(
            false, false
          )
      }

      @Test
      fun `get visit with visit id not found`() {
        assertThat(
          webTestClient.get().uri("/visits/123")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage
        ).isEqualTo("Not Found: visit id 123")
      }

      @Test
      fun `get visit prevents access without appropriate role`() {
        val visitId = offenderAtMoorlands.bookings[0].visits[0].id
        assertThat(
          webTestClient.get().uri("/visits/$visitId")
            .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
            .exchange()
            .expectStatus().isForbidden
        )
      }

      @Test
      fun `get visit prevents access without authorization`() {
        val visitId = offenderAtMoorlands.bookings[0].visits[0].id
        assertThat(
          webTestClient.get().uri("/visits/$visitId")
            .exchange()
            .expectStatus().isUnauthorized
        )
      }
    }

    @DisplayName("With a lead visitor with multiple telephone numbers")
    @Nested
    inner class WithLeadVisitorMultipleTelephoneNumbers {

      @BeforeEach
      internal fun createPrisonerWithVisit() {
        val leadVisitor = repository.save(
          PersonBuilder(
            firstName = "Manon",
            lastName = "Dupont",
            phoneNumbers = listOf("HOME" to "01145551234", "MOB" to "07973555123"),
            addressBuilders = listOf(
              PersonAddressBuilder(phoneNumbers = listOf("HOME" to "01145559999"))
            )
          )
        )

        offenderAtMoorlands = repository.save(
          OffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder().withVisitors(
                    VisitVisitorBuilder(leadVisitor, leadVisitor = true)
                  )
                )
            )
        )

        offenderNo = offenderAtMoorlands.nomsId
        offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(offenderAtMoorlands)
      }

      @Test
      fun `get visit success`() {
        val visitId = offenderAtMoorlands.bookings[0].visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.visitStatus.code).isEqualTo("SCH")
        assertThat(visit.visitType.code).isEqualTo("SCON")
        assertThat(visit.visitOutcome).isNull()
        assertThat(visit.offenderNo).isEqualTo("A1234TT")
        assertThat(visit.prisonId).isEqualTo("MDI")
        assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T12:05"))
        assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T13:05"))
        assertThat(visit.leadVisitor).isNotNull
        assertThat(visit.leadVisitor?.fullName).isEqualTo("Manon Dupont")
        assertThat(visit.leadVisitor?.telephones).containsExactly("01145551234", "07973555123", "01145559999")
        assertThat(visit.visitors).extracting("leadVisitor")
          .containsExactly(
            true
          )
      }
    }
  }

  @DisplayName("filter Visits")
  @Nested
  inner class GetVisitIdsByFilterRequest {
    @BeforeEach
    internal fun createPrisonerWithVisits() {
      val person1 = repository.save(PersonBuilder())
      val person2 = repository.save(PersonBuilder())
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = "2022-01-01T09:00",
                  endDateTimeString = "2022-01-01T10:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                  VisitVisitorBuilder(person2, leadVisitor = true)
                ),
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = "2022-01-01T12:00",
                  endDateTimeString = "2022-01-01T13:00",
                  agencyInternalLocationDescription = null, // no room specified
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
              )
          )
      )
      offenderAtLeeds = repository.save(
        OffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "LEI", startDateTimeString = "2022-01-02T09:00",
                  endDateTimeString = "2022-01-02T10:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "LEI", startDateTimeString = "2022-01-02T14:00",
                  endDateTimeString = "2022-01-02T15:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
              )
          )
      )
      offenderAtBrixton = repository.save(
        OffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2022-01-01T09:00",
                  endDateTimeString = "2022-01-01T10:00",
                  visitTypeCode = "OFFI"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-01-01T09:00",
                  endDateTimeString = "2023-01-01T10:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-02-01T09:00",
                  endDateTimeString = "2023-02-01T10:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-03-01T09:00",
                  endDateTimeString = "2023-03-01T10:00"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
              )
          )
      )
      repository.updateCreatedToMatchVisitStart() // hack to allow easier testing of date ranges (CREATED is not updateable via JPA)
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
      repository.delete(offenderAtLeeds)
      repository.delete(offenderAtBrixton)
    }

    @Test
    fun `get all visit ids - no filter specified`() {
      webTestClient.get().uri("/visits/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(8)
    }

    @Test
    fun `get visit ids by prisons - Leeds`() {
      val leedsVisitIds = offenderAtLeeds.latestBooking().visits.map { it.id.toInt() }.toTypedArray()
      webTestClient.get().uri("/visits/ids?prisonIds=LEI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(2)
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            *leedsVisitIds
          )
        )
    }

    @Test
    fun `get visit by prison ID - Leeds and Brixton`() {
      val brixtonVisitIds = offenderAtBrixton.latestBooking().visits.map { it.id.toInt() }.toTypedArray()
      val leedsVisitIds = offenderAtLeeds.latestBooking().visits.map { it.id.toInt() }.toTypedArray()
      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("prisonIds", "LEI")
          .queryParam("prisonIds", "BXI")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            *brixtonVisitIds + leedsVisitIds
          )
        )
    }

    @Test
    fun `get visit ids by visit type`() {
      webTestClient.get().uri("/visits/ids?visitTypes=OFFI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            offenderAtBrixton.latestBooking().visits[0].id.toInt()
          )
        )
    }

    @Test
    fun `get visit ids excluding visits without a room`() {
      webTestClient.get().uri("/visits/ids?ignoreMissingRoom=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(7)
        .jsonPath("$.content..visitId").value(
          Matchers.not(
            Matchers.hasItem(
              offenderAtMoorlands.latestBooking().visits[1].id.toInt()
            )
          )
        )
    }

    @Test
    fun `get visit ids including visits without a room`() {
      webTestClient.get().uri("/visits/ids?ignoreMissingRoom=false")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(8)
    }

    @Test
    fun `get visits starting within a given date and time range`() {

      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("fromDateTime", "2022-01-01T09:00:00")
          .queryParam("toDateTime", "2022-01-01T09:30:00")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            offenderAtBrixton.latestBooking().visits[0].id.toInt(),
            offenderAtMoorlands.latestBooking().visits[0].id.toInt()
          )
        )
    }

    @Test
    fun `get visits starting after a given date and time`() {

      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("fromDateTime", "2022-10-01T09:00:00")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            offenderAtBrixton.latestBooking().visits[1].id.toInt(),
            offenderAtBrixton.latestBooking().visits[2].id.toInt(),
            offenderAtBrixton.latestBooking().visits[3].id.toInt()
          )
        )
    }

    @Test
    fun `get visits for a prison, in a date range and of type`() {

      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("prisonIds", "MDI")
          .queryParam("visitTypes", "SCON")
          .queryParam("fromDateTime", "2022-01-01T09:00:00")
          .queryParam("toDateTime", "2022-01-01T09:30:00")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..visitId").value(
          Matchers.contains(
            offenderAtMoorlands.latestBooking().visits[0].id.toInt()
          )
        )
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("size", "2")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(8)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(4)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("size", "2")
          .queryParam("page", "3")
          .queryParam("sort", "location.id,startDateTime,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(8)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(3)
        .jsonPath("totalPages").isEqualTo(4)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `malformed date returns bad request`() {

      webTestClient.get().uri {
        it.path("/visits/ids")
          .queryParam("fromDateTime", "202-10-01T09:00:00")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get visit prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/visits/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden
      )
    }

    @Test
    fun `get visit prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/visits/ids")
          .exchange()
          .expectStatus().isUnauthorized
      )
    }
  }

  @DisplayName("filter Visit room usage count")
  @Nested
  inner class GetVisitRoomCountByFilterRequest {
    @BeforeEach
    internal fun createPrisonerWithVisits() {
      val person1 = repository.save(PersonBuilder())
      val person2 = repository.save(PersonBuilder())
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = "2022-01-02T11:00",
                  endDateTimeString = "2022-01-02T12:00",
                  agencyInternalLocationDescription = "MDI-1-1-001",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                  VisitVisitorBuilder(person2, leadVisitor = true)
                ),
              )
          )
      )
      offenderAtLeeds = repository.save(
        OffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "LEI", startDateTimeString = "2022-01-02T09:00",
                  endDateTimeString = "2022-01-02T10:00",
                  agencyInternalLocationDescription = null // ignored in results
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
              )
          )
      )
      offenderAtBrixton = repository.save(
        OffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2022-01-01T09:00",
                  endDateTimeString = "2022-01-01T10:00",
                  visitTypeCode = "OFFI",
                  agencyInternalLocationDescription = "BXI-VISIT"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-01-01T09:00",
                  endDateTimeString = "2023-01-01T10:00",
                  agencyInternalLocationDescription = "BXI-VISIT"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-02-01T09:00",
                  endDateTimeString = "2023-02-01T10:00",
                  agencyInternalLocationDescription = "BXI-VISIT2"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-03-01T09:00",
                  endDateTimeString = "2023-03-01T10:00",
                  agencyInternalLocationDescription = "BXI-VISIT2"
                ).withVisitors(
                  VisitVisitorBuilder(person1)
                ),
              )
          )
      )
      repository.updateCreatedToMatchVisitStart() // hack to allow easier testing of date ranges (CREATED is not updateable via JPA)
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
      repository.delete(offenderAtLeeds)
      repository.delete(offenderAtBrixton)
    }

    @Test
    fun `get room usage count for all visit rooms - no filter specified`() {
      webTestClient.get().uri("/visits/rooms/usage-count")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(3)
    }

    @Test
    fun `get visit rooms usage filtered by prison, date and visit type`() {
      webTestClient.get().uri("/visits/rooms/usage-count?prisonIds=BXI&prisonIds=MDI&fromDateTime=2022-01-02T10:00:00&visitTypes=SCON")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(3)
        .jsonPath("$[0].agencyInternalLocationDescription").isEqualTo("BXI-VISIT")
        .jsonPath("$[0].count").isEqualTo(1)
        .jsonPath("$[0].prisonId").isEqualTo("BXI")
        .jsonPath("$[1].agencyInternalLocationDescription").isEqualTo("BXI-VISIT2")
        .jsonPath("$[1].count").isEqualTo(2)
        .jsonPath("$[1].prisonId").isEqualTo("BXI")
        .jsonPath("$[2].agencyInternalLocationDescription").isEqualTo("MDI-1-1-001")
        .jsonPath("$[2].count").isEqualTo(1)
        .jsonPath("$[2].prisonId").isEqualTo("MDI")
    }

    @Test
    fun `malformed date returns bad request`() {

      webTestClient.get().uri {
        it.path("/visits/rooms/usage-count")
          .queryParam("fromDateTime", "202-10-01T09:00:00")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get visit rooms usage prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/visits/rooms/usage-count")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden
      )
    }

    @Test
    fun `get visit rooms usage prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/visits/rooms/usage-count")
          .exchange()
          .expectStatus().isUnauthorized
      )
    }
  }
}

private fun Offender.latestBooking(): OffenderBooking =
  this.bookings.firstOrNull { it.active } ?: throw IllegalStateException("Offender has no active bookings")
