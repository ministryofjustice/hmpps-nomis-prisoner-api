package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson.OffenderBelief as OffenderBeliefCorePerson

class CorePersonResourceIntTest : IntegrationTestBase() {
  fun deleteAll() {
    repository.deleteAllBeliefs()
    deleteOffenders()
  }

  @DisplayName("GET /core-person/{prisonNumber}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffender {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender

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
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class HappyPath {

      @BeforeAll
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
            birthCountryCode = "ATA",
            ethnicityCode = "M3",
            genderCode = "F",
            nameTypeCode = "MAID",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          ) {
            booking { }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      fun `will return basic offender data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderMinimal.nomsId)
          .jsonPath("offenders[0].offenderId").isEqualTo(offenderMinimal.id)
          .jsonPath("offenders[0].title").doesNotExist()
          .jsonPath("offenders[0].firstName").isEqualTo("JOHN")
          .jsonPath("offenders[0].middleName1").doesNotExist()
          .jsonPath("offenders[0].middleName2").doesNotExist()
          .jsonPath("offenders[0].lastName").isEqualTo("BOG")
          .jsonPath("offenders[0].dateOfBirth").doesNotExist()
          .jsonPath("offenders[0].birthPlace").doesNotExist()
          .jsonPath("offenders[0].birthCountry").doesNotExist()
          .jsonPath("offenders[0].ethnicity").doesNotExist()
          .jsonPath("offenders[0].sex.code").isEqualTo("M")
          .jsonPath("offenders[0].sex.description").isEqualTo("Male")
          .jsonPath("offenders[0].nameType").doesNotExist()
          .jsonPath("offenders[0].workingName").isEqualTo(true)
          .jsonPath("inOutStatus").isEqualTo("OUT")
          .jsonPath("activeFlag").isEqualTo("false")
          .jsonPath("identifiers").doesNotExist()
          .jsonPath("sentenceStartDates").doesNotExist()
          .jsonPath("addresses").doesNotExist()
          .jsonPath("phoneNumbers").doesNotExist()
          .jsonPath("emailAddresses").doesNotExist()
          .jsonPath("nationalities").doesNotExist()
          .jsonPath("nationalityDetails").doesNotExist()
          .jsonPath("beliefs").doesNotExist()
      }

      @Test
      fun `is able to re-hydrate the core person`() {
        val person = webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<CorePerson>().responseBody.blockFirst()!!

        assertThat(person.prisonNumber).isEqualTo(offenderMinimal.nomsId)
      }

      @Test
      fun `will return full offender data`() {
        webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderFull.nomsId)
          .jsonPath("inOutStatus").isEqualTo("IN")
          .jsonPath("activeFlag").isEqualTo("true")
          .jsonPath("offenders[0].offenderId").isEqualTo(offenderFull.id)
          .jsonPath("offenders[0].title.code").isEqualTo("MRS")
          .jsonPath("offenders[0].title.description").isEqualTo("Mrs")
          .jsonPath("offenders[0].firstName").isEqualTo("JANE")
          .jsonPath("offenders[0].middleName1").isEqualTo("Mary")
          .jsonPath("offenders[0].middleName2").isEqualTo("Ann")
          .jsonPath("offenders[0].lastName").isEqualTo("NARK")
          .jsonPath("offenders[0].dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("offenders[0].birthPlace").isEqualTo("LONDON")
          .jsonPath("offenders[0].birthCountry.code").isEqualTo("ATA")
          .jsonPath("offenders[0].birthCountry.description").isEqualTo("Antarctica")
          .jsonPath("offenders[0].ethnicity.code").isEqualTo("M3")
          .jsonPath("offenders[0].ethnicity.description").isEqualTo("Mixed: White and Asian")
          .jsonPath("offenders[0].sex.code").isEqualTo("F")
          .jsonPath("offenders[0].sex.description").isEqualTo("Female")
          .jsonPath("offenders[0].nameType.code").isEqualTo("MAID")
          .jsonPath("offenders[0].nameType.description").isEqualTo("Maiden")
          .jsonPath("offenders[0].createDate").isEqualTo("2020-03-20")
          .jsonPath("offenders[0].workingName").isEqualTo(true)
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class Offenders {
      private lateinit var offender: Offender
      private lateinit var offender2: Offender
      private lateinit var alias: Offender
      private lateinit var alias2: Offender

      @BeforeAll
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
            ) {}
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              issuedDate = LocalDate.parse("2020-01-01"),
              verified = true,
            )
          }
          offender2 = offender(
            nomsId = "C1234EG",
            firstName = "JOHN",
            lastName = "BARK",
          ) {
            alias2 = alias(
              firstName = "AJOHN",
              lastName = "ABARK",
              birthDate = LocalDate.parse("1965-07-19"),
            ) { booking(bookingSequence = 1) { } }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return aliases`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offender.nomsId)
          .jsonPath("offenders.length()").isEqualTo(2)
          .jsonPath("offenders[0].offenderId").isEqualTo(offender.id)
          .jsonPath("offenders[0].firstName").isEqualTo("JANE")
          .jsonPath("offenders[0].lastName").isEqualTo("NARK")
          .jsonPath("offenders[0].workingName").isEqualTo(true)
          .jsonPath("offenders[1].offenderId").isEqualTo(alias.id)
          .jsonPath("offenders[1].title.code").isEqualTo("MR")
          .jsonPath("offenders[1].title.description").isEqualTo("Mr")
          .jsonPath("offenders[1].firstName").isEqualTo("LEKAN")
          .jsonPath("offenders[1].middleName1").isEqualTo("Fred")
          .jsonPath("offenders[1].middleName2").isEqualTo("Johas")
          .jsonPath("offenders[1].lastName").isEqualTo("NTHANDA")
          .jsonPath("offenders[1].dateOfBirth").isEqualTo("1965-07-19")
          .jsonPath("offenders[1].ethnicity.code").isEqualTo("M1")
          .jsonPath("offenders[1].ethnicity.description").isEqualTo("Mixed: White and Black Caribbean")
          .jsonPath("offenders[1].sex.code").isEqualTo("M")
          .jsonPath("offenders[1].sex.description").isEqualTo("Male")
          .jsonPath("offenders[1].workingName").isEqualTo(false)
      }

      @Test
      fun `will set working name to offender record linked to active booking`() {
        webTestClient.get().uri("/core-person/${offender2.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offender2.nomsId)
          .jsonPath("offenders.length()").isEqualTo(2)
          .jsonPath("offenders[0].offenderId").isEqualTo(offender2.id)
          .jsonPath("offenders[0].firstName").isEqualTo("JOHN")
          .jsonPath("offenders[0].lastName").isEqualTo("BARK")
          .jsonPath("offenders[0].workingName").isEqualTo(false)
          .jsonPath("offenders[1].offenderId").isEqualTo(alias2.id)
          .jsonPath("offenders[1].firstName").isEqualTo("AJOHN")
          .jsonPath("offenders[1].lastName").isEqualTo("ABARK")
          .jsonPath("offenders[1].workingName").isEqualTo(true)
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class Identifiers {
      private lateinit var offender: Offender
      private lateinit var alias: Offender

      @BeforeAll
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
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              issuedDate = LocalDate.parse("2020-01-01"),
              verified = true,
            )
            identifier(type = "STAFF", identifier = "123")
            alias = alias {
              identifier(type = "STAFF", identifier = "456")
            }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return identifiers`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW"))).exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("offenders[0].offenderId").isEqualTo(offender.id)
          .jsonPath("offenders[0].identifiers[0].sequence").isEqualTo(1)
          .jsonPath("offenders[0].identifiers[0].identifier").isEqualTo("20/0071818T")
          .jsonPath("offenders[0].identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("offenders[0].identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("offenders[0].identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("offenders[0].identifiers[0].issuedDate").isEqualTo("2020-01-01")
          .jsonPath("offenders[0].identifiers[0].verified").isEqualTo(true)
          .jsonPath("offenders[0].identifiers[1].sequence").isEqualTo(2)
          .jsonPath("offenders[0].identifiers[1].identifier").isEqualTo("123")
          .jsonPath("offenders[0].identifiers[1].type.code").isEqualTo("STAFF")
          .jsonPath("offenders[0].identifiers[1].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("offenders[0].identifiers[1].issuedAuthority").doesNotExist()
          .jsonPath("offenders[0].identifiers[1].issuedDate").doesNotExist()
          .jsonPath("offenders[0].identifiers[1].verified").isEqualTo(false)
          .jsonPath("offenders[0].identifiers.length()").isEqualTo(2)
          .jsonPath("offenders[1].offenderId").isEqualTo(alias.id)
          .jsonPath("offenders[1].identifiers[0].sequence").isEqualTo(1)
          .jsonPath("offenders[1].identifiers[0].identifier").isEqualTo("456")
          .jsonPath("offenders[1].identifiers[0].type.code").isEqualTo("STAFF")
          .jsonPath("offenders[1].identifiers[0].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("offenders[1].identifiers[0].issuedAuthority").doesNotExist()
          .jsonPath("offenders[1].identifiers[0].issuedDate").doesNotExist()
          .jsonPath("offenders[1].identifiers[0].verified").isEqualTo(false)
          .jsonPath("offenders[1].identifiers.length()").isEqualTo(1)
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class OffenderBeliefs {
      private lateinit var offender: Offender
      private lateinit var belief1: OffenderBelief
      private lateinit var belief2: OffenderBelief
      private lateinit var belief3: OffenderBelief
      private lateinit var offender2: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking {
              belief2 = belief(
                beliefCode = "JAIN",
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
              )
              belief1 = belief(
                beliefCode = "ZORO",
                startDate = LocalDate.parse("2018-01-01"),
                endDate = LocalDate.parse("2019-02-03"),
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
            }
            booking(active = false) {
              belief3 = belief(
                beliefCode = "DRU",
                startDate = LocalDate.parse("2023-01-01"),
                changeReason = false,
                verified = false,
              )
            }
          }
          offender2 = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BF",
          ) {
            booking(active = false, bookingSequence = 2) {
              belief(
                beliefCode = "JAIN",
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
              )
            }
            alias(
              firstName = "AJOHN",
              lastName = "ABARK",
              birthDate = LocalDate.parse("1965-07-19"),
            ) { booking(bookingSequence = 1) { } }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return beliefs`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("beliefs.length()").isEqualTo(3)
          .jsonPath("beliefs[0].beliefId").isEqualTo(belief3.beliefId)
          .jsonPath("beliefs[0].belief.code").isEqualTo("DRU")
          .jsonPath("beliefs[0].belief.description").isEqualTo("Druid")
          .jsonPath("beliefs[0].startDate").isEqualTo("2023-01-01")
          .jsonPath("beliefs[0].changeReason").isEqualTo(false)
          .jsonPath("beliefs[0].comments").doesNotExist()
          .jsonPath("beliefs[1].beliefId").isEqualTo(belief2.beliefId)
          .jsonPath("beliefs[1].belief.code").isEqualTo("JAIN")
          .jsonPath("beliefs[1].belief.description").isEqualTo("Jain")
          .jsonPath("beliefs[1].startDate").isEqualTo("2021-01-01")
          .jsonPath("beliefs[1].endDate").doesNotExist()
          .jsonPath("beliefs[1].changeReason").isEqualTo(true)
          .jsonPath("beliefs[1].comments").isEqualTo("No longer believes in Zoroastrianism")
          .jsonPath("beliefs[1].audit.createUsername").isNotEmpty
          .jsonPath("beliefs[1].audit.createDatetime").isNotEmpty
          .jsonPath("beliefs[2].beliefId").isEqualTo(belief1.beliefId)
          .jsonPath("beliefs[2].belief.code").isEqualTo("ZORO")
          .jsonPath("beliefs[2].belief.description").isEqualTo("Zoroastrian")
          .jsonPath("beliefs[2].startDate").isEqualTo("2018-01-01")
          .jsonPath("beliefs[2].endDate").isEqualTo("2019-02-03")
          .jsonPath("beliefs[2].changeReason").doesNotExist()
          .jsonPath("beliefs[2].comments").doesNotExist()
          .jsonPath("beliefs[2].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("beliefs[2].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }

      @Test
      fun `will return beliefs when current alias is different from root offender`() {
        webTestClient.get().uri("/core-person/${offender2.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("beliefs.length()").isEqualTo(1)
          .jsonPath("beliefs[0].belief.code").isEqualTo("JAIN")
          .jsonPath("beliefs[0].belief.description").isEqualTo("Jain")
          .jsonPath("beliefs[0].startDate").isEqualTo("2021-01-01")
          .jsonPath("beliefs[0].endDate").doesNotExist()
          .jsonPath("beliefs[0].changeReason").isEqualTo(true)
          .jsonPath("beliefs[0].comments").isEqualTo("No longer believes in Zoroastrianism")
      }
    }
  }

  @DisplayName("GET /core-person/{prisonNumber}/aliases-identifiers")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderAliasesAndIdentifiers {
    private lateinit var offender: Offender
    private lateinit var alias: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(
          nomsId = "D5678EF",
          firstName = "JANE",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
        ) {
          identifier(
            type = "PNC",
            identifier = "20/0071818T",
            issuedAuthority = "Met Police",
            issuedDate = LocalDate.parse("2020-01-01"),
            verified = true,
          )
          booking(bookingSequence = 2, active = false) { }
          alias = alias(
            firstName = "AJOHN",
            lastName = "ABARK",
          ) {
            identifier(type = "STAFF", identifier = "123")
            booking(bookingSequence = 1) { }
          }
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}/aliases-identifiers")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}/aliases-identifiers")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}/aliases-identifiers")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C/aliases-identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `will return aliases and identifiers`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}/aliases-identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)
          .jsonPath("[0].offenderId").isEqualTo(offender.id)
          .jsonPath("[0].firstName").isEqualTo("JANE")
          .jsonPath("[0].lastName").isEqualTo("NARK")
          .jsonPath("[0].workingName").isEqualTo(false)
          .jsonPath("[0].identifiers.length()").isEqualTo(1)
          .jsonPath("[0].identifiers[0].sequence").isEqualTo(1)
          .jsonPath("[0].identifiers[0].identifier").isEqualTo("20/0071818T")
          .jsonPath("[0].identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("[0].identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("[0].identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("[0].identifiers[0].issuedDate").isEqualTo("2020-01-01")
          .jsonPath("[0].identifiers[0].verified").isEqualTo(true)
          .jsonPath("[1].offenderId").isEqualTo(alias.id)
          .jsonPath("[1].firstName").isEqualTo("AJOHN")
          .jsonPath("[1].lastName").isEqualTo("ABARK")
          .jsonPath("[1].workingName").isEqualTo(true)
          .jsonPath("[1].identifiers.length()").isEqualTo(1)
          .jsonPath("[1].identifiers[0].sequence").isEqualTo(1)
          .jsonPath("[1].identifiers[0].identifier").isEqualTo("123")
          .jsonPath("[1].identifiers[0].type.code").isEqualTo("STAFF")
          .jsonPath("[1].identifiers[0].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("[1].identifiers[0].issuedAuthority").doesNotExist()
          .jsonPath("[1].identifiers[0].issuedDate").doesNotExist()
          .jsonPath("[1].identifiers[0].verified").isEqualTo(false)
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/merge")
  @Nested
  @TestInstance(PER_CLASS)
  @ExtendWith(OutputCaptureExtension::class)
  inner class UpdateOffenderAfterMerge {
    private lateinit var offender: Offender
    private lateinit var latestBelief: OffenderBelief
    private lateinit var deletedOffender: Offender
    private lateinit var beliefToMove: OffenderBelief

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(
          nomsId = "A1234BC",
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          booking {
            latestBelief = belief(
              beliefCode = "JAIN",
              startDate = LocalDate.parse("2021-01-01"),
            )
          }
        }
        deletedOffender = offender(
          nomsId = "A1234BD",
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          booking {
            beliefToMove = belief(
              beliefCode = "AGNO",
              startDate = LocalDate.parse("2020-01-01"),
            )
          }
        }
      }
      repository.offenderRepository.delete(deletedOffender)
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/merge")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CorePersonMergeRequest(religions = listOf(CorePersonReligionRequest(beliefId = 1L))))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/merge")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CorePersonMergeRequest(religions = listOf(CorePersonReligionRequest(beliefId = 1L))))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/merge")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CorePersonMergeRequest(religions = listOf(CorePersonReligionRequest(beliefId = 1L))))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns no content and logs the merge update`(capturedOutput: CapturedOutput) {
        val request = CorePersonMergeRequest(
          religions = listOf(
            CorePersonReligionRequest(
              beliefId = latestBelief.beliefId,
            ),
            CorePersonReligionRequest(
              beliefId = beliefToMove.beliefId,
              endDate = LocalDate.parse("2024-12-12"),
            ),
          ),
        )

        webTestClient.post().uri("/core-person/${offender.nomsId}/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isNoContent

        val updatedBelief = repository.offenderBeliefRepository.findByIdOrNull(latestBelief.beliefId)!!
        assertThat(updatedBelief.endDate).isNull()
        assertThat(updatedBelief.rootOffenderId).isEqualTo(offender.id)

        val updatedBelief2 = repository.offenderBeliefRepository.findByIdOrNull(beliefToMove.beliefId)!!
        assertThat(updatedBelief2.endDate).isEqualTo(LocalDate.parse("2024-12-12"))
        assertThat(updatedBelief2.rootOffenderId).isEqualTo(offender.id)

        assertThat(capturedOutput.all).contains("Updating offender ${offender.nomsId} after merge")
      }
    }
  }

  @DisplayName("GET /core-person/{prisonNumber}/religions")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderReligions {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender
    private lateinit var belief: OffenderBelief

    @BeforeAll
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
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          booking {
            belief = belief(
              beliefCode = "JAIN",
              changeReason = true,
              comments = "No longer believes in Zoroastrianism",
              verified = true,
            )
          }
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/religions")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return empty list if no data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(0)
      }

      @Test
      fun `is able to re-hydrate the beliefs`() {
        val beliefs = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult(ParameterizedTypeReference.forType<OffenderBeliefCorePerson>(OffenderBeliefCorePerson::class.java)).responseBody.blockFirst()!!

        assertThat(beliefs.beliefId).isEqualTo(belief.beliefId)
        assertThat(beliefs.belief).isEqualTo(CodeDescription(code = "JAIN", description = "Jain"))
        assertThat(beliefs.comments).isEqualTo("No longer believes in Zoroastrianism")
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class OffenderBeliefs {
      private lateinit var offender: Offender
      private lateinit var belief1: OffenderBelief
      private lateinit var belief2: OffenderBelief
      private lateinit var belief3: OffenderBelief
      private lateinit var belief4: OffenderBelief
      private lateinit var offender2: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            nomsId = "B1234CD",
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking {
              belief2 = belief(
                beliefCode = "JAIN",
                startDate = LocalDate.parse("2018-01-01"),
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
                whenCreated = LocalDateTime.parse("2022-01-01T10:00"),
              )
              belief1 = belief(
                beliefCode = "ZORO",
                startDate = LocalDate.parse("2018-01-01"),
                endDate = LocalDate.parse("2019-02-03"),
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              belief4 = belief(
                beliefCode = "SATN",
                startDate = LocalDate.parse("2018-01-01"),
                endDate = LocalDate.parse("2018-01-01"),
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2021-01-01T10:00"),
              )
            }
            booking(active = false) {
              belief3 = belief(
                beliefCode = "DRU",
                startDate = LocalDate.parse("2023-01-01"),
                changeReason = false,
                verified = false,
              )
            }
          }
          offender2 = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BF",
          ) {
            booking(active = false, bookingSequence = 2) {
              belief(
                beliefCode = "JAIN",
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
              )
            }
            alias(
              firstName = "AJOHN",
              lastName = "ABARK",
              birthDate = LocalDate.parse("1965-07-19"),
            ) { booking(bookingSequence = 1) { } }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return beliefs`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(4)
          .jsonPath("[0].beliefId").isEqualTo(belief3.beliefId)
          .jsonPath("[0].belief.code").isEqualTo("DRU")
          .jsonPath("[0].belief.description").isEqualTo("Druid")
          .jsonPath("[0].startDate").isEqualTo("2023-01-01")
          .jsonPath("[0].changeReason").isEqualTo(false)
          .jsonPath("[0].comments").doesNotExist()
          .jsonPath("[1].beliefId").isEqualTo(belief2.beliefId)
          .jsonPath("[1].belief.code").isEqualTo("JAIN")
          .jsonPath("[1].belief.description").isEqualTo("Jain")
          .jsonPath("[1].startDate").isEqualTo("2018-01-01")
          .jsonPath("[1].endDate").doesNotExist()
          .jsonPath("[1].changeReason").isEqualTo(true)
          .jsonPath("[1].comments").isEqualTo("No longer believes in Zoroastrianism")
          .jsonPath("[1].audit.createDatetime").isEqualTo("2022-01-01T10:00:00")
          .jsonPath("[2].beliefId").isEqualTo(belief4.beliefId)
          .jsonPath("[2].belief.code").isEqualTo("SATN")
          .jsonPath("[2].belief.description").isEqualTo("Satanism")
          .jsonPath("[2].startDate").isEqualTo("2018-01-01")
          .jsonPath("[2].endDate").isEqualTo("2018-01-01")
          .jsonPath("[2].audit.createDatetime").isEqualTo("2021-01-01T10:00:00")
          .jsonPath("[3].beliefId").isEqualTo(belief1.beliefId)
          .jsonPath("[3].belief.code").isEqualTo("ZORO")
          .jsonPath("[3].belief.description").isEqualTo("Zoroastrian")
          .jsonPath("[3].startDate").isEqualTo("2018-01-01")
          .jsonPath("[3].endDate").isEqualTo("2019-02-03")
          .jsonPath("[3].changeReason").doesNotExist()
          .jsonPath("[3].comments").doesNotExist()
          .jsonPath("[3].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("[3].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }

      @Test
      fun `will return beliefs when current alias is different from root offender`() {
        webTestClient.get().uri("/core-person/${offender2.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("[0].belief.code").isEqualTo("JAIN")
          .jsonPath("[0].belief.description").isEqualTo("Jain")
          .jsonPath("[0].startDate").isEqualTo("2021-01-01")
          .jsonPath("[0].endDate").doesNotExist()
          .jsonPath("[0].changeReason").isEqualTo(true)
          .jsonPath("[0].comments").isEqualTo("No longer believes in Zoroastrianism")
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/identifier/{sequenceNumber}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetIdentifier {
    private lateinit var offender: Offender
    private lateinit var alias: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offender = offender(
          nomsId = "D5678EF",
          firstName = "JANE",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
        ) {
          identifier(
            type = "PNC",
            identifier = "20/0071818T",
            issuedAuthority = "Met Police",
            issuedDate = LocalDate.parse("2020-01-01"),
            verified = true,
          )
          identifier(type = "STAFF", identifier = "123")
          alias = alias {
            identifier(type = "DL", identifier = "NARK991222")
          }
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/999999/identifier/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when identifier not found`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return identifier by offender id and sequence number`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(1)
          .jsonPath("identifier").isEqualTo("20/0071818T")
          .jsonPath("type.code").isEqualTo("PNC")
          .jsonPath("type.description").isEqualTo("PNC Number")
          .jsonPath("issuedAuthority").isEqualTo("Met Police")
          .jsonPath("issuedDate").isEqualTo("2020-01-01")
          .jsonPath("verified").isEqualTo(true)
      }

      @Test
      fun `will return second identifier with correct data`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifier/2")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(2)
          .jsonPath("identifier").isEqualTo("123")
          .jsonPath("type.code").isEqualTo("STAFF")
          .jsonPath("type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("issuedAuthority").doesNotExist()
          .jsonPath("issuedDate").doesNotExist()
          .jsonPath("verified").isEqualTo(false)
      }

      @Test
      fun `will return identifier for alias`() {
        webTestClient.get().uri("/core-person/${alias.id}/identifier/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("sequence").isEqualTo(1)
          .jsonPath("identifier").isEqualTo("NARK991222")
          .jsonPath("type.code").isEqualTo("DL")
          .jsonPath("type.description").isEqualTo("Driving Licence")
          .jsonPath("issuedAuthority").doesNotExist()
          .jsonPath("issuedDate").doesNotExist()
          .jsonPath("verified").isEqualTo(false)
      }

      @Test
      fun `is able to re-hydrate the identifier`() {
        val identifier = webTestClient.get().uri("/core-person/${offender.id}/identifier/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<Identifier>().responseBody.blockFirst()!!

        assertThat(identifier.sequence).isEqualTo(1)
        assertThat(identifier.identifier).isEqualTo("20/0071818T")
        assertThat(identifier.type).isEqualTo(CodeDescription(code = "PNC", description = "PNC Number"))
        assertThat(identifier.issuedAuthority).isEqualTo("Met Police")
        assertThat(identifier.issuedDate).isEqualTo(LocalDate.parse("2020-01-01"))
        assertThat(identifier.verified).isTrue()
      }
    }
  }

  @DisplayName("GET /core-person/alias/{offenderId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetAlias {
    private lateinit var offenderWithoutBooking: Offender
    private lateinit var offenderWithActiveAlias: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        offenderWithoutBooking = offender(
          nomsId = "F5678GH",
          firstName = "JANE",
          lastName = "NARK",
        )
        offenderWithActiveAlias = offender(
          nomsId = "G5678HJ",
          firstName = "JOHN",
          lastName = "BARK",
        ) {
          booking(bookingSequence = 2, active = false) { }
          alias {
            booking(bookingSequence = 1, active = true) { }
          }
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/alias/${offenderWithoutBooking.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/alias/${offenderWithoutBooking.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/alias/${offenderWithoutBooking.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/alias/999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns the alias when there is no booking`() {
        webTestClient.get().uri("/core-person/alias/${offenderWithoutBooking.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("offenderId").isEqualTo(offenderWithoutBooking.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("workingName").isEqualTo(true)
          .jsonPath("identifiers.length()").isEqualTo(0)
      }

      @Test
      fun `marks the alias as not working name (not active) when a different alias is current`() {
        webTestClient.get().uri("/core-person/alias/${offenderWithActiveAlias.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("offenderId").isEqualTo(offenderWithActiveAlias.id)
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("lastName").isEqualTo("BARK")
          .jsonPath("workingName").isEqualTo(false)
          .jsonPath("identifiers.length()").isEqualTo(0)
      }

      @Test
      fun `is able to re-hydrate the alias offender`() {
        val alias = webTestClient.get().uri("/core-person/alias/${offenderWithoutBooking.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<CoreOffender>().responseBody.blockFirst()!!

        assertThat(alias.offenderId).isEqualTo(offenderWithoutBooking.id)
        assertThat(alias.workingName).isTrue()
        assertThat(alias.identifiers).hasSize(0)
      }
    }
  }
}
