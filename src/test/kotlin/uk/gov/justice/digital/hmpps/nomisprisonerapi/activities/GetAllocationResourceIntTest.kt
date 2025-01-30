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
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
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
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
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
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
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
              courseActivities += courseActivity(startDate = "$today", createdByDps = false)
            }
          }
          courseActivities.forEachIndexed { index, activity ->
            offender {
              booking {
                // give half of the activities active allocations
                if (index % 2 == 0) {
                  courseAllocation(courseActivity = activity, startDate = "$today")
                } else {
                  // and half of the activities inactive allocations
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
     * Note that each test in this class also checks the endpoints `/allocations/ids`, `/allocations/suspended` and
     * `/allocations/missing-pay-bands`.
     *
     * This is to test that the same rules are being applied to active allocations, suspended allocations and allocations
     * with missing pay rates and effectively tests that the following custom queries are aligned:
     * - OffenderProgramProfilesRepository.findActiveAllocations
     * - OffenderProgramProfilesRepository.findSuspendedAllocations
     * - OffenderProgramProfilesRepository.findMissingPayBands
     */
    @Nested
    inner class AllocationSelection {

      private lateinit var courseActivity: CourseActivity
      lateinit var courseAllocation: OffenderProgramProfile
      lateinit var courseAllocationSuspended: OffenderProgramProfile
      lateinit var courseAllocationMissingPayBand: OffenderProgramProfile
      val nomsIdSuspended = "S1234SS"
      val nomsIdMissingPayBand = "P1234PP"

      @Test
      fun `should include future course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$tomorrow", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender(nomsId = nomsIdSuspended) {
            booking {
              courseAllocationSuspended = courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          offender(nomsId = nomsIdMissingPayBand) {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocationMissingPayBand = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdSuspended)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdMissingPayBand)
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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include activities with no prisoners`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if prisoner allocation has ended status`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
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
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today", programStatusCode = "END")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if prisoner allocation past end date`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
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
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$yesterday", endDate = "$yesterday")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner allocation in future`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            booking {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow")
            }
          }
          offender(nomsId = nomsIdSuspended) {
            booking {
              courseAllocationSuspended = courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow", suspended = true)
            }
          }
          offender(nomsId = nomsIdMissingPayBand) {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocationMissingPayBand = courseAllocation(courseActivity = courseActivity, startDate = "$tomorrow")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdSuspended)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdMissingPayBand)
      }

      @Test
      fun `should not include if prisoner active in different prison`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(prisonId = "BXI", startDate = "$today", createdByDps = false)
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
          offender {
            booking(agencyLocationId = "LEI") {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if prisoner is inactive`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
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
          offender {
            booking(active = false) {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should include if prisoner is ACTIVE OUT`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocation = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender(nomsId = nomsIdSuspended) {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocationSuspended = courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          offender(nomsId = nomsIdMissingPayBand) {
            booking(active = true, inOutStatus = "OUT") {
              incentive(iepLevelCode = "BAS")
              courseAllocationMissingPayBand = courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdSuspended)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdMissingPayBand)
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
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = true)
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
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should not include if there are no course schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", createdByDps = false) {} // no schedule rules
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
          offender {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }

      @Test
      fun `should only include once if there are multiple course schedule rules`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$yesterday", createdByDps = false) {
              courseScheduleRule(startTimeHours = 9, startTimeMinutes = 30, endTimeHours = 11, endTimeMinutes = 30)
              courseScheduleRule(startTimeHours = 13, startTimeMinutes = 30, endTimeHours = 15, endTimeMinutes = 30)
              payRate()
            }
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
          offender(nomsId = nomsIdSuspended) {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
            }
          }
          offender(nomsId = nomsIdMissingPayBand) {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations()
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdSuspended)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdMissingPayBand)
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
          offender(nomsId = nomsIdSuspended) {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today", suspended = true)
            }
          }
          offender(nomsId = nomsIdMissingPayBand) {
            booking {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(3)

        webTestClient.getSuspendedAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdSuspended)
          .jsonPath("$[0].courseActivityId").isEqualTo(otherCourseActivity.courseActivityId)

        webTestClient.getAllocationsWithMissingPayBands(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].offenderNo").isEqualTo(nomsIdMissingPayBand)
          .jsonPath("$[0].courseActivityId").isEqualTo(otherCourseActivity.courseActivityId)
      }

      @Test
      fun `should ignore the course activity requested if it doesn't respect other filters`() {
        lateinit var otherCourseActivity: CourseActivity

        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = "$today", createdByDps = false)
            otherCourseActivity = courseActivity(startDate = "$today", createdByDps = false)
          }
          offender {
            // wrong prison
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
          offender {
            // wrong prison
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = "$today", suspended = true)
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today", suspended = true)
            }
          }
          offender {
            // wrong prison
            booking(agencyLocationId = "LEI") {
              incentive(iepLevelCode = "BAS")
              courseAllocation(courseActivity = courseActivity, startDate = "$today")
              courseAllocation(courseActivity = otherCourseActivity, startDate = "$today")
            }
          }
        }

        webTestClient.getActiveAllocations(courseActivityId = otherCourseActivity.courseActivityId)
          .expectBody()
          .jsonPath("content.size()").isEqualTo(0)

        webTestClient.getSuspendedAllocations()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)

        webTestClient.getAllocationsWithMissingPayBands()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(0)
      }
    }

    fun WebTestClient.getActiveAllocations(
      pageSize: Int = 10,
      page: Int = 0,
      prison: String = "BXI",
      courseActivityId: Long? = null,
    ): WebTestClient.ResponseSpec = get().uri {
      it.path("/allocations/ids")
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
          courseActivity = courseActivity(startDate = "$today", createdByDps = false)
          courseActivity2 = courseActivity(startDate = "$today", createdByDps = false)
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
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
        .jsonPath("$[1].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[1].courseActivityId").isEqualTo(courseActivity2.courseActivityId)
        .jsonPath("$[1].courseActivityDescription").isEqualTo(courseActivity2.description!!)
    }
  }

  private fun WebTestClient.getSuspendedAllocations(
    prison: String = "BXI",
    courseActivityId: Long? = null,
  ): WebTestClient.ResponseSpec = get().uri {
    it.path("/allocations/suspended")
      .queryParam("prisonId", prison)
      .apply { courseActivityId?.run { queryParam("courseActivityId", courseActivityId) } }
      .build()
  }
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
    .exchange()
    .expectStatus().isOk

  /*
   * This endpoint is closely related to `GET /allocations/ids` and any changes to the allocation selection rules should
   * affect it. For this reason I've added an allocation with a missing pay band to each of the tests in the `AllocationSelection`
   * nested class above to further test this endpoint.
   */
  @Nested
  @DisplayName("GET /allocations/missing-pay-bands")
  inner class FindAllocationsWithNoPayRates {
    private lateinit var courseActivity: CourseActivity

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/allocations/missing-pay-bands?prisonId=ANY")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/allocations/missing-pay-bands?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/allocations/missing-pay-bands?prisonId=ANY")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return allocations with a missing pay band`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(payBandCode = "5", iepLevelCode = "STD")
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "6") // wrong pay band
            }
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[0].incentiveLevel").isEqualTo("STD")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should return allocations with correct pay band on wrong incentive level`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(payBandCode = "5", iepLevelCode = "STD")
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            incentive(iepLevelCode = "BAS") // wrong incentive level
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[0].incentiveLevel").isEqualTo("BAS")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should not return allocations with match on expired pay band`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(payBandCode = "5", iepLevelCode = "STD")
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5", endDate = "$yesterday") // matching pay band expired
              payBand(payBandCode = "6", startDate = "$today")
            }
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[0].incentiveLevel").isEqualTo("STD")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should not return allocations with match on expired pay rate`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(payBandCode = "5", iepLevelCode = "STD", endDate = "$yesterday")
            payRate(payBandCode = "6", iepLevelCode = "STD", startDate = "$today")
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "6")
            }
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5") // matching pay rate expired
            }
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[0].incentiveLevel").isEqualTo("STD")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }

    @Test
    fun `should not return allocations where course activity has no pay rates`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            // no pay rates
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            courseAllocation(courseActivity = courseActivity) {} // no pay bands
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should return allocations where matching incentive level not the latest`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(createdByDps = false) {
            courseSchedule()
            courseScheduleRule()
            payRate(payBandCode = "5", iepLevelCode = "STD")
          }
        }
        offender(nomsId = "A1234AA") {
          booking {
            incentive(iepLevelCode = "STD")
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
        offender(nomsId = "B1234BB") {
          booking {
            incentive(iepLevelCode = "STD", iepDateTime = yesterday.atStartOfDay(), sequence = 1) // matching incentive level not the latest
            incentive(iepLevelCode = "BAS", iepDateTime = today.atStartOfDay(), sequence = 2)
            courseAllocation(courseActivity = courseActivity) {
              payBand(payBandCode = "5")
            }
          }
        }
      }

      webTestClient.getAllocationsWithMissingPayBands()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].offenderNo").isEqualTo("B1234BB")
        .jsonPath("$[0].incentiveLevel").isEqualTo("BAS")
        .jsonPath("$[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
        .jsonPath("$[0].courseActivityDescription").isEqualTo(courseActivity.description!!)
    }
  }

  private fun WebTestClient.getAllocationsWithMissingPayBands(
    prison: String = "BXI",
    courseActivityId: Long? = null,
  ): WebTestClient.ResponseSpec = get().uri {
    it.path("/allocations/missing-pay-bands")
      .queryParam("prisonId", prison)
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
          .jsonPath("activityStartDate").isEqualTo("${courseActivity.scheduleStartDate}")
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

    private fun WebTestClient.getAllocationDetails(): WebTestClient.ResponseSpec = get().uri("/allocations/${courseAllocation.offenderProgramReferenceId}")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }
}
