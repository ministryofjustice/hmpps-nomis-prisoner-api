package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository
import java.time.LocalDateTime

class UserResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var rolesRepository: RoleRepository
  private lateinit var staff1: Staff
  private lateinit var staff2: Staff
  private lateinit var role1: Role
  private lateinit var role2: Role
  private lateinit var role3: Role

  @BeforeEach
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

  @AfterEach
  fun tearDown() {
    repository.delete(staff1)
    repository.delete(staff2)
    rolesRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /users/{staffUserId}")
  inner class GetUser {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown user should return not found`() {
      webTestClient.get().uri("/users/-99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Staff User with id=-99999 does not exist")
        }
    }

    @Test
    fun `will return user details`() {
      webTestClient.get().uri("/users/${staff1.id}")
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
    fun `will return user details with no email`() {
      webTestClient.get().uri("/users/${staff2.id}")
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
    fun `will return a user's account details`() {
      val userDetails = webTestClient.get().uri("/users/${staff1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody<UserDetails>()
        .returnResult()
        .responseBody!!

      with(userDetails) {
        assertThat(id).isEqualTo(staff1.id)
        assertThat(accounts.size).isEqualTo(2)
        with(accounts[0]) {
          assertThat(username).isEqualTo("JIIMSTAFFA_GEN")
          assertThat(sourceCode).isEqualTo("USER")
          assertThat(status).isEqualTo("")
          assertThat(typeCode).isEqualTo("GENERAL")
          assertThat(activeCaseloadId).isEqualTo("MDI")
          assertThat(lastLoggedIn).isEqualTo("2026-03-17T12:30:00")
          assertThat(caseloads.size).isEqualTo(0)
          assertThat(roles.size).isEqualTo(0)
          assertThat(audit.createDatetime).isNotNull
          assertThat(audit.createUsername).isEqualTo("SA")
        }
        with(accounts[1]) {
          assertThat(typeCode).isEqualTo("ADMIN")
          assertThat(lastLoggedIn).isNull()
          assertThat(caseloads.size).isEqualTo(3)
          assertThat(caseloads).containsExactly("LEI", "MDI", "NWEB")
          assertThat(roles.size).isEqualTo(3)
          assertThat(roles).contains("DPS_CODE_1", "DPS_CODE_2", "NOMIS_CODE_1")
          assertThat(audit.createDatetime).isNotNull
          assertThat(audit.createUsername).isEqualTo("SA")
        }
      }
    }

    @Test
    fun `will return a user's details with dps roles only`() {
      webTestClient.get().uri("/users/${staff1.id}?dpsRolesOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("accounts[1].caseloads").value<List<String>>
        { assertThat(it).containsExactlyElementsOf(listOf("LEI", "MDI", "NWEB")) }
        .jsonPath("accounts[1].roles[0]").isEqualTo("DPS_CODE_1")
        .jsonPath("accounts[1].roles.size()").isEqualTo(2)
        .jsonPath("accounts[1].roles[1]").isEqualTo("DPS_CODE_2")
    }
  }
}
