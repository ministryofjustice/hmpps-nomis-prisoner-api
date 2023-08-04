@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDate

class GetActivityResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  @BeforeEach
  fun setup() {
    nomisDataBuilder.build {
      programService()
    }
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  @DisplayName("GET /activities/ids")
  inner class FindMigrationActivities {

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/activities/ids?prisonId=BXI")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/activities/ids?prisonId=BXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/activities/ids?prisonId=BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `invalid prison should return not found`() {
        webTestClient.get().uri("/activities/ids?prisonId=XXX")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Prison with id=XXX does not exist")
          }
      }
    }

    @Nested
    inner class Paging {

      private lateinit var courseActivity: CourseActivity
      private lateinit var offenderBooking: OffenderBooking

      @Test
      fun `finds an active migration with an allocation`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            offenderBooking = booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }

      @Test
      fun `finds a full page of activities`() {
        val courseActivities = mutableListOf<CourseActivity>()
        val pageSize = 3
        nomisDataBuilder.build {
          programService {
            repeat(pageSize + 1) {
              courseActivities += courseActivity(startDate = "$today")
            }
          }
          offender {
            offenderBooking = booking {
              courseActivities.forEach {
                courseAllocation(courseActivity = it, startDate = "$today")
              }
            }
          }
        }

        webTestClient.getMigrationActivities(pageSize = pageSize, page = 0)
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivities[0].courseActivityId)
          .jsonPath("content[1].courseActivityId").isEqualTo(courseActivities[1].courseActivityId)
          .jsonPath("content[2].courseActivityId").isEqualTo(courseActivities[2].courseActivityId)
          .jsonPath("content[3].courseActivityId").doesNotExist()
      }

      @Test
      fun `finds the second page of activities`() {
        val courseActivities = mutableListOf<CourseActivity>()
        val pageSize = 3
        nomisDataBuilder.build {
          programService {
            repeat(pageSize + 1) {
              courseActivities += courseActivity(startDate = "$today")
            }
          }
          offender {
            offenderBooking = booking {
              courseActivities.forEach {
                courseAllocation(courseActivity = it, startDate = "$today")
              }
            }
          }
        }

        webTestClient.getMigrationActivities(pageSize = pageSize, page = 1)
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivities[3].courseActivityId)
          .jsonPath("content[1].courseActivityId").doesNotExist()
      }

      @Test
      fun `should return correct paging details for all pages`() {
        val courseActivities = mutableListOf<CourseActivity>()
        nomisDataBuilder.build {
          programService {
            repeat(50) {
              courseActivities += courseActivity(startDate = "$today")
            }
          }
          courseActivities.forEachIndexed { index, activity ->
            offender {
              booking {
                // give half of the activities active allocations
                if (index % 2 == 0) {
                  courseAllocation(courseActivity = activity, startDate = "$today")
                }
                // and half of the activities inactive allocations
                else {
                  courseAllocation(courseActivity = activity, startDate = "$yesterday", endDate = "$yesterday")
                }
              }
            }
          }
        }

        webTestClient.getMigrationActivities(page = 0)
          .expectBody()
          .jsonPath("totalElements").isEqualTo(25)
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(10)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getMigrationActivities(page = 1)
          .expectBody()
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(1)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getMigrationActivities(page = 2)
          .expectBody()
          .jsonPath("numberOfElements").isEqualTo(5)
          .jsonPath("number").isEqualTo(2)
          .jsonPath("content.length()").isEqualTo(5)
      }
    }

    @Nested
    inner class ActivitySelection {

      private lateinit var courseActivity: CourseActivity

      @Test
      fun `should not include future course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$tomorrow")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include ended course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", endDate = "$yesterday")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include activities with no prisoners`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner allocation has ended status`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", programStatusCode = "END")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner allocation past end date`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$yesterday", endDate = "$yesterday")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner allocation in future`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner active in different prison`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner is inactive`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking(active = false) {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should include if prisoner is ACTIVE OUT`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getMigrationActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }
    }

    fun WebTestClient.getMigrationActivities(
      pageSize: Int = 10,
      page: Int = 0,
      prison: String = "BXI",
    ): WebTestClient.ResponseSpec =
      get().uri {
        it.path("/activities/ids")
          .queryParam("prisonId", prison)
          .queryParam("size", pageSize)
          .queryParam("page", page)
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
  }

  @Nested
  @DisplayName("GET /activities/{courseActivityId}")
  inner class GetActivityMigration {

    private lateinit var courseActivity: CourseActivity

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/activities/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/activities/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/activities/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `unknown course activity should return not found`() {
        webTestClient.get().uri("/activities/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course Activity with id=9999 does not exist")
          }
      }
    }

    @Nested
    inner class ActivityDetails {

      @Test
      fun `should return all Activity details`() {
        nomisDataBuilder.build {
          programService(programCode = "SOME_PROGRAM") {
            courseActivity = courseActivity(
              prisonId = "BXI",
              startDate = "$yesterday",
              endDate = "$tomorrow",
              internalLocationId = -3005,
              capacity = 10,
              description = "Kitchen work",
              minimumIncentiveLevelCode = "BAS",
              excludeBankHolidays = true,
            )
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("programCode").isEqualTo("SOME_PROGRAM")
          .jsonPath("prisonId").isEqualTo("BXI")
          .jsonPath("startDate").isEqualTo("$yesterday")
          .jsonPath("endDate").isEqualTo("$tomorrow")
          .jsonPath("internalLocationId").isEqualTo(-3005)
          .jsonPath("internalLocationCode").isEqualTo("CLASS1")
          .jsonPath("internalLocationDescription").isEqualTo("BXI-CLASS1")
          .jsonPath("capacity").isEqualTo(10)
          .jsonPath("description").isEqualTo("Kitchen work")
          .jsonPath("minimumIncentiveLevel").isEqualTo("BAS")
          .jsonPath("excludeBankHolidays").isEqualTo(true)
      }

      @Test
      fun `should handle nullable Activity details`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(
              endDate = null,
              internalLocationId = null,
            )
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("endDate").doesNotExist()
          .jsonPath("internalLocationId").doesNotExist()
          .jsonPath("internalLocationCode").doesNotExist()
          .jsonPath("internalLocationDescription").doesNotExist()
      }
    }

    @Nested
    inner class ScheduleRules {

      @Test
      fun `should include schedule rules`() {
        nomisDataBuilder.build {
          programService(programCode = "SOME_PROGRAM") {
            courseActivity = courseActivity {
              courseScheduleRule(
                startTimeHours = 9,
                startTimeMinutes = 30,
                endTimeHours = 12,
                endTimeMinutes = 15,
                monday = true,
                tuesday = false,
                wednesday = true,
                thursday = false,
                friday = true,
                saturday = false,
                sunday = false,
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("scheduleRules[0].startTime").isEqualTo("09:30")
          .jsonPath("scheduleRules[0].endTime").isEqualTo("12:15")
          .jsonPath("scheduleRules[0].monday").isEqualTo(true)
          .jsonPath("scheduleRules[0].tuesday").isEqualTo(false)
          .jsonPath("scheduleRules[0].wednesday").isEqualTo(true)
          .jsonPath("scheduleRules[0].thursday").isEqualTo(false)
          .jsonPath("scheduleRules[0].friday").isEqualTo(true)
          .jsonPath("scheduleRules[0].saturday").isEqualTo(false)
          .jsonPath("scheduleRules[0].sunday").isEqualTo(false)
      }

      @Test
      fun `should handle multiple schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              courseScheduleRule(
                startTimeHours = 9,
                startTimeMinutes = 30,
                endTimeHours = 12,
                endTimeMinutes = 15,
              )
              courseScheduleRule(
                startTimeHours = 13,
                startTimeMinutes = 0,
                endTimeHours = 16,
                endTimeMinutes = 30,
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("scheduleRules[0].startTime").isEqualTo("09:30")
          .jsonPath("scheduleRules[0].endTime").isEqualTo("12:15")
          .jsonPath("scheduleRules[1].startTime").isEqualTo("13:00")
          .jsonPath("scheduleRules[1].endTime").isEqualTo("16:30")
      }
    }

    @Nested
    inner class PayRates {

      @Test
      fun `should return pay rates`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate(
                iepLevelCode = "BAS",
                payBandCode = "1",
                startDate = "$today",
                halfDayRate = 1.1,
              )
              payRate(
                iepLevelCode = "BAS",
                payBandCode = "2",
                startDate = "$today",
                halfDayRate = 2.2,
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("payRates[0].incentiveLevelCode").isEqualTo("BAS")
          .jsonPath("payRates[0].payBand").isEqualTo("1")
          .jsonPath("payRates[0].rate").isEqualTo("1.1")
          .jsonPath("payRates[1].incentiveLevelCode").isEqualTo("BAS")
          .jsonPath("payRates[1].payBand").isEqualTo("2")
          .jsonPath("payRates[1].rate").isEqualTo("2.2")
      }

      @Test
      fun `should not return inactive pay rates`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate(
                payBandCode = "1",
                startDate = "$yesterday",
                endDate = "$yesterday",
              )
              payRate(
                payBandCode = "2",
                startDate = "$today",
                endDate = null,
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("payRates[0].payBand").isEqualTo("2")
          .jsonPath("payRates[1]").doesNotExist()
      }

      @Test
      fun `should include pay rates expiring today`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate(
                payBandCode = "1",
                startDate = "$yesterday",
                endDate = "$today",
              )
              payRate(
                payBandCode = "2",
                startDate = "$today",
                endDate = null,
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("payRates[0].payBand").isEqualTo("1")
          .jsonPath("payRates[1].payBand").isEqualTo("2")
      }
    }

    @Nested
    inner class Allocations {

      @Test
      fun `should include allocations`() {
        lateinit var booking1: OffenderBooking
        lateinit var booking2: OffenderBooking
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking1 = booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "$yesterday",
                endDate = null,
                programStatusCode = "ALLOC",
              )
            }
          }
          offender(nomsId = "A2222AA") {
            booking2 = booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "$today",
                endDate = null,
                programStatusCode = "ALLOC",
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("allocations[0].startDate").isEqualTo("$yesterday")
          .jsonPath("allocations[1].nomisId").isEqualTo("A2222AA")
          .jsonPath("allocations[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("allocations[1].startDate").isEqualTo("$today")
      }

      @Test
      fun `should include all allocation details`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender {
            booking(livingUnitId = -3009) {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "$yesterday",
                endDate = "$tomorrow",
                endReasonCode = "WDRAWN",
                endComment = "Withdrawn",
                suspended = true,
              ) {
                payBand(payBandCode = "1")
              }
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].startDate").isEqualTo("$yesterday")
          .jsonPath("allocations[0].endDate").isEqualTo("$tomorrow")
          .jsonPath("allocations[0].endReasonCode").isEqualTo("WDRAWN")
          .jsonPath("allocations[0].endComment").isEqualTo("Withdrawn")
          .jsonPath("allocations[0].suspended").isEqualTo(true)
          .jsonPath("allocations[0].payBand").isEqualTo("1")
          .jsonPath("allocations[0].livingUnitDescription").isEqualTo("BXI-A-1-016")
      }

      @Test
      fun `should not include allocations in the wrong status`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                programStatusCode = "ALLOC",
              )
            }
          }
          offender(nomsId = "A2222AA") {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                programStatusCode = "END",
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[1]").doesNotExist()
      }

      @Test
      fun `should not include allocations which are inactive by date `() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "$yesterday",
                endDate = null,
              )
            }
          }
          offender(nomsId = "A2222AA") {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "$yesterday",
                endDate = "$yesterday",
              )
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[1]").doesNotExist()
      }

      @Test
      fun `should not include prisoners who are OUT`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation(courseActivity = courseActivity)
            }
          }
          offender(nomsId = "A2222AA") {
            booking(active = false) {
              courseAllocation(courseActivity = courseActivity)
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[1]").doesNotExist()
      }

      @Test
      fun `should not include prisoners from a different prison`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(prisonId = "BXI")
          }
          offender(nomsId = "A1111AA") {
            booking(agencyLocationId = "BXI") {
              courseAllocation(courseActivity = courseActivity)
            }
          }
          offender(nomsId = "A2222AA") {
            booking(agencyLocationId = "MDI") {
              courseAllocation(courseActivity = courseActivity)
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[1]").doesNotExist()
      }

      @Test
      fun `should only include active allocation pay bands`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation(courseActivity = courseActivity) {
                payBand(startDate = "$yesterday", endDate = "$yesterday", payBandCode = "5")
                payBand(startDate = "$today", endDate = null, payBandCode = "6")
              }
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[0].payBand").isEqualTo("6")
      }

      @Test
      fun `should include allocations even if there are no pay bands`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation(courseActivity = courseActivity) {} // no pay bands
            }
          }
        }

        webTestClient.get().uri("/activities/${courseActivity.courseActivityId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("allocations[0].nomisId").isEqualTo("A1111AA")
          .jsonPath("allocations[0].payBand").doesNotExist()
      }
    }
  }
}
