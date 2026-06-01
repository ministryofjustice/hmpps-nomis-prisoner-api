package uk.gov.justice.digital.hmpps.nomisprisonerapi.staff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload.Companion.DPS_CASELOAD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import java.time.LocalDateTime

class StaffResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var staffRepository: StaffRepository

  @Autowired
  private lateinit var rolesRepository: RoleRepository

  @TestInstance(PER_CLASS)
  @Nested
  @DisplayName("GET /staff/{staffId}")
  inner class GetStaffDetails {

    private lateinit var staff1: Staff
    private lateinit var staff2: Staff
    private lateinit var role1: Role
    private lateinit var role2: Role
    private lateinit var role3: Role

    @BeforeAll
    fun setup() {
      nomisDataBuilder.build {
        role1 = role(
          code = "DPS_CODE_1",
          name = "This is Dps test role 1",
          userAccountType = "GENERAL",
        )
        role2 = role(
          code = "DPS_CODE_2",
          name = "This is Dps test role 2",
          userAccountType = "ADMIN",
        )
        role3 = role(
          code = "NOMIS_CODE_1",
          name = "This is Nomis test role 1",
          userAccountType = "ADMIN",
        )
        role(
          code = "DPS_CODE_3",
          name = "This is Dps test role 3",
          userAccountType = "ADMIN",
        )
        staff1 = staff(firstName = "JIM", lastName = "STAFFA") {
          email(emailAddress = "jim.staffa@justice.gov.uk")
          // TODO add web address so that it goes into INTERNET ADDRESSES
          account(username = "JIIMSTAFFA_GEN", activeCaseloadId = "MDI", lastLoggedIn = LocalDateTime.parse("2026-03-17T12:30"))
          account(username = "JIIMSTAFFA_ADM", type = ADMIN) {
            userCaseload(caseloadId = "MDI") {
              userCaseloadRole(role = role3)
            }
            userCaseload(caseloadId = "LEI") {
              userCaseloadRole(role = role3)
            }
            userCaseload(caseloadId = "NWEB") {
              userCaseloadRole(role = role1)
              userCaseloadRole(role = role2)
            }
          }
        }

        staff2 = staff(firstName = "JOE", lastName = "STAFFB") {
          account(username = "JOESTAFFB")
        }
      }
    }

    @AfterAll
    fun tearDown() {
      repository.delete(staff1)
      repository.delete(staff2)
      rolesRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/staff/${staff1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/staff/${staff1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/staff/${staff1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown staff should return not found`() {
      webTestClient.get().uri("/staff/-99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Staff with id=-99999 does not exist")
        }
    }

    @Test
    fun `will return staff details`() {
      webTestClient.get().uri("/staff/${staff1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(staff1.id)
        .jsonPath("firstName").isEqualTo("JIM")
        .jsonPath("lastName").isEqualTo("STAFFA")
        .jsonPath("email").isEqualTo("jim.staffa@justice.gov.uk")
        .jsonPath("status").isEqualTo("ACTIVE")
        .jsonPath("audit.createDatetime").isNotEmpty
        .jsonPath("audit.createUsername").isEqualTo("SA")
    }

    @Test
    fun `will return staff details with no email`() {
      webTestClient.get().uri("/staff/${staff2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(staff2.id)
        .jsonPath("firstName").isEqualTo("JOE")
        .jsonPath("lastName").isEqualTo("STAFFB")
        .jsonPath("email").doesNotExist()
        .jsonPath("status").isEqualTo("ACTIVE")
        .jsonPath("audit.createDatetime").isNotEmpty
        .jsonPath("audit.createUsername").isEqualTo("SA")
    }

    @Test
    fun `will return a staff user's account details`() {
      val staffDetails = webTestClient.get().uri("/staff/${staff1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody<StaffDetails>()
        .returnResult()
        .responseBody!!

      with(staffDetails) {
        assertThat(id).isEqualTo(staff1.id)
        assertThat(accounts.size).isEqualTo(2)
        with(accounts[0]) {
          assertThat(username).isEqualTo("JIIMSTAFFA_GEN")
          assertThat(sourceCode).isEqualTo("USER")
          assertThat(status).isEqualTo("")
          assertThat(typeCode).isEqualTo("GENERAL")
          assertThat(activeCaseloadId).isEqualTo("MDI")
          assertThat(caseloads.size).isEqualTo(0)
          assertThat(lastLoggedIn).isEqualTo("2026-03-17T12:30:00")
          assertThat(audit.createDatetime).isNotNull
          assertThat(audit.createUsername).isEqualTo("SA")
        }

        with(accounts[1]) {
          assertThat(typeCode).isEqualTo("ADMIN")
          assertThat(caseloads.size).isEqualTo(3)
          with(caseloads[0]) {
            assertThat(caseloadId).isEqualTo("LEI")
            assertThat(audit.createDatetime).isNotNull
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(roles.size).isEqualTo(1)
            assertThat(roles[0].code).isEqualTo("NOMIS_CODE_1")
            assertThat(roles[0].name).isEqualTo("This is Nomis test role 1")
            assertThat(roles[0].audit.createDatetime).isNotNull
            assertThat(roles[0].audit.createUsername).isEqualTo("SA")
          }
          with(caseloads[1]) {
            assertThat(caseloadId).isEqualTo("MDI")
            assertThat(audit.createDatetime).isNotNull
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(roles.size).isEqualTo(1)
            assertThat(roles[0].code).isEqualTo("NOMIS_CODE_1")
            assertThat(roles[0].name).isEqualTo("This is Nomis test role 1")
            assertThat(roles[0].audit.createDatetime).isNotNull
            assertThat(roles[0].audit.createUsername).isEqualTo("SA")
          }
          with(caseloads[2]) {
            assertThat(caseloadId).isEqualTo(DPS_CASELOAD)
            assertThat(audit.createDatetime).isNotNull
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(roles.size).isEqualTo(2)
            assertThat(roles[0].code).isEqualTo("DPS_CODE_1")
            assertThat(roles[0].name).isEqualTo("This is Dps test role 1")
            assertThat(roles[0].audit.createDatetime).isNotNull
            assertThat(roles[0].audit.createUsername).isEqualTo("SA")
            assertThat(roles[1].code).isEqualTo("DPS_CODE_2")
            assertThat(roles[1].name).isEqualTo("This is Dps test role 2")
            assertThat(roles[1].audit.createDatetime).isNotNull
            assertThat(roles[1].audit.createUsername).isEqualTo("SA")
          }
          assertThat(lastLoggedIn).isNull()
          assertThat(audit.createDatetime).isNotNull
          assertThat(audit.createUsername).isEqualTo("SA")
        }
      }
    }

    @Test
    fun `will return a staff user's details with dps roles only`() {
      webTestClient.get().uri("/staff/${staff1.id}?dpsRolesOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("accounts[1].caseloads[*].caseloadId").value<List<String>>
        { assertThat(it).containsExactlyElementsOf(listOf("LEI", "MDI", "NWEB")) }
        .jsonPath("accounts[1].caseloads[0].caseloadId").isEqualTo("LEI")
        .jsonPath("accounts[1].caseloads[0].roles.size()").isEqualTo(0)
        .jsonPath("accounts[1].caseloads[1].caseloadId").isEqualTo("MDI")
        .jsonPath("accounts[1].caseloads[1].roles.size()").isEqualTo(0)
        .jsonPath("accounts[1].caseloads[2].caseloadId").isEqualTo(DPS_CASELOAD)
        .jsonPath("accounts[1].caseloads[2].roles.size()").isEqualTo(2)
        .jsonPath("accounts[1].caseloads[2].roles[0].code").isEqualTo("DPS_CODE_1")
        .jsonPath("accounts[1].caseloads[2].roles[1].code").isEqualTo("DPS_CODE_2")
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  @DisplayName("GET /staff/ids")
  inner class GetStaffIds {
    lateinit var staffIds: MutableList<Long>

    @BeforeAll
    fun deleteExistingStaffApartFromPrisonUser() {
      val staff = repository.staffRepository.findAll().filter { it.id != -1L }
      staffRepository.deleteAll(staff)

      nomisDataBuilder.build {
        staffIds = (1..32).map { staff(firstName = "John", lastName = "Smith").id }.toMutableList()
      }
    }

    @AfterAll
    fun tearDown() {
      staffRepository.deleteAllById(staffIds)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/staff/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/staff/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/staff/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 staff`() {
        webTestClient.get().uri {
          it.path("/staff/ids")
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
          it.path("/staff/ids")
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
      fun `id just contains staff id`() {
        webTestClient.get().uri {
          it.path("/staff/ids")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(33)
          .jsonPath("content.size()").isEqualTo(2)
          .jsonPath("content[0].staffId").isEqualTo(-1)
          .jsonPath("content[1].staffId").isEqualTo(staffIds[0])
      }
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  @DisplayName("GET /staff/ids/all-from-id")
  inner class GetStaffIdsFromId {
    lateinit var staffIds: MutableList<Long>

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        staffIds = (1..30).map { staff(firstName = "John", lastName = "Smith").id }.toMutableList()
      }
    }

    @AfterAll
    fun tearDown() {
      staffRepository.deleteAllById(staffIds)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/staff/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/staff/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/staff/ids/all-from-id")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `by default will return first 20 staff ids`() {
        webTestClient.get().uri {
          it.path("/staff/ids/all-from-id")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/staff/ids/all-from-id")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("ids.size()").isEqualTo(1)
      }

      @Test
      fun `id just contains staff id`() {
        val pageResponse: StaffIdsPage = webTestClient.get().uri {
          it.path("/staff/ids/all-from-id")
            .queryParam("size", "2")
            .queryParam("staffId", staffIds[0] - 1)
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(pageResponse.ids).hasSize(2)
        assertThat(pageResponse.ids[0].staffId).isEqualTo(staffIds[0])
        assertThat(pageResponse.ids[1].staffId).isEqualTo(staffIds[1])
      }
    }
  }
}
