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
  inner class FindActiveActivities {

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
      fun `finds an active activity with an allocation`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            offenderBooking = booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
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
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
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

        webTestClient.getActiveActivities(pageSize = pageSize, page = 0)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivities[0].courseActivityId)
          .jsonPath("content[1].courseActivityId").isEqualTo(courseActivities[1].courseActivityId)
          .jsonPath("content[2].courseActivityId").isEqualTo(courseActivities[2].courseActivityId)
      }

      @Test
      fun `finds the second page of activities`() {
        val courseActivities = mutableListOf<CourseActivity>()
        val pageSize = 3
        nomisDataBuilder.build {
          programService {
            repeat(pageSize + 1) {
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
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

        webTestClient.getActiveActivities(pageSize = pageSize, page = 1)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivities[3].courseActivityId)
      }

      @Test
      fun `should return correct paging details for all pages`() {
        val courseActivities = mutableListOf<CourseActivity>()
        nomisDataBuilder.build {
          programService {
            repeat(50) {
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
            }
          }
          courseActivities.forEachIndexed { index, activity ->
            offender {
              booking {
                // give half of the activities active allocations and the rest no allocations
                if (index % 2 == 0) {
                  courseAllocation(courseActivity = activity, startDate = "$today")
                }
              }
            }
          }
        }

        webTestClient.getActiveActivities(page = 0)
          .expectBody()
          .jsonPath("totalElements").isEqualTo(25)
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(10)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getActiveActivities(page = 1)
          .expectBody()
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(1)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getActiveActivities(page = 2)
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
      fun `should include future course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$tomorrow", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }

      @Test
      fun `should not include ended course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", endDate = "$yesterday", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner allocation in future`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow", programStatusCode = "WAIT")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }

      @Test
      fun `should not include if course activity created by DPS`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = true)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if there are no schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", createdByDps = false) {} // no schedule rules
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }

      @Test
      fun `should only include once if there are multiple schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", createdByDps = false) {
              courseScheduleRule(startTimeHours = 9, startTimeMinutes = 30, endTimeHours = 11, endTimeMinutes = 30)
              courseScheduleRule(startTimeHours = 13, startTimeMinutes = 30, endTimeHours = 15, endTimeMinutes = 30)
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
      }

      @Test
      fun `should only include the course activity requested`() {
        lateinit var otherCourseActivity: CourseActivity
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
            otherCourseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(otherCourseActivity.courseActivityId)
      }

      @Test
      fun `should ignore the course activity requested if it doesn't respect other filters`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(endDate = "$yesterday", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveActivities(courseActivityId = courseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner allocated in the last 6 months`() {
        val sixMonthsAgo = LocalDate.now().minusMonths(6)
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "${sixMonthsAgo.minusMonths(1)}",
                endDate = "$sixMonthsAgo",
                programStatusCode = "END",
              )
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }

      @Test
      fun `should not include if prisoner allocated more than 6 months ago`() {
        val sixMonthsAgo = LocalDate.now().minusMonths(6)
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$tomorrow", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = "${sixMonthsAgo.minusMonths(1)}",
                endDate = "${sixMonthsAgo.minusDays(1)}",
                programStatusCode = "END",
              )
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if prisoner waiting but never started`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(createdByDps = false)
          }
          offender {
            booking {
              courseAllocation(
                courseActivity = courseActivity,
                startDate = null,
                programStatusCode = "WAIT",
              )
            }
          }
        }

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)
      }
    }

    fun WebTestClient.getActiveActivities(
      pageSize: Int = 10,
      page: Int = 0,
      prison: String = "BXI",
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec = get().uri {
      it.path("/activities/ids")
        .queryParam("prisonId", prison)
        .queryParam("size", pageSize)
        .queryParam("page", page)
        .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }

  @Nested
  @DisplayName("GET /activities/rates-with-unknown-incentives")
  inner class GetPayRatesWithUnknownIncentives {
    private lateinit var courseActivity: CourseActivity

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/activities/rates-with-unknown-incentives?prisonId=ANY")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/activities/rates-with-unknown-incentives?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/activities/rates-with-unknown-incentives?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return activities with a pay rate for an inactive incentive level`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(iepLevelCode = "ENT", payBandCode = "5") // incentive level not active
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity)
          }
        }
      }

      webTestClient.getPayRatesWithUnknownIncentives()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
        .jsonPath("$[0].payBandCode").isEqualTo("5")
        .jsonPath("$[0].incentiveLevelCode").isEqualTo("ENT")
    }

    @Test
    fun `should ignore activities where the bad pay rate has expired`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(iepLevelCode = "ENT", endDate = "$yesterday") // inactive incentive level on expired pay rate
            payRate(iepLevelCode = "BAS", startDate = "$today")
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity)
          }
        }
      }

      webTestClient.getPayRatesWithUnknownIncentives()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should INCLUDE activities where an allocation ended LESS THAN 6 months ago`() {
      val sixMonthsAgo = LocalDate.now().minusMonths(6)
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(iepLevelCode = "ENT", payBandCode = "5") // incentive level not active
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "${sixMonthsAgo.minusMonths(1)}", endDate = "$sixMonthsAgo")
          }
        }
      }

      webTestClient.getPayRatesWithUnknownIncentives()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
        .jsonPath("$[0].payBandCode").isEqualTo("5")
        .jsonPath("$[0].incentiveLevelCode").isEqualTo("ENT")
    }

    @Test
    fun `should IGNORE activities an allocation ended MORE THAN 6 months ago`() {
      val sixMonthsAgo = LocalDate.now().minusMonths(6)
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(iepLevelCode = "ENT", payBandCode = "5") // incentive level not active
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "${sixMonthsAgo.minusMonths(1)}", endDate = "${sixMonthsAgo.minusDays(1)}")
          }
        }
      }

      webTestClient.getPayRatesWithUnknownIncentives()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    private fun WebTestClient.getPayRatesWithUnknownIncentives(
      prison: String = "BXI",
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec = get().uri {
      it.path("/activities/rates-with-unknown-incentives")
        .queryParam("prisonId", prison)
        .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }

  @Nested
  @DisplayName("GET /activities/without-schedule-rules")
  inner class GetActivitiesWithoutScheduleRules {
    private lateinit var courseActivity: CourseActivity

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/activities/without-schedule-rules?prisonId=ANY")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/activities/without-schedule-rules?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/activities/without-schedule-rules?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return active activities without schedule rules`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {}
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity)
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should return multiple active activities without schedule rules`() {
      lateinit var courseActivity2: CourseActivity
      lateinit var courseActivity3: CourseActivity

      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {}
          courseActivity2 = courseActivity(createdByDps = false) {}
          courseActivity3 = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
          }
        }
        listOf(courseActivity, courseActivity2, courseActivity3).forEach {
          offender {
            booking {
              courseAllocation(courseActivity = it)
            }
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$[*].courseActivityId").value<List<Int>> {
          assertThat(it).containsExactlyInAnyOrder(courseActivity.courseActivityId.toInt(), courseActivity2.courseActivityId.toInt())
        }
        .jsonPath("$[*].courseActivityDescription").value<List<String>> {
          assertThat(it).containsExactlyInAnyOrder(courseActivity.description!!, courseActivity2.description!!)
        }
    }

    @Test
    fun `should return single active activities without schedule rules`() {
      lateinit var courseActivity2: CourseActivity

      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {}
          courseActivity2 = courseActivity(createdByDps = false) {}
        }
        listOf(courseActivity, courseActivity2).forEach {
          offender {
            booking {
              courseAllocation(courseActivity = it)
            }
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules(courseActivityId = courseActivity.courseActivityId)
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should ignore inactive activities without schedule rules`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(endDate = "$yesterday", createdByDps = false)
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity)
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore active activities with schedule rules`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity)
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should ignore activities without allocations`() {
      nomisDataBuilder.build {
        programService {
          courseActivity(createdByDps = false) {
            courseSchedule()
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should INCLUDE activities with ended allocations LESS THAN 6 months old`() {
      val sixMonthsAgo = LocalDate.now().minusMonths(6)
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "${sixMonthsAgo.minusMonths(1)}", endDate = "$sixMonthsAgo")
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
    }

    @Test
    fun `should IGNORE activities with ended allocations MORE THAN 6 months old`() {
      val sixMonthsAgo = LocalDate.now().minusMonths(6)
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
          }
        }
        offender {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "${sixMonthsAgo.minusMonths(1)}", endDate = "${sixMonthsAgo.minusDays(1)}")
          }
        }
      }

      webTestClient.getActivitiesWithoutScheduleRules()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    private fun WebTestClient.getActivitiesWithoutScheduleRules(
      prison: String = "BXI",
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec = get().uri {
      it.path("/activities/without-schedule-rules")
        .queryParam("prisonId", prison)
        .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }

  @Nested
  @DisplayName("GET /activities/{courseActivityId}")
  inner class GetActivity {

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
              excludeBankHolidays = true,
              payPerSession = "F",
              outsideWork = true,
            )
          }
        }

        webTestClient.getActivityDetails()
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
          .jsonPath("minimumIncentiveLevel").doesNotExist()
          .jsonPath("excludeBankHolidays").isEqualTo(true)
          .jsonPath("payPerSession").isEqualTo("F")
          .jsonPath("outsideWork").isEqualTo("true")
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

        webTestClient.getActivityDetails()
          .expectBody()
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

        webTestClient.getActivityDetails()
          .expectBody()
          .jsonPath("scheduleRules[0].startTime").isEqualTo("09:30")
          .jsonPath("scheduleRules[0].endTime").isEqualTo("12:15")
          .jsonPath("scheduleRules[0].monday").isEqualTo(true)
          .jsonPath("scheduleRules[0].tuesday").isEqualTo(false)
          .jsonPath("scheduleRules[0].wednesday").isEqualTo(true)
          .jsonPath("scheduleRules[0].thursday").isEqualTo(false)
          .jsonPath("scheduleRules[0].friday").isEqualTo(true)
          .jsonPath("scheduleRules[0].saturday").isEqualTo(false)
          .jsonPath("scheduleRules[0].sunday").isEqualTo(false)
          .jsonPath("scheduleRules[0].slotCategoryCode").isEqualTo("AM")
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

        webTestClient.getActivityDetails()
          .expectBody()
          .jsonPath("scheduleRules[0].startTime").isEqualTo("09:30")
          .jsonPath("scheduleRules[0].endTime").isEqualTo("12:15")
          .jsonPath("scheduleRules[0].slotCategoryCode").isEqualTo("AM")
          .jsonPath("scheduleRules[1].startTime").isEqualTo("13:00")
          .jsonPath("scheduleRules[1].endTime").isEqualTo("16:30")
          .jsonPath("scheduleRules[1].slotCategoryCode").isEqualTo("PM")
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

        webTestClient.getActivityDetails()
          .expectBody()
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

        webTestClient.getActivityDetails()
          .expectBody()
          .jsonPath("payRates.size()").isEqualTo(1)
          .jsonPath("payRates[0].payBand").isEqualTo("2")
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

        webTestClient.getActivityDetails()
          .expectBody()
          .jsonPath("payRates[0].payBand").isEqualTo("1")
          .jsonPath("payRates[1].payBand").isEqualTo("2")
      }
    }

    private fun WebTestClient.getActivityDetails(): WebTestClient.ResponseSpec = get().uri("/activities/${courseActivity.courseActivityId}")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }
}
