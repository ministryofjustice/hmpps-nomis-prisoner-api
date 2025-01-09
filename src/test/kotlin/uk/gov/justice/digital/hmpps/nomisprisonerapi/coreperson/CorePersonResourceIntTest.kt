package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CorePersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /core-person/{prisonNumber}")
  @Nested
  inner class GetOffender {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offenderMinimal = offender(
          nomsId = "A1234BC",
          firstName = "JOHN",
          lastName = "BOG",
          birthDate = null,
        ) {
        }
        offenderFull = offender(
          nomsId = "B1234CD",
          titleCode = "MRS",
          firstName = "JANE",
          middleName = "Mary",
          middleName2 = "Ann",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
          birthPlace = "LONDON",
          ethnicityCode = "M3",
          genderCode = "F",
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic offender data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderMinimal.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderMinimal.id)
          .jsonPath("title").doesNotExist()
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("middleName1").doesNotExist()
          .jsonPath("middleName2").doesNotExist()
          .jsonPath("lastName").isEqualTo("BOG")
          .jsonPath("dateOfBirth").doesNotExist()
          .jsonPath("birthPlace").doesNotExist()
          .jsonPath("race").doesNotExist()
          .jsonPath("sex.code").isEqualTo("M")
          .jsonPath("sex.description").isEqualTo("Male")
          .jsonPath("aliases").doesNotExist()
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }

      @Test
      fun `will return full offender data`() {
        webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderFull.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderFull.id)
          .jsonPath("title.code").isEqualTo("MRS")
          .jsonPath("title.description").isEqualTo("Mrs")
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("middleName1").isEqualTo("Mary")
          .jsonPath("middleName2").isEqualTo("Ann")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("birthPlace").isEqualTo("LONDON")
          .jsonPath("race.code").isEqualTo("M3")
          .jsonPath("race.description").isEqualTo("Mixed: White and Asian")
          .jsonPath("sex.code").isEqualTo("F")
          .jsonPath("sex.description").isEqualTo("Female")
          .jsonPath("audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }

    @Nested
    inner class Aliases {
      private lateinit var offender: Offender
      private lateinit var alias: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff(firstName = "KOFE", lastName = "ADDY") {
            account(username = "KOFEADDY", type = "GENERAL")
          }
          offender = offender(
            nomsId = "C1234EF",
            firstName = "JANE",
            lastName = "NARK",
            birthDate = LocalDate.parse("1999-12-22"),
          ) {
            alias = alias(
              titleCode = "MR",
              lastName = "NTHANDA",
              firstName = "LEKAN",
              middleName = "Fred",
              middleName2 = "Johas",
              birthDate = LocalDate.parse("1965-07-19"),
              ethnicityCode = "M1",
              genderCode = "M",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            ) {
            }
          }
        }
      }

      @Test
      fun `will return aliases`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offender.nomsId)
          .jsonPath("offenderId").isEqualTo(offender.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("aliases.length()").isEqualTo(1)
          .jsonPath("aliases[0].offenderId").isEqualTo(alias.id)
          .jsonPath("aliases[0].title.code").isEqualTo("MR")
          .jsonPath("aliases[0].title.description").isEqualTo("Mr")
          .jsonPath("aliases[0].firstName").isEqualTo("LEKAN")
          .jsonPath("aliases[0].middleName1").isEqualTo("Fred")
          .jsonPath("aliases[0].middleName2").isEqualTo("Johas")
          .jsonPath("aliases[0].lastName").isEqualTo("NTHANDA")
          .jsonPath("aliases[0].dateOfBirth").isEqualTo("1965-07-19")
          .jsonPath("aliases[0].race.code").isEqualTo("M1")
          .jsonPath("aliases[0].race.description").isEqualTo("Mixed: White and Black Caribbean")
          .jsonPath("aliases[0].sex.code").isEqualTo("M")
          .jsonPath("aliases[0].sex.description").isEqualTo("Male")
          .jsonPath("aliases[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("aliases[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("aliases[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }
  }
}
