@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDate

class MigrationResourceIntTest : IntegrationTestBase() {

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
  @DisplayName("GET /activities/migrate")
  inner class FindMigrationActivities {

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/activities/migrate/BXI")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `invalid prison should return not found`() {
        webTestClient.get().uri("/activities/migrate/XXX")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isNotFound
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
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            offenderBooking = booking {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
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
              courseActivities += courseActivity(startDate = today.toString())
            }
          }
          offender {
            offenderBooking = booking {
              courseActivities.forEach {
                courseAllocation(courseActivity = it, startDate = today.toString())
              }
            }
          }
        }

        webTestClient.get().uri {
          it.path("/activities/migrate/BXI").queryParam("size", pageSize).build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
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
              courseActivities += courseActivity(startDate = today.toString())
            }
          }
          offender {
            offenderBooking = booking {
              courseActivities.forEach {
                courseAllocation(courseActivity = it, startDate = today.toString())
              }
            }
          }
        }

        webTestClient.get().uri {
          it.path("/activities/migrate/BXI")
            .queryParam("size", pageSize)
            .queryParam("page", 1)
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivities[3].courseActivityId)
          .jsonPath("content[1].courseActivityId").doesNotExist()
      }
    }

    @Nested
    inner class ActivitySelection {

      private lateinit var courseActivity: CourseActivity

      @Test
      fun `should not include future course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = tomorrow.toString())
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include ended course activities`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = yesterday.toString(), endDate = yesterday.toString())
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include activities with no prisoners`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner not allocated`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString(), programStatusCode = "END")
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner allocation ended`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = yesterday.toString(), endDate = yesterday.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner allocation in future`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking {
              courseAllocation(courseActivity = courseActivity, startDate = tomorrow.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoner active in different prison`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking(agencyLocationId = "LEI") {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should not include if prisoners who are inactive`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking(active = false) {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").doesNotExist()
      }

      @Test
      fun `should include if prisoner is ACTIVE OUT`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity(startDate = today.toString())
          }
          offender {
            booking(active = true, inOutStatus = "OUT") {
              courseAllocation(courseActivity = courseActivity, startDate = today.toString())
            }
          }
        }

        webTestClient.get().uri("/activities/migrate/BXI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].courseActivityId").isEqualTo(courseActivity.courseActivityId)
      }
    }
  }
}
