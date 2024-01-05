package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalDate

class GetAllocationResourceIntTest : IntegrationTestBase() {

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
  @DisplayName("GET /allocations/ids")
  inner class FindActiveAllocations {

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/allocations/ids?prisonId=BXI")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/allocations/ids?prisonId=BXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/allocations/ids?prisonId=BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `invalid prison should return not found`() {
        webTestClient.get().uri("/allocations/ids?prisonId=XXX")
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
      private lateinit var courseAllocation: OffenderProgramProfile

      @Test
      fun `finds an active allocation`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content[0].allocationId").isEqualTo(courseAllocation.offenderProgramReferenceId)
      }

      @Test
      fun `finds a full page of allocations`() {
        val courseActivities = mutableListOf<CourseActivity>()
        val courseAllocations = mutableListOf<OffenderProgramProfile>()
        val pageSize = 3
        nomisDataBuilder.build {
          programService {
            repeat(pageSize + 1) {
              courseActivities += courseActivity(startDate = "$today")
            }
          }
          offender {
            booking {
              courseActivities.forEach {
                courseAllocations += courseAllocation(courseActivity = it, startDate = "$today")
              }
            }
          }
        }

        webTestClient.getActiveAllocations(pageSize = pageSize, page = 0)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)
          .jsonPath("content[0].allocationId").isEqualTo(courseAllocations[0].offenderProgramReferenceId)
          .jsonPath("content[1].allocationId").isEqualTo(courseAllocations[1].offenderProgramReferenceId)
          .jsonPath("content[2].allocationId").isEqualTo(courseAllocations[2].offenderProgramReferenceId)
      }

      @Test
      fun `finds the second page of allocations`() {
        val courseActivities = mutableListOf<CourseActivity>()
        val courseAllocations = mutableListOf<OffenderProgramProfile>()
        val pageSize = 3
        nomisDataBuilder.build {
          programService {
            repeat(pageSize + 1) {
              courseActivities += courseActivity(startDate = "$today")
            }
          }
          offender {
            booking {
              courseActivities.forEach {
                courseAllocations += courseAllocation(courseActivity = it, startDate = "$today")
              }
            }
          }
        }

        webTestClient.getActiveAllocations(pageSize = pageSize, page = 1)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].allocationId").isEqualTo(courseAllocations[3].offenderProgramReferenceId)
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

        webTestClient.getActiveAllocations(page = 0)
          .expectBody()
          .jsonPath("totalElements").isEqualTo(25)
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(10)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getActiveAllocations(page = 1)
          .expectBody()
          .jsonPath("numberOfElements").isEqualTo(10)
          .jsonPath("number").isEqualTo(1)
          .jsonPath("content.length()").isEqualTo(10)

        webTestClient.getActiveAllocations(page = 2)
          .expectBody()
          .jsonPath("numberOfElements").isEqualTo(5)
          .jsonPath("number").isEqualTo(2)
          .jsonPath("content.length()").isEqualTo(5)
      }
    }

    /*
     * Note that each test in this class also checks the endpoint `/activities/ids` and `/allocations/suspended` as well
     * as the allocation endpoint.
     *
     * This is to test that the same rules are being applied to the activities, suspended activities and allocations
     * selection and effectively tests that the custom queries in CourseActivityRepository.findActiveActivities,
     * OffenderProgramProfilesRepository.findActiveAllocations / findSuspendedAllocations are aligned.
     */
    @Nested
    inner class AllocationSelection {

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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include activities with no prisoners`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", programStatusCode = "END", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$yesterday", endDate = "$yesterday", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner allocation in future`() {
        lateinit var courseAllocation: OffenderProgramProfile
        lateinit var courseAllocation2: OffenderProgramProfile
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow")
            }
          }
          offender(nomsId = "B1234BB") {
            booking {
              courseAllocation2 = courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(2)
          .jsonPath("content[0].allocationId").isEqualTo(courseAllocation.offenderProgramReferenceId)
          .jsonPath("content[1].allocationId").isEqualTo(courseAllocation2.offenderProgramReferenceId)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
      }

      @Test
      fun `should not include if prisoner active in different prison`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(prisonId = "BXI", startDate = "$today")
          }
          offender {
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender {
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
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
          offender {
            booking(active = false) {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner is ACTIVE OUT`() {
        lateinit var courseAllocation: OffenderProgramProfile
        lateinit var courseAllocation2: OffenderProgramProfile
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender(nomsId = "B1234BB") {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocation2 = courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(2)
          .jsonPath("content[0].allocationId").isEqualTo(courseAllocation.offenderProgramReferenceId)
          .jsonPath("content[1].allocationId").isEqualTo(courseAllocation2.offenderProgramReferenceId)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
      }

      @Test
      fun `should not include if course activity not in program`() {
        nomisDataBuilder.build {
          programService("INTTEST") {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          programService("ANOTHER_PROGRAM") {
            courseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations(excludeProgramCodes = listOf("INTTEST", "ANOTHER_PROGRAM"))
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities(excludeProgramCodes = listOf("INTTEST", "ANOTHER_PROGRAM"))
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations(excludeProgramCodes = listOf("INTTEST", "ANOTHER_PROGRAM"))
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if there are no course schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday") {} // no schedule rules
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should only include once if there are multiple course schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday") {
              courseScheduleRule(startTimeHours = 9, startTimeMinutes = 30, endTimeHours = 11, endTimeMinutes = 30)
              courseScheduleRule(startTimeHours = 13, startTimeMinutes = 30, endTimeHours = 15, endTimeMinutes = 30)
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender(nomsId = "B1234BB") {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(2)

        webTestClient.getActiveActivities()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
      }

      @Test
      fun `should only include the course activity requested`() {
        lateinit var otherCourseActivity: CourseActivity
        lateinit var otherCourseAllocation: OffenderProgramProfile
        lateinit var otherCourseAllocation2: OffenderProgramProfile
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
            otherCourseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              otherCourseAllocation = courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
          offender(nomsId = "B1234BB") {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
              otherCourseAllocation2 = courseAllocation(courseActivity = otherCourseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(2)
          .jsonPath("content[0].allocationId").isEqualTo(otherCourseAllocation.offenderProgramReferenceId)
          .jsonPath("content[1].allocationId").isEqualTo(otherCourseAllocation2.offenderProgramReferenceId)

        webTestClient.getActiveActivities(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].courseActivityId").isEqualTo(otherCourseActivity.courseActivityId)

        webTestClient.getSuspendedAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
          .jsonPath("$[0].courseActivityId").isEqualTo(otherCourseActivity.courseActivityId)
      }

      @Test
      fun `should ignore the course activity requested if it doesn't respect other filters`() {
        lateinit var otherCourseActivity: CourseActivity
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today")
            otherCourseActivity = courseActivity(startDate = "$today")
          }
          offender {
            booking(agencyLocationId = "LEI") { // wrong prison
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
          offender {
            booking(agencyLocationId = "LEI") { // wrong prison
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today", suspended = true)
            }
          }
        }

        webTestClient.getActiveAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getActiveActivities(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }
    }

    fun WebTestClient.getActiveAllocations(
      pageSize: Int = 10,
      page: Int = 0,
      prison: String = "BXI",
      excludeProgramCodes: List<String> = listOf(),
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec =
      get().uri {
        it.path("/allocations/ids")
          .queryParam("prisonId", prison)
          .queryParam("size", pageSize)
          .queryParam("page", page)
          .queryParams(LinkedMultiValueMap<String, String>().apply { addAll("excludeProgramCode", excludeProgramCodes) })
          .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk

    fun WebTestClient.getActiveActivities(
      pageSize: Int = 10,
      page: Int = 0,
      prison: String = "BXI",
      excludeProgramCodes: List<String> = listOf(),
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec =
      get().uri {
        it.path("/activities/ids")
          .queryParam("prisonId", prison)
          .queryParam("size", pageSize)
          .queryParam("page", page)
          .queryParams(LinkedMultiValueMap<String, String>().apply { addAll("excludeProgramCode", excludeProgramCodes) })
          .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
  }

  /*
   * This endpoint is closely related to `GET /allocations/ids` and any changes to the allocation selection rules should
   * affect. For this reason I've added a suspended allocation to each of the tests in the `AllocationSelection` nested
   * class above to further test this endpoint.
   */
  @Nested
  @DisplayName("GET /allocations/suspended")
  inner class FindSuspendedAllocations {
    private lateinit var courseActivity: CourseActivity

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/allocations/suspended?prisonId=ANY")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/allocations/suspended?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/allocations/suspended?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should only return suspended prisoners`() {
      lateinit var courseActivity2: CourseActivity
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(startDate = "$today")
          courseActivity2 = courseActivity(startDate = "$today")
        }
        offender(nomsId = "A1234AA") {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            courseAllocation(courseActivity = courseActivity2, startDate = "$today", suspended = false)
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = false)
            courseAllocation(courseActivity = courseActivity2, startDate = "$today", suspended = true)
          }
        }
      }

      webTestClient.getSuspendedAllocations()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].offenderNo").isEqualTo("A1234AA")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description)
        .jsonPath("$[1].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[1].courseActivityId").isEqualTo(courseActivity2.courseActivityId)
        .jsonPath("$[1].courseActivityDescription").isEqualTo(courseActivity2.description)
    }
  }

  private fun WebTestClient.getSuspendedAllocations(
    prison: String = "BXI",
    excludeProgramCodes: List<String> = listOf(),
    courseActivityId: Long? = null,
  ): WebTestClient.ResponseSpec =
    get().uri {
      it.path("/allocations/suspended")
        .queryParam("prisonId", prison)
        .queryParams(LinkedMultiValueMap<String, String>().apply { addAll("excludeProgramCode", excludeProgramCodes) })
        .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk

  @Nested
  @DisplayName("GET /allocations/{allocationId}")
  inner class GetActiveAllocation {

    private lateinit var courseActivity: CourseActivity
    private lateinit var courseAllocation: OffenderProgramProfile

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/allocations/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/allocations/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/allocations/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `unknown course allocation should return not found`() {
        webTestClient.get().uri("/allocations/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender program profile with id=9999 does not exist")
          }
      }
    }

    @Nested
    inner class AllocationDetails {

      private lateinit var offenderBooking: OffenderBooking

      @Test
      fun `should return all allocation details`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1234AA") {
            offenderBooking = booking(livingUnitId = -3009) {
              courseAllocation = courseAllocation(
                courseActivity = courseActivity,
                startDate = "$yesterday",
                programStatusCode = "ALLOC",
                endDate = "$tomorrow",
                endReasonCode = "WDRAWN",
                endComment = "Withdrawn",
                suspended = false,
              ) {
                payBand(payBandCode = "1")
              }
            }
          }
        }

        webTestClient.getAllocationDetails()
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("prisonId").isEqualTo("BXI")
          .jsonPath("nomisId").isEqualTo("A1234AA")
          .jsonPath("bookingId").isEqualTo(offenderBooking.bookingId)
          .jsonPath("startDate").isEqualTo("$yesterday")
          .jsonPath("endDate").isEqualTo("$tomorrow")
          .jsonPath("endReasonCode").isEqualTo("WDRAWN")
          .jsonPath("endComment").isEqualTo("Withdrawn")
          .jsonPath("suspended").isEqualTo(false)
          .jsonPath("payBand").isEqualTo("1")
          .jsonPath("livingUnitDescription").isEqualTo("BXI-A-1-016")
          .jsonPath("exclusions").isEmpty
      }

      @Test
      fun `should return allocation exclusions`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender {
            offenderBooking = booking {
              courseAllocation = courseAllocation(courseActivity) {
                exclusion(SlotCategory.AM, WeekDay.MON)
                exclusion(null, WeekDay.TUE)
              }
            }
          }
        }

        webTestClient.getAllocationDetails()
          .expectBody()
          .jsonPath("courseActivityId").isEqualTo(courseActivity.courseActivityId)
          .jsonPath("exclusions[0].slot").isEqualTo("AM")
          .jsonPath("exclusions[0].day").isEqualTo("MON")
          .jsonPath("exclusions[1].slot").doesNotExist()
          .jsonPath("exclusions[1].day").isEqualTo("TUE")
      }

      @Test
      fun `should only include active allocation pay bands`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity) {
                payBand(startDate = "$yesterday", endDate = "$yesterday", payBandCode = "5")
                payBand(startDate = "$today", endDate = null, payBandCode = "6")
              }
            }
          }
        }

        webTestClient.getAllocationDetails()
          .expectBody()
          .jsonPath("payBand").isEqualTo("6")
      }

      @Test
      fun `should allow missing pay bands`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity()
          }
          offender(nomsId = "A1111AA") {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity) {} // no pay bands
            }
          }
        }

        webTestClient.getAllocationDetails()
          .expectBody()
          .jsonPath("nomisId").isEqualTo("A1111AA")
          .jsonPath("payBand").doesNotExist()
      }

      @Test
      fun `should return all schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              courseSchedule()
              payRate()
              courseScheduleRule(startTimeHours = 9, startTimeMinutes = 30, endTimeHours = 11, endTimeMinutes = 30)
              courseScheduleRule(startTimeHours = 14, startTimeMinutes = 0, endTimeHours = 17, endTimeMinutes = 0)
            }
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity)
            }
          }
        }

        webTestClient.getAllocationDetails()
          .expectBody()
          .jsonPath("scheduleRules[0].startTime").isEqualTo("09:30")
          .jsonPath("scheduleRules[0].endTime").isEqualTo("11:30")
          .jsonPath("scheduleRules[0].monday").isEqualTo(true)
          .jsonPath("scheduleRules[0].tuesday").isEqualTo(true)
          .jsonPath("scheduleRules[0].wednesday").isEqualTo(true)
          .jsonPath("scheduleRules[0].thursday").isEqualTo(true)
          .jsonPath("scheduleRules[0].friday").isEqualTo(true)
          .jsonPath("scheduleRules[0].saturday").isEqualTo(false)
          .jsonPath("scheduleRules[0].sunday").isEqualTo(false)
          .jsonPath("scheduleRules[1].startTime").isEqualTo("14:00")
          .jsonPath("scheduleRules[1].endTime").isEqualTo("17:00")
          .jsonPath("scheduleRules[1].monday").isEqualTo(true)
          .jsonPath("scheduleRules[1].tuesday").isEqualTo(true)
          .jsonPath("scheduleRules[1].wednesday").isEqualTo(true)
          .jsonPath("scheduleRules[1].thursday").isEqualTo(true)
          .jsonPath("scheduleRules[1].friday").isEqualTo(true)
          .jsonPath("scheduleRules[1].saturday").isEqualTo(false)
          .jsonPath("scheduleRules[1].sunday").isEqualTo(false)
      }
    }

    private fun WebTestClient.getAllocationDetails(): WebTestClient.ResponseSpec =
      get().uri("/allocations/${courseAllocation.offenderProgramReferenceId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
  }
}
