package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import java.time.DayOfWeek
import java.time.LocalTime

class VisitsConfigurationIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @DisplayName("GET /visits/configuration/time-slots/ids")
  @Nested
  inner class GetVisitTimeSlotIds {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        agencyVisitDay(prisonerId = "MDI", weekDay = WeekDay.MON) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "MDI", weekDay = WeekDay.TUE) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "MDI", weekDay = WeekDay.WED) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "MDI", weekDay = WeekDay.THU) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "MDI", weekDay = WeekDay.FRI) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.MON) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.WED) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.FRI) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.SAT) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.SUN) {
          visitTimeSlot(timeSlotSequence = 1, startTime = LocalTime.parse("09:00"), endTime = LocalTime.parse("10:00"))
          visitTimeSlot(timeSlotSequence = 2, startTime = LocalTime.parse("10:00"), endTime = LocalTime.parse("11:00"))
          (3L..6L).forEach {
            visitTimeSlot(timeSlotSequence = it.toInt(), startTime = LocalTime.parse("09:00").plusHours(it), endTime = LocalTime.parse("10:00").plusHours(it))
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/visits/configuration/time-slots/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/visits/configuration/time-slots/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/visits/configuration/time-slots/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 or all time slots`() {
        webTestClient.get().uri {
          it.path("/visits/configuration/time-slots/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(60)
          .jsonPath("numberOfElements").isEqualTo(20)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/visits/configuration/time-slots/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(60)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(60)
          .jsonPath("size").isEqualTo(1)
      }

      @Test
      fun `id contains prison, day of week and sequence number`() {
        val pageResponse: VisitTimeSlotIdPageResponse = webTestClient.get().uri {
          it.path("/visits/configuration/time-slots/ids")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(pageResponse.content).hasSize(2)
        assertThat(pageResponse.content[0].prisonId).isEqualTo("MDI")
        assertThat(pageResponse.content[0].dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(pageResponse.content[0].timeSlotSequence).isEqualTo(1L)
        assertThat(pageResponse.content[1].prisonId).isEqualTo("MDI")
        assertThat(pageResponse.content[1].dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(pageResponse.content[1].timeSlotSequence).isEqualTo(2L)
      }
    }
  }
}

private data class VisitTimeSlotIdPageResponse(
  val totalElements: Long,
  val totalPages: Int,
  val content: List<VisitTimeSlotIdResponse>,
  val number: Int,
  val numberOfElements: Int? = null,
)
