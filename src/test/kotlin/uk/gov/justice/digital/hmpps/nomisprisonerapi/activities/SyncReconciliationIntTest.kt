package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDate

class SyncReconciliationIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class AllocationReconciliation {

    lateinit var courseActivity: CourseActivity
    lateinit var offenderBooking: OffenderBooking

    private fun WebTestClient.getAllocationReconciliation(prisonId: String = "BXI") = get()
      .uri("/allocations/reconciliation/$prisonId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk

    @BeforeEach
    fun `create an activity in BXI`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(prisonId = "BXI")
        }
      }
    }

    @Test
    fun `should return unauthorised if no token`() {
      webTestClient.get()
        .uri("/allocations/reconciliation/BXI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/allocations/reconciliation/BXI")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.get()
        .uri("/allocations/reconciliation/BXI")
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return empty list if none`() {
      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("prisonId").isEqualTo("BXI")
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should find a single booking's allocation`() {
      nomisDataBuilder.build {
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity)
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("prisonId").isEqualTo("BXI")
        .jsonPath("bookings.size()").isEqualTo(1)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("1")
    }

    @Test
    fun `should ignore allocations at the wrong status`() {
      nomisDataBuilder.build {
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity, programStatusCode = "END")
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore allocations with an end date in the past`() {
      nomisDataBuilder.build {
        offender {
          booking {
            courseAllocation(courseActivity, endDate = "$yesterday")
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should find allocations with an end date not in the past`() {
      nomisDataBuilder.build {
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity, endDate = "$today")
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(1)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("1")
    }

    @Test
    fun `should ignore allocations with a start date in the future`() {
      nomisDataBuilder.build {
        offender {
          booking {
            courseAllocation(courseActivity, startDate = "$tomorrow")
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore allocations where the course activity has ended`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(endDate = "$yesterday")
        }
        offender {
          booking {
            courseAllocation(courseActivity)
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore allocations in the wrong prison`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(prisonId = "MDI")
        }
        offender {
          booking {
            courseAllocation(courseActivity)
          }
        }
      }

      webTestClient.getAllocationReconciliation(prisonId = "BXI")
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore bookings in the wrong prison`() {
      nomisDataBuilder.build {
        offender {
          booking(agencyLocationId = "MDI") {
            courseAllocation(courseActivity)
          }
        }
      }

      webTestClient.getAllocationReconciliation(prisonId = "BXI")
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore allocations which are suspended`() {
      nomisDataBuilder.build {
        offender {
          booking {
            courseAllocation(courseActivity, suspended = true)
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should find multiple allocations for the same booking`() {
      lateinit var courseActivity2: CourseActivity
      nomisDataBuilder.build {
        programService {
          courseActivity2 = courseActivity()
        }
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity)
            courseAllocation(courseActivity2)
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(1)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("2")
    }

    @Test
    fun `should find multiple bookings`() {
      lateinit var offenderBooking2: OffenderBooking
      nomisDataBuilder.build {
        offender(nomsId = "A1234AA") {
          offenderBooking = booking {
            courseAllocation(courseActivity)
          }
        }
        offender(nomsId = "A1234BB") {
          offenderBooking2 = booking {
            courseAllocation(courseActivity)
          }
        }
      }

      webTestClient.getAllocationReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(2)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("1")
        .jsonPath("bookings[1].bookingId").isEqualTo(offenderBooking2.bookingId)
        .jsonPath("bookings[1].count").isEqualTo("1")
    }
  }

  @Nested
  inner class AttendanceReconciliation {

    lateinit var courseActivity: CourseActivity
    lateinit var offenderBooking: OffenderBooking
    lateinit var courseSchedule: CourseSchedule

    private fun WebTestClient.getAttendanceReconciliation(prisonId: String = "BXI") = get()
      .uri("/attendances/reconciliation/$prisonId?date=$today")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk

    @BeforeEach
    fun `create an activity in BXI with a course scheduled for today`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(prisonId = "BXI") {
            courseSchedule = courseSchedule(scheduleDate = "$today")
          }
        }
      }
    }

    @Test
    fun `should return unauthorised if no token`() {
      webTestClient.get()
        .uri("/attendances/reconciliation/prisonId?date=$today")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/attendances/reconciliation/prisonId?date=$today")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.get()
        .uri("/attendances/reconciliation/prisonId?date=$today")
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return bad request if no date passed`() {
      webTestClient.get()
        .uri("/attendances/reconciliation/prisonId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return empty list if no attendances`() {
      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("prisonId").isEqualTo("BXI")
        .jsonPath("date").isEqualTo("$today")
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should find a single booking's attendance`() {
      nomisDataBuilder.build {
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(1)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("1")
    }

    @Test
    fun `should ignore an unpaid attendance`() {
      nomisDataBuilder.build {
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = false)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore an attendance in the wrong prison`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(prisonId = "MDI") {
            courseSchedule = courseSchedule(scheduleDate = "$today")
          }
        }
        offender {
          offenderBooking = booking(agencyLocationId = "MDI") {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore an attendance on the wrong date`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(prisonId = "BXI") {
            courseSchedule = courseSchedule(scheduleDate = "$yesterday")
          }
        }
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(0)
    }

    @Test
    fun `should include multiple attendances for the same booking`() {
      lateinit var courseSchedule2: CourseSchedule
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseSchedule = courseSchedule(scheduleDate = "$today")
            courseSchedule2 = courseSchedule(scheduleDate = "$today")
          }
        }
        offender {
          offenderBooking = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
              courseAttendance(courseSchedule2, pay = true)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(1)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("2")
    }

    @Test
    fun `should include multiple bookings`() {
      lateinit var offenderBooking2: OffenderBooking
      nomisDataBuilder.build {
        offender(nomsId = "A1234AA") {
          offenderBooking = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
            }
          }
        }
        offender(nomsId = "A1234BB") {
          offenderBooking2 = booking {
            courseAllocation(courseActivity) {
              courseAttendance(courseSchedule, pay = true)
            }
          }
        }
      }

      webTestClient.getAttendanceReconciliation()
        .expectBody()
        .jsonPath("bookings.size()").isEqualTo(2)
        .jsonPath("bookings[0].bookingId").isEqualTo(offenderBooking.bookingId)
        .jsonPath("bookings[0].count").isEqualTo("1")
        .jsonPath("bookings[1].bookingId").isEqualTo(offenderBooking2.bookingId)
        .jsonPath("bookings[1].count").isEqualTo("1")
    }
  }
}
