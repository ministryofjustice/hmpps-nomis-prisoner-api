package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.LegacyOffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderContactBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonAddressBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.VisitBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.VisitVisitorBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.VisitResponse.Visitor
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val PRISON_ID = "BXI"

private val createVisit: (visitorPersonIds: List<Long>) -> CreateVisitRequest =
  { visitorPersonIds ->
    CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = PRISON_ID,
      visitorPersonIds = visitorPersonIds,
      issueDate = LocalDate.parse("2021-11-02"),
      room = "Main visit room",
      openClosedStatus = "OPEN",
    )
  }
private val updateVisit: (visitorPersonIds: List<Long>) -> UpdateVisitRequest =
  { visitorPersonIds ->
    UpdateVisitRequest(
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      visitorPersonIds = visitorPersonIds,
      room = "Main visit room",
      openClosedStatus = "OPEN",
    )
  }

class VisitResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var repository: Repository

  lateinit var offenderNo: String
  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtLeeds: Offender
  lateinit var offenderAtBrixton: Offender
  private var offenderBookingId: Long = 0L
  private val threePeople = mutableListOf<Person>()
  private val createVisitWithPeople: () -> CreateVisitRequest = { createVisit(threePeople.map { it.id }) }
  private val updateVisitWithPeople: () -> UpdateVisitRequest = { updateVisit(threePeople.map { it.id }) }

  internal fun createPrisoner() {
    threePeople.addAll(
      (1..3).map {
        repository.save(PersonBuilder())
      },
    )
    offenderAtMoorlands = repository.save(
      LegacyOffenderBuilder()
        .withBooking(
          OffenderBookingBuilder()
            .withVisitBalance()
            .withContacts(*threePeople.map { OffenderContactBuilder(it) }.toTypedArray()),
        ),
    )

    offenderNo = offenderAtMoorlands.nomsId
    offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
  }

  @DisplayName("Create")
  @Nested
  inner class CreateVisit {
    @BeforeEach
    internal fun setUpData() {
      createPrisoner()
    }

    @AfterEach
    internal fun deleteData() {
      repository.deleteOffenders()
      repository.delete(threePeople)
      repository.deleteAllVisitSlots()
      repository.deleteAllVisitTimes()
      repository.deleteAllVisitDays()
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
    internal fun `will add visitors to visit and visit order`() {
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
            "prisonId"          : "$PRISON_ID",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody

      val visit = repository.getVisit(response?.visitId)

      assertThat(visit.visitors).extracting("person.id", "eventStatus.code").containsExactly(
        tuple(null, "SCH"),
        tuple(threePeople[0].id, "SCH"),
        tuple(threePeople[1].id, "SCH"),
        tuple(threePeople[2].id, "SCH"),
      )
      assertThat(visit.visitors[0].eventId).isGreaterThan(0)

      assertThat(visit.visitOrder?.visitors).extracting("person.id", "groupLeader").containsExactly(
        tuple(threePeople[0].id, true),
        tuple(threePeople[1].id, false),
        tuple(threePeople[2].id, false),
      )
    }

    @Test
    internal fun `will create visit with correct visit details`() {
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
            "prisonId"          : "$PRISON_ID",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      // Spot check that the database has been populated.
      val visit = repository.getVisit(response?.visitId)

      assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
      assertThat(visit.offenderBooking.bookingId).isEqualTo(offenderBookingId)
      assertThat(visit.commentText).isEqualTo("VSIP Ref: asd-fff-ddd")
    }

    @Test
    internal fun `will create visit with visit order and balance adjustment`() {
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
            "prisonId"          : "$PRISON_ID",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      // Spot check that the database has been populated.
      val visit = repository.getVisit(response?.visitId)

      assertThat(visit.visitOrder).isNotNull
      assertThat(visit.visitOrder?.commentText).isEqualTo("VSIP Order Ref: asd-fff-ddd")
      // lead visitor assigned randomly to first visitor in list to create a valid order

      val balanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll()

      assertThat(balanceAdjustment).extracting("offenderBooking.bookingId", "remainingPrivilegedVisitOrders")
        .containsExactly(
          tuple(offenderBookingId, -1),
        )
    }

    @Test
    internal fun `will create visit with the correct slot`() {
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
            "prisonId"          : "$PRISON_ID",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      repository.runInTransaction {
        // Spot check that the database has been populated.
        val visit = repository.getVisit(response?.visitId)

        assertThat(visit.agencyVisitSlot).isNotNull
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.description).isEqualTo("$PRISON_ID-VISIT-VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.locationCode).isEqualTo("VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.timeSlotSequence).isEqualTo(1)
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.startTime).isEqualTo(LocalTime.parse("12:05"))
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.endTime).isEqualTo(LocalTime.parse("13:04"))
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.effectiveDate).isEqualTo(LocalDate.parse("2021-11-03"))
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.expiryDate).isEqualTo(LocalDate.parse("2021-11-03"))
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.agencyVisitTimesId.weekDay).isEqualTo("THU")
        assertThat(visit.agencyVisitSlot!!.agencyVisitTime.agencyVisitTimesId.timeSlotSequence).isEqualTo(1)
        assertThat(visit.agencyVisitSlot!!.maxAdults).isEqualTo(0)
        assertThat(visit.agencyVisitSlot!!.maxGroups).isEqualTo(0)
      }
    }

    @Test
    internal fun `room description defaults to VISITS when no visit parent area exists`() {
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
            "prisonId"          : "MDI",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      repository.runInTransaction {
        // Spot check that the database has been populated.
        val visit = repository.getVisit(response?.visitId)

        assertThat(visit.agencyVisitSlot).isNotNull
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.description).isEqualTo("MDI-VISITS-VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.locationCode).isEqualTo("VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.parentLocation).isNull()
      }
    }

    @Test
    internal fun `room description depends on root visits internal location when it exists - BXI it is VISIT`() {
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
            "prisonId"          : "BXI",
            "visitorPersonIds"  : [$personIds],
            "issueDate"         : "2021-11-02",
            "visitComment"      : "VSIP Ref: asd-fff-ddd",
            "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
            "room"              : "Main visit room",
            "openClosedStatus"  : "OPEN"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      repository.runInTransaction {
        // Spot check that the database has been populated.
        val visit = repository.getVisit(response?.visitId)

        assertThat(visit.agencyVisitSlot).isNotNull
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.description).isEqualTo("BXI-VISIT-VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.locationCode).isEqualTo("VSIP_SOC")
        assertThat(visit.agencyVisitSlot!!.agencyInternalLocation.parentLocation?.description).isEqualTo("BXI-VISIT")
      }
    }

    @Nested
    @DisplayName("Slot creation side effects")
    inner class RoomSlotSideEffects {
      @Test
      internal fun `will create a weekly slot when one does not already exist`() {
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).isEmpty()

        val visit = repository.getVisit(
          createVisit(
            startDateTime = "2021-11-04T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )

        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(1)
          .anyMatch { it.id == visit.agencyVisitSlot!!.id }

        val visitForFollowingWeek = repository.getVisit(
          createVisit(
            startDateTime = "2021-11-11T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(1)
          .anyMatch { it.id == visitForFollowingWeek.agencyVisitSlot!!.id }

        val visitForNextDay = repository.getVisit(
          createVisit(
            startDateTime = "2021-11-12T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(2)
          .anyMatch { it.id == visitForNextDay.agencyVisitSlot!!.id }
      }

      @Test
      internal fun `visits in different rooms are in different slots only when closed v open`() {
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).isEmpty()

        repository.runInTransaction {
          val visit = repository.getVisit(
            createVisit(
              startDateTime = "2021-11-04T14:00",
              endTime = "16:00",
              room = "Main visit room",
              openClosedStatus = "OPEN",
            ),
          )

          assertThat(visit.agencyInternalLocation!!.description).isEqualTo("$PRISON_ID-VISIT-VSIP_SOC")
          assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(1)
            .anyMatch { it.id == visit.agencyVisitSlot!!.id }
        }

        repository.runInTransaction {
          val visitInDifferentRestrictionRoom = repository.getVisit(
            createVisit(
              startDateTime = "2021-11-04T14:00",
              endTime = "16:00",
              room = "Main visit room",
              openClosedStatus = "CLOSED",
            ),
          )
          assertThat(visitInDifferentRestrictionRoom.agencyInternalLocation!!.description).isEqualTo("$PRISON_ID-VISIT-VSIP_CLO")
          assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(2)
            .anyMatch { it.id == visitInDifferentRestrictionRoom.agencyVisitSlot!!.id }

          val visitInDifferentPhysicalRoom = repository.getVisit(
            createVisit(
              startDateTime = "2021-11-04T14:00",
              endTime = "16:00",
              room = "Big blue room",
              openClosedStatus = "CLOSED",
            ),
          )
          assertThat(visitInDifferentPhysicalRoom.agencyInternalLocation!!.description).isEqualTo("$PRISON_ID-VISIT-VSIP_CLO")
          assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(2)
            .anyMatch { it.id == visitInDifferentPhysicalRoom.agencyVisitSlot!!.id }
        }
      }

      @Test
      internal fun `a different end time does not cause a new slot to be created`() {
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).isEmpty()

        val visit = repository.getVisit(
          createVisit(
            startDateTime = "2021-11-04T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )

        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(1)
          .anyMatch { it.id == visit.agencyVisitSlot!!.id }

        val visitThatEndAtDifferentTime = repository.getVisit(
          createVisit(
            startDateTime = "2021-11-04T14:00",
            endTime = "14:30",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )
        assertThat(repository.getAllAgencyVisitSlots(PRISON_ID)).hasSize(1)
          .anyMatch { it.id == visitThatEndAtDifferentTime.agencyVisitSlot!!.id }
      }

      @Test
      internal fun `a different end time does not cause a new time of day to be created`() {
        assertThat(repository.getAllAgencyVisitTimes(PRISON_ID)).isEmpty()

        repository.runInTransaction {
          val visit = repository.getVisit(
            createVisit(
              startDateTime = "2021-11-04T14:00",
              endTime = "16:00",
              room = "Main visit room",
              openClosedStatus = "OPEN",
            ),
          )

          assertThat(repository.getAllAgencyVisitTimes(PRISON_ID)).hasSize(1)
            .anyMatch { it.agencyVisitTimesId == visit.agencyVisitSlot!!.agencyVisitTime.agencyVisitTimesId }

          val visitThatEndAtDifferentTime = repository.getVisit(
            createVisit(
              startDateTime = "2021-11-04T14:00",
              endTime = "14:30",
              room = "Main visit room",
              openClosedStatus = "OPEN",
            ),
          )
          assertThat(repository.getAllAgencyVisitTimes(PRISON_ID)).hasSize(1)
            .anyMatch { it.agencyVisitTimesId == visitThatEndAtDifferentTime.agencyVisitSlot!!.agencyVisitTime.agencyVisitTimesId }
        }
      }

      @Test
      internal fun `a day of the week is created for prison when one does not already exist`() {
        assertThat(repository.getAgencyVisitDays("MONDAY", PRISON_ID)).isNull()

        val visit = repository.getVisit(
          createVisit(
            startDateTime = "2022-08-01T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )

        assertThat(repository.getAgencyVisitDays("MON", PRISON_ID))
          .isNotNull
          .matches { it!!.agencyVisitDayId.weekDay == visit.agencyVisitSlot!!.weekDay }
          .matches { it!!.agencyVisitDayId.weekDay == "MON" }

        val visitForFollowingWeek = repository.getVisit(
          createVisit(
            startDateTime = "2022-08-08T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )
        assertThat(repository.getAgencyVisitDays("MON", PRISON_ID))
          .isNotNull
          .matches { it!!.agencyVisitDayId.weekDay == visitForFollowingWeek.agencyVisitSlot!!.weekDay }
          .matches { it!!.agencyVisitDayId.weekDay == "MON" }

        val visitForNextDay = repository.getVisit(
          createVisit(
            startDateTime = "2022-08-09T14:00",
            endTime = "16:00",
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )
        assertThat(
          repository.getAgencyVisitDays(
            "TUE",
            PRISON_ID,
          ),
        )
          .isNotNull
          .matches { it!!.agencyVisitDayId.weekDay == visitForNextDay.agencyVisitSlot!!.weekDay }
          .matches { it!!.agencyVisitDayId.weekDay == "TUE" }
      }
    }
  }

  private fun createVisit(startDateTime: String, endTime: String, room: String, openClosedStatus: String) =
    webTestClient.post().uri("/prisoners/$offenderNo/visits")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          """{
                  "visitType"         : "SCON",
                  "startDateTime"     : "$startDateTime",
                  "endTime"           : "$endTime",
                  "prisonId"          : "$PRISON_ID",
                  "visitorPersonIds"  : [${threePeople.map { it.id }.joinToString(",")}],
                  "issueDate"         : "2021-11-02",
                  "visitComment"      : "VSIP Ref: asd-fff-ddd",
                  "visitOrderComment" : "VSIP Order Ref: asd-fff-ddd",
                  "room"              : "$room",
                  "openClosedStatus"  : "$openClosedStatus"
                }""",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody(CreateVisitResponse::class.java)
      .returnResult().responseBody!!.visitId

  @DisplayName("Cancel")
  @Nested
  inner class CancelVisit {
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
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody().isEmpty

      // Spot check that the database has been updated correctly.
      val visit = repository.getVisit(visitId)

      assertThat(visit.visitStatus.code).isEqualTo("CANC")
      assertThat(visit.offenderBooking.bookingId).isEqualTo(offenderBookingId)
      assertThat(visit.visitors).extracting("eventOutcome.code", "eventStatus.code", "outcomeReason.code")
        .containsExactly(
          tuple("ABS", "CANC", "VISCANC"),
          tuple("ABS", "CANC", "VISCANC"),
          tuple("ABS", "CANC", "VISCANC"),
          tuple("ABS", "CANC", "VISCANC"),
        )
      assertThat(visit.visitOrder?.status?.code).isEqualTo("CANC")
      assertThat(visit.visitOrder?.outcomeReason?.code).isEqualTo("VISCANC")
      assertThat(visit.visitOrder?.expiryDate).isEqualTo(LocalDate.now())

      val balanceAdjustments = offenderVisitBalanceAdjustmentRepository.findAll()

      assertThat(balanceAdjustments).extracting("offenderBooking.bookingId", "remainingPrivilegedVisitOrders")
        .containsExactly(
          tuple(offenderBookingId, -1),
          tuple(offenderBookingId, 1),
        )
    }

    @Test
    fun `cancel visit with visit id not found`() {
      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/9999/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue("""{ "outcome" : "VISCANC" }"""),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
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
          .returnResult().responseBody?.userMessage,
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
          .returnResult().responseBody?.userMessage,
      ).isEqualTo("Visit already cancelled, with outcome VISCANC")

      repository.updateVisitStatus(visitId)

      assertThat(
        webTestClient.put().uri("/prisoners/$offenderNo/visits/$visitId/cancel")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{ "outcome" : "VISCANC" }"""))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
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
          .returnResult().responseBody?.userMessage,
      ).isEqualTo("Bad request: Visit's offenderNo = A5194DY does not match argument = B1234BB")
    }
  }

  @DisplayName("Update")
  @Nested
  inner class UpdateVisit {
    private var existingVisitId: Long = 0
    private var existingVisitNoBalanceId: Long = 0

    @BeforeEach
    internal fun setUpData() {
      createPrisoner()
      existingVisitId = createVisit()!!
    }

    @AfterEach
    internal fun deleteData() {
      repository.delete(offenderAtMoorlands)
      repository.delete(threePeople)
      repository.deleteAllVisitSlots()
      repository.deleteAllVisitTimes()
      repository.deleteAllVisitDays()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/prisoners/$offenderNo/visits/$existingVisitId")
        .body(BodyInserters.fromValue(updateVisitWithPeople()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/prisoners/$offenderNo/visits/$existingVisitId")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(updateVisitWithPeople()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `update visit forbidden with wrong role`() {
      webTestClient.put().uri("/prisoners/$offenderNo/visits/$existingVisitId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(updateVisitWithPeople()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `404 when visit does not exist`() {
      webTestClient.put().uri("/prisoners/$offenderNo/visits/987654321")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(updateVisitWithPeople()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `404 when offender does not exist`() {
      webTestClient.put().uri("/prisoners/A1234JK/visits/$existingVisitId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(updateVisitWithPeople()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Nested
    inner class WrongOffender {
      lateinit var anotherOffender: Offender

      @BeforeEach
      internal fun setUp() {
        anotherOffender = repository.save(
          LegacyOffenderBuilder(nomsId = "A9999AA")
            .withBooking(
              OffenderBookingBuilder()
                .withVisitBalance(),
            ),
        )
      }

      @AfterEach
      internal fun tearDown() {
        repository.delete(anotherOffender)
      }

      @Test
      fun `404 when offender does not own the visit`() {
        webTestClient.put().uri("/prisoners/${anotherOffender.nomsId}/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(BodyInserters.fromValue(updateVisitWithPeople()))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class UpdateSuccessful {
      lateinit var johnSmith: Person
      lateinit var neoAyomide: Person
      lateinit var kashfAbidi: Person
      lateinit var offenderWithVisit: Offender
      lateinit var offenderWithVisitNoBalance: Offender
      lateinit var updateRequest: UpdateVisitRequest

      @BeforeEach
      internal fun setUp() {
        johnSmith = repository.save(PersonBuilder(firstName = "JOHN", lastName = "SMITH"))
        neoAyomide = repository.save(PersonBuilder(firstName = "NEO", lastName = "AYOMIDE"))
        kashfAbidi = repository.save(PersonBuilder(firstName = "KASHF", lastName = "ABIDI"))

        offenderWithVisit = repository.save(
          LegacyOffenderBuilder(nomsId = "A7688JM")
            .withBooking(
              OffenderBookingBuilder()
                .withVisitBalance()
                .withContacts(
                  *listOf(johnSmith, neoAyomide, kashfAbidi).map { OffenderContactBuilder(it) }
                    .toTypedArray(),
                ),
            ),
        )

        offenderWithVisitNoBalance = repository.save(
          LegacyOffenderBuilder(nomsId = "A7688JJ")
            .withBooking(
              OffenderBookingBuilder()
                .withContacts(
                  *listOf(johnSmith, neoAyomide, kashfAbidi).map { OffenderContactBuilder(it) }
                    .toTypedArray(),
                ),
            ),
        )

        existingVisitId = createVisit(
          offenderWithVisit.nomsId,
          CreateVisitRequest(
            visitType = "SCON",
            startDateTime = LocalDateTime.parse("2021-11-04T09:00"),
            endTime = LocalTime.parse("10:30"),
            prisonId = PRISON_ID,
            visitorPersonIds = listOf(johnSmith, neoAyomide).map { it.id },
            issueDate = LocalDate.parse("2021-11-02"),
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )!!

        existingVisitNoBalanceId = createVisit(
          offenderWithVisitNoBalance.nomsId,
          CreateVisitRequest(
            visitType = "SCON",
            startDateTime = LocalDateTime.parse("2021-11-04T09:00"),
            endTime = LocalTime.parse("10:30"),
            prisonId = PRISON_ID,
            visitorPersonIds = listOf(johnSmith, neoAyomide).map { it.id },
            issueDate = LocalDate.parse("2021-11-02"),
            room = "Main visit room",
            openClosedStatus = "OPEN",
          ),
        )!!

        updateRequest = UpdateVisitRequest(
          startDateTime = LocalDateTime.parse("2021-11-04T09:00"),
          endTime = LocalTime.parse("10:30"),
          visitorPersonIds = listOf(johnSmith, neoAyomide).map { it.id },
          room = "Main visit room",
          openClosedStatus = "OPEN",
        )
      }

      @AfterEach
      internal fun tearDown() {
        repository.delete(offenderWithVisit)
        repository.delete(offenderWithVisitNoBalance)
        repository.delete(listOf(johnSmith, neoAyomide, kashfAbidi))
      }

      @Test
      internal fun `can change the people visiting`() {
        webTestClient.put().uri("/prisoners/${offenderWithVisit.nomsId}/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(
            BodyInserters.fromValue(updateRequest.copy(visitorPersonIds = listOf(neoAyomide.id, kashfAbidi.id))),
          )
          .exchange()
          .expectStatus().isOk

        val updatedVisit = webTestClient.get().uri("/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(updatedVisit.visitors).extracting<Long>(Visitor::personId)
          .containsExactlyInAnyOrder(neoAyomide.id, kashfAbidi.id)

        // check visit order is updated with visitor changes
        val visit = repository.getVisit(updatedVisit.visitId)
        assertThat(visit.visitOrder).isNotNull
        assertThat(visit.visitOrder?.visitors).extracting("person.id", "groupLeader").containsExactly(
          tuple(neoAyomide.id, true),
          tuple(kashfAbidi.id, false),
        )
      }

      @Test
      internal fun `can change the people visiting for a visit without a visit order`() {
        webTestClient.put().uri("/prisoners/${offenderWithVisitNoBalance.nomsId}/visits/$existingVisitNoBalanceId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(
            BodyInserters.fromValue(updateRequest.copy(visitorPersonIds = listOf(neoAyomide.id, kashfAbidi.id))),
          )
          .exchange()
          .expectStatus().isOk

        val updatedVisit = webTestClient.get().uri("/visits/$existingVisitNoBalanceId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(updatedVisit.visitors).extracting<Long>(Visitor::personId)
          .containsExactlyInAnyOrder(neoAyomide.id, kashfAbidi.id)

        // confirm no visit order (and no visit order visitors)
        assertThat(repository.getVisit(updatedVisit.visitId).visitOrder).isNull()
      }

      @Test
      internal fun `can not change the room the visit is in if restriction does not change`() {
        webTestClient.put().uri("/prisoners/${offenderWithVisit.nomsId}/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(
            BodyInserters.fromValue(updateRequest.copy(room = "Another room", openClosedStatus = "OPEN")),
          )
          .exchange()
          .expectStatus().isOk

        val updatedVisit = webTestClient.get().uri("/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(updatedVisit.agencyInternalLocation?.description).isEqualTo("$PRISON_ID-VISIT-VSIP_SOC")

        val visits = jdbcTemplate.query(
          """SELECT * FROM V_OFFENDER_VISITS 
            |JOIN AGENCY_INTERNAL_LOCATIONS location ON VISIT_INTERNAL_LOCATION_ID = location.INTERNAL_LOCATION_ID 
            |WHERE OFFENDER_ID_DISPLAY = '${offenderWithVisit.nomsId}'
          """.trimMargin(),
          ColumnMapRowMapper(),
        )

        assertThat(visits).hasSize(1)
        assertThat(visits.first()["DESCRIPTION"]).isEqualTo("$PRISON_ID-VISIT-VSIP_SOC")
      }

      @Test
      internal fun `can change the restriction status`() {
        webTestClient.put().uri("/prisoners/${offenderWithVisit.nomsId}/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(
            BodyInserters.fromValue(updateRequest.copy(openClosedStatus = "CLOSED")),
          )
          .exchange()
          .expectStatus().isOk

        val updatedVisit = webTestClient.get().uri("/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(updatedVisit.agencyInternalLocation?.description).isEqualTo("$PRISON_ID-VISIT-VSIP_CLO")
      }

      @Test
      internal fun `can change date and time of visit`() {
        webTestClient.put().uri("/prisoners/${offenderWithVisit.nomsId}/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .body(
            BodyInserters.fromValue(updateRequest.copy(LocalDateTime.parse("2021-11-05T14:00"), LocalTime.parse("15:30"))),
          )
          .exchange()
          .expectStatus().isOk

        val updatedVisit = webTestClient.get().uri("/visits/$existingVisitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(updatedVisit.startDateTime).isEqualTo(LocalDateTime.parse("2021-11-05T14:00"))
        assertThat(updatedVisit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-05T15:30"))

        val visits = jdbcTemplate.query(
          """SELECT * FROM V_OFFENDER_VISITS 
            |WHERE OFFENDER_ID_DISPLAY = '${offenderWithVisit.nomsId}'
          """.trimMargin(),
          ColumnMapRowMapper(),
        )

        assertThat(visits).hasSize(1)
        assertThat(visits.first()["VISIT_DATE"]).isEqualTo(LocalDate.parse("2021-11-05").asSQLTimestamp())
        assertThat(visits.first()["START_TIME"]).isEqualTo(LocalDateTime.parse("2021-11-05T14:00").asSQLTimestamp())
        assertThat(visits.first()["END_TIME"]).isEqualTo(LocalDateTime.parse("2021-11-05T15:30").asSQLTimestamp())
      }
    }
  }

  private fun createVisit(offenderNo: String = this.offenderNo, request: CreateVisitRequest = createVisitWithPeople()) =
    webTestClient.post().uri("/prisoners/$offenderNo/visits")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(request))
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
          LegacyOffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder().withVisitors(
                    VisitVisitorBuilder(person1, leadVisitor = false),
                    VisitVisitorBuilder(person2, leadVisitor = false),
                  ),
                ),
            ),
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
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.visitStatus.code).isEqualTo("SCH")
        assertThat(visit.visitType.code).isEqualTo("SCON")
        assertThat(visit.visitOutcome).isNull()
        assertThat(visit.modifyUserId).isNull()
        assertThat(visit.createUserId).isEqualTo("SA")
        assertThat(visit.offenderNo).isEqualTo("A1234TT")
        assertThat(visit.prisonId).isEqualTo("MDI")
        assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T12:05"))
        assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2022-01-01T13:05"))
        assertThat(visit.leadVisitor).isNull()
        assertThat(visit.whenUpdated).isNull()
        assertThat(visit.whenCreated).isNotNull()
        assertThat(visit.visitors).extracting("leadVisitor")
          .containsExactly(
            false,
            false,
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
            .returnResult().responseBody?.userMessage,
        ).isEqualTo("Not Found: visit id 123")
      }

      @Test
      fun `get visit prevents access without appropriate role`() {
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        assertThat(
          webTestClient.get().uri("/visits/$visitId")
            .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
            .exchange()
            .expectStatus().isForbidden,
        )
      }

      @Test
      fun `get visit prevents access without authorization`() {
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        assertThat(
          webTestClient.get().uri("/visits/$visitId")
            .exchange()
            .expectStatus().isUnauthorized,
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
            phoneNumbers = listOf(Triple("HOME", "01145551234", "ext456"), Triple("MOB", "07973555123", null)),
            addressBuilders = listOf(
              PersonAddressBuilder(phoneNumbers = listOf(Triple("HOME", "01145559999", null))),
            ),
          ),
        )

        offenderAtMoorlands = repository.save(
          LegacyOffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder().withVisitors(
                    VisitVisitorBuilder(leadVisitor, leadVisitor = true),
                  ),
                ),
            ),
        )

        offenderNo = offenderAtMoorlands.nomsId
        offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(offenderAtMoorlands)
      }

      @Test
      fun `visit wil contain lead visitor with telephone numbers`() {
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.leadVisitor).isNotNull
        assertThat(visit.leadVisitor?.fullName).isEqualTo("Manon Dupont")
        assertThat(visit.leadVisitor?.telephones).containsExactlyInAnyOrder(
          "01145551234 ext456",
          "07973555123",
          "01145559999",
        )
        assertThat(visit.visitors).extracting("leadVisitor")
          .containsExactly(
            true,
          )
      }
    }

    @DisplayName("With an outcome")
    @Nested
    inner class WithOutcome {

      @BeforeEach
      internal fun createPrisonerWithVisit() {
        val leadVisitor = repository.save(
          PersonBuilder(
            firstName = "Manon",
            lastName = "Dupont",
          ),
        )

        offenderAtMoorlands = repository.save(
          LegacyOffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder()
                    .withVisitors(VisitVisitorBuilder(leadVisitor, leadVisitor = true))
                    .withVisitOutcome("REFUSED"),
                ),
            ),
        )

        offenderNo = offenderAtMoorlands.nomsId
        offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(offenderAtMoorlands)
      }

      @Test
      fun `visit will contain outcome and status`() {
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.visitStatus.code).isEqualTo("SCH")
        assertThat(visit.visitType.code).isEqualTo("SCON")
        assertThat(visit.visitOutcome).isEqualTo(
          CodeDescription(
            code = "REFUSED",
            description = "Offender Refused Visit",
          ),
        )
      }
    }

    @DisplayName("With an outcome that is not in reference table")
    @Nested
    inner class WithOutcomeNoReferenceData {

      @BeforeEach
      internal fun createPrisonerWithVisit() {
        val leadVisitor = repository.save(
          PersonBuilder(
            firstName = "Manon",
            lastName = "Dupont",
          ),
        )

        offenderAtMoorlands = repository.save(
          LegacyOffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withVisits(
                  VisitBuilder()
                    .withVisitors(VisitVisitorBuilder(leadVisitor, leadVisitor = true))
                    .withVisitOutcome("BATCH_CANC"),
                ),
            ),
        )

        offenderNo = offenderAtMoorlands.nomsId
        offenderBookingId = offenderAtMoorlands.latestBooking().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(offenderAtMoorlands)
      }

      @Test
      fun `visit will contain outcome and status`() {
        val visitId = offenderAtMoorlands.latestBooking().visits[0].id
        val visit = webTestClient.get().uri("/visits/$visitId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(VisitResponse::class.java)
          .returnResult().responseBody!!

        assertThat(visit.visitStatus.code).isEqualTo("SCH")
        assertThat(visit.visitType.code).isEqualTo("SCON")
        assertThat(visit.visitOutcome).isEqualTo(
          CodeDescription(
            code = "BATCH_CANC",
            description = "BATCH_CANC",
          ),
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
        LegacyOffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = "2022-01-01T09:00",
                  endDateTimeString = "2022-01-01T10:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                  VisitVisitorBuilder(person2, leadVisitor = true),
                ),
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = "2022-01-01T12:00",
                  endDateTimeString = "2022-01-01T13:00",
                  // no room specified
                  agencyInternalLocationDescription = null,
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
              ),
          ),
      )
      offenderAtLeeds = repository.save(
        LegacyOffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "LEI",
                  startDateTimeString = "2022-01-02T09:00",
                  endDateTimeString = "2022-01-02T10:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "LEI",
                  startDateTimeString = "2022-01-02T14:00",
                  endDateTimeString = "2022-01-02T15:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
              ),
          ),
      )
      offenderAtBrixton = repository.save(
        LegacyOffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2022-01-01T09:00",
                  endDateTimeString = "2022-01-01T10:00",
                  visitTypeCode = "OFFI",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-01-01T09:00",
                  endDateTimeString = "2023-01-01T10:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-02-01T09:00",
                  endDateTimeString = "2023-02-01T10:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = "2023-03-01T09:00",
                  endDateTimeString = "2023-03-01T10:00",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
              ),
          ),
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
            *leedsVisitIds,
          ),
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
            *brixtonVisitIds + leedsVisitIds,
          ),
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
            offenderAtBrixton.latestBooking().visits[0].id.toInt(),
          ),
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
            offenderAtMoorlands.latestBooking().visits[0].id.toInt(),
          ),
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
            offenderAtBrixton.latestBooking().visits[3].id.toInt(),
          ),
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
            offenderAtMoorlands.latestBooking().visits[0].id.toInt(),
          ),
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
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get visit prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/visits/ids")
          .exchange()
          .expectStatus().isUnauthorized,
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
        LegacyOffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "MDI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(2), LocalTime.of(11, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(2), LocalTime.of(12, 0)).toString(),
                  agencyInternalLocationDescription = "MDI-1-1-001",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                  VisitVisitorBuilder(person2, leadVisitor = true),
                ),
              ),
          ),
      )
      offenderAtLeeds = repository.save(
        LegacyOffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "LEI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().minusWeeks(2), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().minusWeeks(2), LocalTime.of(10, 0)).toString(),
                  agencyInternalLocationDescription = "LEI-VISITS-NEW_SOC_VIS",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                /* bad data - visit booked way in the future */
                VisitBuilder(
                  agyLocId = "LEI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusYears(24), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusYears(24), LocalTime.of(10, 0)).toString(),
                  agencyInternalLocationDescription = "LEI-VISITS-SEG_VIS",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
              ),
          ),
      )
      offenderAtBrixton = repository.save(
        LegacyOffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withVisits(
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(1), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(1), LocalTime.of(10, 0)).toString(),
                  visitTypeCode = "OFFI",
                  agencyInternalLocationDescription = "BXI-VISIT",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(5), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(5), LocalTime.of(10, 0)).toString(),
                  agencyInternalLocationDescription = "BXI-VISIT",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(10), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(10), LocalTime.of(10, 0)).toString(),
                  agencyInternalLocationDescription = "BXI-VISIT2",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
                VisitBuilder(
                  agyLocId = "BXI",
                  startDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(15), LocalTime.of(9, 0)).toString(),
                  endDateTimeString = LocalDateTime.of(LocalDate.now().plusWeeks(15), LocalTime.of(10, 0)).toString(),
                  agencyInternalLocationDescription = "BXI-VISIT2",
                ).withVisitors(
                  VisitVisitorBuilder(person1),
                ),
              ),
          ),
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
      val oneWeekInFuture = LocalDateTime.of(LocalDate.now().plusWeeks(1), LocalTime.NOON).toString()
      webTestClient.get()
        .uri("/visits/rooms/usage-count?prisonIds=BXI&prisonIds=MDI&fromDateTime=$oneWeekInFuture&visitTypes=SCON")
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
    fun `get visit rooms usage - ignores visits in the past (and excludes erroneous future dates) by default`() {
      webTestClient.get()
        .uri("/visits/rooms/usage-count?prisonIds=LEI&visitTypes=SCON")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `get visit rooms usage - include ALL visits regardless of visit date (including any erroneous future dates)`() {
      webTestClient.get()
        .uri("/visits/rooms/usage-count?prisonIds=LEI&visitTypes=SCON&futureVisitsOnly=false")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
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
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get visit rooms usage prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/visits/rooms/usage-count")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }
}

private fun LocalDate.asSQLTimestamp(): Timestamp = Timestamp.valueOf(this.atStartOfDay())
private fun LocalDateTime.asSQLTimestamp(): Timestamp = Timestamp.valueOf(this)
