package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateInternetAddressDsl.Companion.EMAIL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateInternetAddressDsl.Companion.WEB
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporatePhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporatePhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CorporateResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var corporateRepository: CorporateRepository

  @Autowired
  private lateinit var corporateAddressRepository: CorporateAddressRepository

  @Autowired
  private lateinit var corporatePhoneRepository: CorporatePhoneRepository

  @Autowired
  private lateinit var addressPhoneRepository: AddressPhoneRepository

  @Autowired
  private lateinit var corporateInternetAddressRepository: CorporateInternetAddressRepository

  @DisplayName("GET /corporates/ids")
  @Nested
  inner class GetCorporateIds {
    private var lowestCorporateId = 0L
    private var highestCorporateId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        lowestCorporateId = (1..20).map {
          corporate(
            corporateName = "BOOTS",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.first().id
        (1..20).forEach { _ ->
          corporate(
            corporateName = "BOOTS",
            whenCreated = LocalDateTime.parse("2022-01-01T00:00"),
          )
        }
        highestCorporateId = (1..20).map {
          corporate(
            corporateName = "BOOTS",
            whenCreated = LocalDateTime.parse("2024-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.last().id
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/corporates/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/corporates/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/corporates/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 or all corporates`() {
        webTestClient.get().uri {
          it.path("/corporates/ids")
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
          it.path("/corporates/ids")
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
    }

    @Test
    fun `can filter by fromDate`() {
      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2020-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(40)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `can filter by toDate`() {
      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("toDate", "2020-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(1)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `can filter by fromDate and toDate`() {
      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2020-01-02")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(1)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `will return corporate Ids create at midnight on the day matching the filter `() {
      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2021-12-31")
          .queryParam("toDate", "2022-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2021-12-31")
          .queryParam("toDate", "2021-12-31")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)

      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2022-01-01")
          .queryParam("toDate", "2022-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2022-01-01")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("fromDate", "2022-01-02")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
    }

    @Test
    fun `will order by corporateId ascending`() {
      webTestClient.get().uri {
        it.path("/corporates/ids")
          .queryParam("size", "60")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("numberOfElements").isEqualTo(60)
        .jsonPath("content[0].corporateId").isEqualTo(lowestCorporateId)
        .jsonPath("content[59].corporateId").isEqualTo(highestCorporateId)
    }
  }

  @Nested
  @DisplayName("POST /corporates")
  inner class CreateCorporate {
    private val corporate = CreateCorporateOrganisationRequest(id = 100000, name = "Police")

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(corporate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(corporate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates")
          .bodyValue(corporate)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 if corporate caseload code does not exist`() {
        webTestClient.post().uri("/corporates")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(corporate.copy(caseloadId = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Caseload ZZZ not found")
      }
    }

    @Nested
    @Transactional
    inner class HappyPath {
      @Test
      fun `will create a corporate with minimal data`() {
        webTestClient.post().uri("/corporates")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(corporate.copy(id = 10001, name = "Bingahm Solicitors"))
          .exchange()
          .expectStatus().isCreated

        with(corporateRepository.findByIdOrNull(10001)!!) {
          assertThat(id).isEqualTo(10001)
          assertThat(corporateName).isEqualTo("Bingahm Solicitors")
          assertThat(active).isTrue()
          assertThat(caseload).isNull()
          assertThat(commentText).isNull()
          assertThat(feiNumber).isNull()
          assertThat(taxNo).isNull()
          assertThat(expiryDate).isNull()
          assertThat(suspended).isFalse()
          assertThat(types).isEmpty()
          assertThat(addresses).isEmpty()
          assertThat(phones).isEmpty()
          assertThat(types).isEmpty()
        }
      }

      @Test
      fun `will create a corporate with maximum data`() {
        webTestClient.post().uri("/corporates")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            corporate.copy(
              id = 10002,
              name = "Bright Solicitors",
              caseloadId = "LEI",
              comment = "Some comment",
              programmeNumber = "123456",
              vatNumber = "1234567890",
              active = false,
              expiryDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporateRepository.findByIdOrNull(10002)!!) {
          assertThat(id).isEqualTo(10002)
          assertThat(corporateName).isEqualTo("Bright Solicitors")
          assertThat(active).isFalse()
          assertThat(caseload?.id).isEqualTo("LEI")
          assertThat(caseload?.description).isEqualTo("LEEDS (HMP)")
          assertThat(commentText).isEqualTo("Some comment")
          assertThat(feiNumber).isEqualTo("123456")
          assertThat(taxNo).isEqualTo("1234567890")
          assertThat(expiryDate).isEqualTo(LocalDate.now())
          assertThat(suspended).isFalse()
          assertThat(types).isEmpty()
          assertThat(addresses).isEmpty()
          assertThat(phones).isEmpty()
          assertThat(types).isEmpty()
        }
      }
    }
  }

  @Nested
  @DisplayName("PUT /corporates/{corporateId}")
  inner class UpdateCorporate {
    private val corporateRequest = UpdateCorporateOrganisationRequest(name = "Police")
    private lateinit var corporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        corporate = corporate(corporateName = "Shipley Young Hope") {
          type("YOTWORKER")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(corporateRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(corporateRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .bodyValue(corporateRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if corporate does not exist`() {
        webTestClient.put().uri("/corporates/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(corporateRequest)
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Not Found: Corporate 99999 not found")
      }

      @Test
      fun `will return 400 if corporate caseload code does not exist`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(corporateRequest.copy(caseloadId = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Caseload ZZZ not found")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a corporate with minimal data`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(corporateRequest.copy(name = "Bingahm Solicitors"))
          .exchange()
          .expectStatus().isNoContent

        nomisDataBuilder.runInTransaction {
          with(corporateRepository.findByIdOrNull(corporate.id)!!) {
            assertThat(corporateName).isEqualTo("Bingahm Solicitors")
            assertThat(active).isTrue()
            assertThat(caseload).isNull()
            assertThat(commentText).isNull()
            assertThat(feiNumber).isNull()
            assertThat(taxNo).isNull()
            assertThat(expiryDate).isNull()
            assertThat(suspended).isFalse()
            assertThat(addresses).isEmpty()
            assertThat(phones).isEmpty()
            assertThat(types).hasSize(1)
          }
        }
      }

      @Test
      fun `will update a corporate with maximum data`() {
        webTestClient.put().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(
            corporateRequest.copy(
              name = "Bright Solicitors",
              caseloadId = "LEI",
              comment = "Some comment",
              programmeNumber = "123456",
              vatNumber = "1234567890",
              active = false,
              expiryDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus().isNoContent

        nomisDataBuilder.runInTransaction {
          with(corporateRepository.findByIdOrNull(corporate.id)!!) {
            assertThat(corporateName).isEqualTo("Bright Solicitors")
            assertThat(active).isFalse()
            assertThat(caseload?.id).isEqualTo("LEI")
            assertThat(caseload?.description).isEqualTo("LEEDS (HMP)")
            assertThat(commentText).isEqualTo("Some comment")
            assertThat(feiNumber).isEqualTo("123456")
            assertThat(taxNo).isEqualTo("1234567890")
            assertThat(expiryDate).isEqualTo(LocalDate.now())
            assertThat(suspended).isFalse()
            assertThat(addresses).isEmpty()
            assertThat(phones).isEmpty()
            assertThat(types).hasSize(1)
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /corporates/{corporateId}")
  inner class DeleteCorporate {
    private lateinit var corporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        corporate = corporate(corporateName = "Shipley Young Hope") {
          type("YOTWORKER")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${corporate.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 204 if corporate does not exist`() {
        webTestClient.delete().uri("/corporates/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the corporate`() {
        assertThat(corporateRepository.existsById(corporate.id)).isTrue()
        webTestClient.delete().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corporateRepository.existsById(corporate.id)).isFalse()
      }
    }
  }

  @Nested
  @DisplayName("GET /corporates/{corporateId}")
  inner class GetCorporate {
    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      private lateinit var corporate: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
        }
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if corporate does not exist`() {
        webTestClient.get().uri("/corporates/999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {

      @Nested
      inner class CoreCorporateData {
        private lateinit var corporate: Corporate
        private lateinit var hotel: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(corporateName = "Police")
            hotel = corporate(
              corporateName = "Holiday Inn",
              caseloadId = "LEI",
              active = false,
              expiryDate = LocalDate.parse("2023-04-01"),
              taxNo = "G123445",
              feiNumber = "1",
              commentText = "Good place to work",
              whoCreated = "M.BOLD",
              whenCreated = LocalDateTime.parse("2023-03-22T10:20:30"),

            ) {
              type("YOTWORKER")
              type("TEA")
            }
          }
        }

        @Test
        fun `will find a corporate when it exists`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("id").isEqualTo(corporate.id)
        }

        @Test
        fun `will return the core corporate data`() {
          val corporateOrganisation: CorporateOrganisation = webTestClient.get().uri("/corporates/${hotel.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse()

          with(corporateOrganisation) {
            assertThat(id).isEqualTo(hotel.id)
            assertThat(name).isEqualTo("Holiday Inn")
            assertThat(caseload?.code).isEqualTo("LEI")
            assertThat(caseload?.description).isEqualTo("LEEDS (HMP)")
            assertThat(comment).isEqualTo("Good place to work")
            assertThat(programmeNumber).isEqualTo("1")
            assertThat(vatNumber).isEqualTo("G123445")
            assertThat(active).isFalse()
            assertThat(expiryDate).isEqualTo(LocalDate.parse("2023-04-01"))
            assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2023-03-22T10:20:30"))
            assertThat(audit.createUsername).isEqualTo("M.BOLD")
          }
        }

        @Test
        fun `will return any associated corporate types`() {
          val corporateOrganisation: CorporateOrganisation = webTestClient.get().uri("/corporates/${hotel.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse()

          with(corporateOrganisation) {
            assertThat(types).hasSize(2)
            assertThat(types[0].type).isEqualTo(
              CodeDescription(
                code = "YOTWORKER",
                description = "YOT Offender Supervisor/Manager",
              ),
            )
            assertThat(types[1].type).isEqualTo(
              CodeDescription(
                code = "TEA",
                description = "Teacher",
              ),
            )
          }
        }
      }

      @Nested
      inner class CorporatePhoneNumbers {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "Boots",
            ) {
              phone(
                phoneType = "MOB",
                phoneNo = "07399999999",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
            }
          }
        }

        @Test
        fun `will return phone numbers`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("phoneNumbers[0].id").isEqualTo(corporate.phones[0].phoneId)
            .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
            .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
            .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
            .jsonPath("phoneNumbers[0].extension").doesNotExist()
            .jsonPath("phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("phoneNumbers[1].id").isEqualTo(corporate.phones[1].phoneId)
            .jsonPath("phoneNumbers[1].type.code").isEqualTo("HOME")
            .jsonPath("phoneNumbers[1].type.description").isEqualTo("Home")
            .jsonPath("phoneNumbers[1].number").isEqualTo("01142561919")
            .jsonPath("phoneNumbers[1].extension").isEqualTo("123")
        }
      }

      @Nested
      inner class Addresses {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "Boots",
            ) {
              address(
                premise = null,
                street = null,
                locality = null,
                postcode = "S1 3GG",
              )
              address(
                type = "HOME",
                flat = "3B",
                premise = "Brown Court",
                street = "Scotland Street",
                locality = "Hunters Bar",
                postcode = "S1 3GG",
                city = SHEFFIELD,
                county = "S.YORKSHIRE",
                country = "ENG",
                validatedPAF = true,
                noFixedAddress = false,
                primaryAddress = true,
                mailAddress = true,
                comment = "Not to be used",
                startDate = "2024-10-01",
                endDate = "2024-11-01",
                isServices = true,
                businessHours = "Monday to Friday 08:00 to 16:00",
                contactPersonName = "Jim Bob",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              ) {
                phone(
                  phoneType = "MOB",
                  phoneNo = "07399999999",
                  whoCreated = "KOFEADDY",
                  whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
                )
                phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
              }
              address(
                noFixedAddress = true,
                primaryAddress = false,
                premise = null,
                street = null,
                locality = null,
              )
            }
          }
        }

        @Test
        fun `will return addresses`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("addresses[0].id").isEqualTo(corporate.addresses[0].addressId)
            .jsonPath("addresses[0].type").doesNotExist()
            .jsonPath("addresses[0].flat").doesNotExist()
            .jsonPath("addresses[0].premise").doesNotExist()
            .jsonPath("addresses[0].street").doesNotExist()
            .jsonPath("addresses[0].locality").doesNotExist()
            .jsonPath("addresses[0].city").doesNotExist()
            .jsonPath("addresses[0].county").doesNotExist()
            .jsonPath("addresses[0].country").doesNotExist()
            .jsonPath("addresses[0].validatedPAF").isEqualTo(false)
            .jsonPath("addresses[0].noFixedAddress").doesNotExist()
            .jsonPath("addresses[0].primaryAddress").isEqualTo(false)
            .jsonPath("addresses[0].mailAddress").isEqualTo(false)
            .jsonPath("addresses[0].isServices").isEqualTo(false)
            .jsonPath("addresses[0].businessHours").doesNotExist()
            .jsonPath("addresses[0].contactPersonName").doesNotExist()
            .jsonPath("addresses[0].comment").doesNotExist()
            .jsonPath("addresses[0].startDate").doesNotExist()
            .jsonPath("addresses[0].endDate").doesNotExist()
            .jsonPath("addresses[1].id").isEqualTo(corporate.addresses[1].addressId)
            .jsonPath("addresses[1].type.code").isEqualTo("HOME")
            .jsonPath("addresses[1].type.description").isEqualTo("Home Address")
            .jsonPath("addresses[1].flat").isEqualTo("3B")
            .jsonPath("addresses[1].premise").isEqualTo("Brown Court")
            .jsonPath("addresses[1].street").isEqualTo("Scotland Street")
            .jsonPath("addresses[1].locality").isEqualTo("Hunters Bar")
            .jsonPath("addresses[1].postcode").isEqualTo("S1 3GG")
            .jsonPath("addresses[1].city.code").isEqualTo("25343")
            .jsonPath("addresses[1].city.description").isEqualTo("Sheffield")
            .jsonPath("addresses[1].county.code").isEqualTo("S.YORKSHIRE")
            .jsonPath("addresses[1].county.description").isEqualTo("South Yorkshire")
            .jsonPath("addresses[1].country.code").isEqualTo("ENG")
            .jsonPath("addresses[1].country.description").isEqualTo("England")
            .jsonPath("addresses[1].validatedPAF").isEqualTo(true)
            .jsonPath("addresses[1].noFixedAddress").isEqualTo(false)
            .jsonPath("addresses[1].primaryAddress").isEqualTo(true)
            .jsonPath("addresses[1].mailAddress").isEqualTo(true)
            .jsonPath("addresses[1].comment").isEqualTo("Not to be used")
            .jsonPath("addresses[1].startDate").isEqualTo("2024-10-01")
            .jsonPath("addresses[1].endDate").isEqualTo("2024-11-01")
            .jsonPath("addresses[1].isServices").isEqualTo(true)
            .jsonPath("addresses[1].businessHours").isEqualTo("Monday to Friday 08:00 to 16:00")
            .jsonPath("addresses[1].contactPersonName").isEqualTo("Jim Bob")
            .jsonPath("addresses[1].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("addresses[1].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("addresses[2].noFixedAddress").isEqualTo(true)
        }

        @Test
        fun `will return phone numbers associated with addresses`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("addresses[1].phoneNumbers[0].id").isEqualTo(corporate.addresses[1].phones[0].phoneId)
            .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
            .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
            .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
            .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
            .jsonPath("addresses[1].phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("addresses[1].phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("addresses[1].phoneNumbers[1].id").isEqualTo(corporate.addresses[1].phones[1].phoneId)
            .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
            .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
            .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
            .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
        }
      }

      @Nested
      inner class CorporateEmailCorporateAddress {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "BOOTS",
            ) {
              internetAddress(
                internetAddress = "john.bog@justice.gov.uk",
                internetAddressClass = EMAIL,
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              internetAddress(internetAddress = "www.boots.com", internetAddressClass = WEB)
            }
          }
        }

        @Test
        fun `will return email address`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("internetAddresses[0].id").isEqualTo(corporate.internetAddresses[0].internetAddressId)
            .jsonPath("internetAddresses[0].internetAddress").isEqualTo("john.bog@justice.gov.uk")
            .jsonPath("internetAddresses[0].type").isEqualTo("EMAIL")
            .jsonPath("internetAddresses[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("internetAddresses[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("internetAddresses[1].id").isEqualTo(corporate.internetAddresses[1].internetAddressId)
            .jsonPath("internetAddresses[1].internetAddress").isEqualTo("www.boots.com")
            .jsonPath("internetAddresses[1].type").isEqualTo("WEB")
        }
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/address")
  @Nested
  inner class CreateCorporateAddress {
    private val validAddressRequest = CreateCorporateAddressRequest(
      mailAddress = true,
      primaryAddress = true,
      isServices = false,
      noFixedAddress = false,
    )

    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(corporateName = "Police")
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/address")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when city code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(validAddressRequest.copy(cityCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when county code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(validAddressRequest.copy(countyCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when address type code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(validAddressRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an address for the corporate`() {
        val response: CreateCorporateAddressResponse = webTestClient.post().uri("/corporates/${existingCorporate.id}/address")
          .bodyValue(
            validAddressRequest.copy(
              typeCode = "BUS",
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              cityCode = SHEFFIELD,
              countyCode = "S.YORKSHIRE",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
              isServices = true,
              contactPersonName = "BOBBY",
              businessHours = "10-12am",

            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(corporateAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(addressId).isEqualTo(response.id)
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(addressType?.description).isEqualTo("Business Address")
          assertThat(flat).isEqualTo("1A")
          assertThat(premise).isEqualTo("Bolden Court")
          assertThat(street).isEqualTo("Fulwood Road")
          assertThat(locality).isEqualTo("Broomhill")
          assertThat(city?.description).isEqualTo("Sheffield")
          assertThat(county?.description).isEqualTo("South Yorkshire")
          assertThat(country?.description).isEqualTo("United Kingdom")
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(noFixedAddress).isFalse()
          assertThat(startDate).isEqualTo(LocalDate.parse("2001-01-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2032-12-31"))
          assertThat(isServices).isTrue()
          assertThat(contactPersonName).isEqualTo("BOBBY")
          assertThat(businessHours).isEqualTo("10-12am")
        }
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.addresses).anyMatch { it.addressId == response.id }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/address/{addressId}")
  @Nested
  inner class UpdateCorporateAddress {
    private val validAddressRequest = UpdateCorporateAddressRequest(
      mailAddress = true,
      primaryAddress = true,
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var existingAddress: CorporateAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingAddress = address()
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.put().uri("/corporates/9999/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/99999")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when city code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(cityCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when county code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(countyCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when address type code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a address`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .bodyValue(
            validAddressRequest.copy(
              typeCode = "HOME",
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              cityCode = SHEFFIELD,
              countyCode = "S.YORKSHIRE",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
              isServices = true,
              contactPersonName = "BOBBY",
              businessHours = "10-12am",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        with(corporateAddressRepository.findByIdOrNull(existingAddress.addressId)!!) {
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(addressType?.description).isEqualTo("Home Address")
          assertThat(flat).isEqualTo("1A")
          assertThat(premise).isEqualTo("Bolden Court")
          assertThat(street).isEqualTo("Fulwood Road")
          assertThat(locality).isEqualTo("Broomhill")
          assertThat(city?.description).isEqualTo("Sheffield")
          assertThat(county?.description).isEqualTo("South Yorkshire")
          assertThat(country?.description).isEqualTo("United Kingdom")
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(noFixedAddress).isFalse()
          assertThat(startDate).isEqualTo(LocalDate.parse("2001-01-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2032-12-31"))
          assertThat(isServices).isTrue()
          assertThat(contactPersonName).isEqualTo("BOBBY")
          assertThat(businessHours).isEqualTo("10-12am")
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/address/{addressId}")
  @Nested
  inner class DeleteCorporateAddress {
    private lateinit var existingCorporate: Corporate
    private lateinit var existingAddress: CorporateAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingAddress = address()
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 address exists but not exist on the corporate `() {
        webTestClient.delete().uri("/corporates/9999/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when address does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a corporate address`() {
        assertThat(corporateAddressRepository.existsById(existingAddress.addressId)).isTrue()
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(corporateAddressRepository.existsById(existingAddress.addressId)).isFalse()
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/address/{addressId}/phone")
  @Nested
  inner class CreateCorporateAddressPhone {
    private val validPhoneRequest = CreateCorporatePhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var anotherCorporate: Corporate
    private lateinit var existingAddress: CorporateAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        anotherCorporate = corporate(
          corporateName = "Another Police",
        )
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingAddress = address { }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address/999/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist on corporate`() {
        webTestClient.post().uri("/corporates/${anotherCorporate.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a phone`() {
        val response: CreateCorporatePhoneResponse = webTestClient.post().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreateCorporatePhoneResponse::class.java)
          .returnResult()
          .responseBody!!

        val phone = addressPhoneRepository.findByIdOrNull(response.id)!!

        with(phone) {
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          val address = corporate!!.addresses.find { it.addressId == existingAddress.addressId }
          assertThat(address?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/address/{addressId}/phone/{phoneId}")
  @Nested
  inner class UpdateCorporateAddressPhone {
    private val validPhoneRequest = CreateCorporatePhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var existingAddress: CorporateAddress
    private lateinit var existingPhone: AddressPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.put().uri("/corporates/9999/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/99999/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the phone`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        with(addressPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!) {
          assertThat(phoneId).isEqualTo(existingPhone.phoneId)
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phoneNo).isEqualTo("07973 555 5555")
          assertThat(extNo).isEqualTo("x555")
          assertThat(phoneType.description).isEqualTo("Mobile")
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/address/phone/{phoneId}")
  @Nested
  inner class DeleteCorporateAddressPhone {
    private lateinit var existingCorporate: Corporate
    private lateinit var existingAddress: CorporateAddress
    private lateinit var existingPhone: AddressPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/phone/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the phone`() {
        assertThat(addressPhoneRepository.existsById(existingPhone.phoneId)).isTrue()
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/address/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(addressPhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/phone")
  @Nested
  inner class CreateCorporatePhone {
    private val validPhoneRequest = CreateCorporatePhoneRequest(
      number = "0114 555 555",
      extension = "ext123",
      typeCode = "BUS",
    )

    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(corporateName = "Police")
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/phone")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an phone for the corporate`() {
        val response: CreateCorporatePhoneResponse = webTestClient.post().uri("/corporates/${existingCorporate.id}/phone")
          .bodyValue(
            validPhoneRequest.copy(
              typeCode = "BUS",
              extension = "ext 123",
              number = "07973 55 55555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(corporatePhoneRepository.findByIdOrNull(response.id)!!) {
          assertThat(phoneId).isEqualTo(response.id)
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(phoneNo).isEqualTo("07973 55 55555")
          assertThat(extNo).isEqualTo("ext 123")
          assertThat(phoneType.description).isEqualTo("Business")
        }
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.phones).anyMatch { it.phoneId == response.id }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/phone/{phoneId}")
  @Nested
  inner class UpdateCorporatePhone {
    private val validPhoneRequest = UpdateCorporatePhoneRequest(
      number = "0114 555 555",
      typeCode = "MOB",
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var existingPhone: CorporatePhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingPhone = phone(phoneNo = "0113 444 4444", phoneType = "MOB")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.put().uri("/corporates/9999/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a phone`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .bodyValue(
            validPhoneRequest.copy(
              typeCode = "MOB",
              number = "07973 555 555",
              extension = "x12",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        with(corporatePhoneRepository.findByIdOrNull(existingPhone.phoneId)!!) {
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(phoneNo).isEqualTo("07973 555 555")
          assertThat(extNo).isEqualTo("x12")
          assertThat(phoneType.description).isEqualTo("Mobile")
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/phone/{phoneId}")
  @Nested
  inner class DeleteCorporatePhone {
    private lateinit var existingCorporate: Corporate
    private lateinit var existingPhone: CorporatePhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingPhone = phone(phoneNo = "0118 8383 883", phoneType = "BUS")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 phone exists but not exist on the corporate `() {
        webTestClient.delete().uri("/corporates/9999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/phone/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a corporate phone`() {
        assertThat(corporatePhoneRepository.existsById(existingPhone.phoneId)).isTrue()
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(corporatePhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/email")
  @Nested
  inner class CreateCorporateEmail {
    private val validEmailRequest = CreateCorporateEmailRequest(
      email = "test1@test.com",
    )

    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(corporateName = "Police")
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/email")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/email")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/email")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an email for the corporate`() {
        val response: CreateCorporateEmailResponse = webTestClient.post().uri("/corporates/${existingCorporate.id}/email")
          .bodyValue(
            validEmailRequest.copy(
              email = "test@justice.gov.uk",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(corporateInternetAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(internetAddressId).isEqualTo(response.id)
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(internetAddressClass).isEqualTo("EMAIL")
          assertThat(internetAddress).isEqualTo("test@justice.gov.uk")
        }
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.internetAddresses).anyMatch { it.internetAddressId == response.id }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/email/{emailId}")
  @Nested
  inner class UpdateCorporateEmail {
    private val validEmailRequest = UpdateCorporateEmailRequest(
      email = "test@justice.gov.uk",
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var existingEmail: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingEmail = internetAddress(internetAddressClass = EMAIL, internetAddress = "biug@just.com")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.put().uri("/corporates/9999/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when email does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/email/99999")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a email`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .bodyValue(
            validEmailRequest.copy(
              email = "newemail@justice.gov.uk",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        with(corporateInternetAddressRepository.findByIdOrNull(existingEmail.internetAddressId)!!) {
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(internetAddress).isEqualTo("newemail@justice.gov.uk")
          assertThat(internetAddressClass).isEqualTo("EMAIL")
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/email/{emailId}")
  @Nested
  inner class DeleteCorporateEmail {
    private lateinit var existingCorporate: Corporate
    private lateinit var existingEmail: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingEmail = internetAddress(internetAddressClass = EMAIL, internetAddress = "biug@just.com")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 email exists but not exist on the corporate `() {
        webTestClient.delete().uri("/corporates/9999/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when email does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/email/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a corporate email`() {
        assertThat(corporateInternetAddressRepository.existsById(existingEmail.internetAddressId)).isTrue()
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(corporateInternetAddressRepository.existsById(existingEmail.internetAddressId)).isFalse()
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/web-address")
  @Nested
  inner class CreateCorporateWebAddress {
    private val validWebAddressRequest = CreateCorporateWebAddressRequest(
      webAddress = "www.test.com",
    )

    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(corporateName = "Police")
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/web-address")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/web-address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/web-address")
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/web-address")
          .bodyValue(validWebAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an web address for the corporate`() {
        val response: CreateCorporateWebAddressResponse = webTestClient.post().uri("/corporates/${existingCorporate.id}/web-address")
          .bodyValue(
            validWebAddressRequest.copy(
              webAddress = "www.justice.gov.uk",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(corporateInternetAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(internetAddressId).isEqualTo(response.id)
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(internetAddressClass).isEqualTo("WEB")
          assertThat(internetAddress).isEqualTo("www.justice.gov.uk")
        }
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.internetAddresses).anyMatch { it.internetAddressId == response.id }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/web-address/{webId}")
  @Nested
  inner class UpdateCorporateWeb {
    private val validWebAddressRequest = UpdateCorporateWebAddressRequest(
      webAddress = "www.justice.gov.uk",
    )

    private lateinit var existingCorporate: Corporate
    private lateinit var existingWebAddress: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingWebAddress = internetAddress(internetAddressClass = WEB, internetAddress = "biug.just.com")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .bodyValue(validWebAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.put().uri("/corporates/9999/web-address/${existingWebAddress.internetAddressId}")
          .bodyValue(validWebAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when web address does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/web-address/99999")
          .bodyValue(validWebAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a web address`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .bodyValue(
            validWebAddressRequest.copy(
              webAddress = "www.newweb.justice.gov.uk",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        with(corporateInternetAddressRepository.findByIdOrNull(existingWebAddress.internetAddressId)!!) {
          assertThat(corporate.id).isEqualTo(existingCorporate.id)
          assertThat(internetAddress).isEqualTo("www.newweb.justice.gov.uk")
          assertThat(internetAddressClass).isEqualTo("WEB")
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/web-address/{webId}")
  @Nested
  inner class DeleteCorporateWebAddress {
    private lateinit var existingCorporate: Corporate
    private lateinit var existingWebAddress: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          existingWebAddress = internetAddress(internetAddressClass = WEB, internetAddress = "www.biug.just.com")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 web address exists but not exist on the corporate `() {
        webTestClient.delete().uri("/corporates/9999/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when web does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/web-address/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a corporate web address`() {
        assertThat(corporateInternetAddressRepository.existsById(existingWebAddress.internetAddressId)).isTrue()
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/web-address/${existingWebAddress.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(corporateInternetAddressRepository.existsById(existingWebAddress.internetAddressId)).isFalse()
      }
    }
  }

  @DisplayName("POST /corporates/{corporateId}/type")
  @Nested
  inner class CreateCorporateType {
    private val validTypeRequest = CreateCorporateTypeRequest(
      typeCode = "TEA",
    )

    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(corporateName = "Police") {
          type(typeCode = "YOTWORKER")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validTypeRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validTypeRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/type")
          .bodyValue(validTypeRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when corporate does not exist`() {
        webTestClient.post().uri("/corporates/999/type")
          .bodyValue(validTypeRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will allow attempt to add existing type for the corporate without raising an error`() {
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(1)
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }

        webTestClient.post().uri("/corporates/${existingCorporate.id}/type")
          .bodyValue(
            validTypeRequest.copy(
              typeCode = "YOTWORKER",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated

        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(1)
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will add a type for the corporate and keep existing type`() {
        webTestClient.post().uri("/corporates/${existingCorporate.id}/type")
          .bodyValue(
            validTypeRequest.copy(
              typeCode = "TEA",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated

        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(2)
          assertThat(corporate?.types).anyMatch { it.type.code == "TEA" && it.type.description == "Teacher" }
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }
      }
    }
  }

  @DisplayName("PUT /corporates/{corporateId}/type")
  @Nested
  inner class UpdateCorporateTypes {
    private lateinit var existingCorporate: Corporate
    private val validRequest = UpdateCorporateTypesRequest(typeCodes = setOf("YOTWORKER", "TRUST"))

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          type(typeCode = "TEA")
          type(typeCode = "YOTWORKER")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/type")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when type does not exist`() {
        webTestClient.put().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(UpdateCorporateTypesRequest(typeCodes = setOf("TEA", "ZZZ")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will add and delete a corporate types`() {
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(2)
          assertThat(corporate?.types).anyMatch { it.type.code == "TEA" }
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }

        webTestClient.put().uri("/corporates/${existingCorporate.id}/type")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(UpdateCorporateTypesRequest(typeCodes = setOf("TEA", "TRUST")))
          .exchange()
          .expectStatus()
          .isNoContent

        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(2)
          assertThat(corporate?.types).anyMatch { it.type.code == "TEA" }
          assertThat(corporate?.types).anyMatch { it.type.code == "TRUST" }
        }
      }
    }
  }

  @DisplayName("DELETE /corporates/{corporateId}/type/{typeCode}")
  @Nested
  inner class DeleteCorporateType {
    private lateinit var existingCorporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingCorporate = corporate(
          corporateName = "Police",
        ) {
          type(typeCode = "TEA")
          type(typeCode = "YOTWORKER")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/type/TEA")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/type/TEA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/type/TEA")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 204 when type does not exist`() {
        webTestClient.delete().uri("/corporates/${existingCorporate.id}/type/ZZZZ")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a corporate type`() {
        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(2)
          assertThat(corporate?.types).anyMatch { it.type.code == "TEA" }
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }

        webTestClient.delete().uri("/corporates/${existingCorporate.id}/type/TEA")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent

        nomisDataBuilder.runInTransaction {
          val corporate = corporateRepository.findByIdOrNull(existingCorporate.id)
          assertThat(corporate?.types).hasSize(1)
          assertThat(corporate?.types).anyMatch { it.type.code == "YOTWORKER" }
        }
      }
    }
  }
}

private inline fun <reified B> WebTestClient.ResponseSpec.expectBodyResponse(): B = this.expectBody(B::class.java).returnResult().responseBody!!
