package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhoneRepository
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson.OffenderBelief as OffenderBeliefCorePerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone as OffenderAddressPhoneJpa
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress as OffenderAddressJpa

class CorePersonResourceIntTest(
  @Autowired private val offenderInternetAddressRepository: OffenderInternetAddressRepository,
  @Autowired private val offenderPhoneRepository: OffenderPhoneRepository,
  @Autowired private val offenderAddressRepository: OffenderAddressRepository,
  @Autowired private val addressPhoneRepository: AddressPhoneRepository,
) : IntegrationTestBase() {
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
            suffixCode = "SR",
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

      @Test
      fun `will return basic offender data`() {
        val result = webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offenderMinimal.nomsId)
        with(result.offenders!![0]) {
          assertThat(offenderId).isEqualTo(offenderMinimal.id)
          assertThat(title).isNull()
          assertThat(suffix).isNull()
          assertThat(firstName).isEqualTo("JOHN")
          assertThat(middleName1).isNull()
          assertThat(middleName2).isNull()
          assertThat(lastName).isEqualTo("BOG")
          assertThat(dateOfBirth).isNull()
          assertThat(birthPlace).isNull()
          assertThat(birthCountry).isNull()
          assertThat(ethnicity).isNull()
          assertThat(sex).isEqualTo(CodeDescription("M", "Male"))
          assertThat(nameType).isNull()
          assertThat(workingName).isTrue()
        }
        assertThat(result.inOutStatus).isEqualTo("OUT")
        assertThat(result.activeFlag).isFalse()
        assertThat(result.addresses).isNull()
        assertThat(result.phoneNumbers).isNull()
        assertThat(result.emailAddresses).isNull()
        assertThat(result.beliefs).isNull()
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
        val result = webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offenderFull.nomsId)
        assertThat(result.inOutStatus).isEqualTo("IN")
        assertThat(result.activeFlag).isTrue()
        with(result.offenders!![0]) {
          assertThat(offenderId).isEqualTo(offenderFull.id)
          assertThat(title).isEqualTo(CodeDescription("MRS", "Mrs"))
          assertThat(suffix).isEqualTo(CodeDescription("SR", "Senior"))
          assertThat(firstName).isEqualTo("JANE")
          assertThat(middleName1).isEqualTo("Mary")
          assertThat(middleName2).isEqualTo("Ann")
          assertThat(lastName).isEqualTo("NARK")
          assertThat(dateOfBirth).isEqualTo(LocalDate.parse("1999-12-22"))
          assertThat(birthPlace).isEqualTo("LONDON")
          assertThat(birthCountry).isEqualTo(CodeDescription("ATA", "Antarctica"))
          assertThat(ethnicity).isEqualTo(CodeDescription("M3", "Mixed: White and Asian"))
          assertThat(sex).isEqualTo(CodeDescription("F", "Female"))
          assertThat(nameType).isEqualTo(CodeDescription("MAID", "Maiden"))
          assertThat(createDate).isEqualTo(LocalDate.parse("2020-03-20"))
          assertThat(workingName).isTrue()
        }
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
              suffixCode = "JR",
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
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offender.nomsId)
        assertThat(result.offenders)
          .extracting("offenderId", "firstName", "lastName", "workingName")
          .containsExactly(
            tuple(offender.id, "JANE", "NARK", true),
            tuple(alias.id, "LEKAN", "NTHANDA", false),
          )
        with(result.offenders!![1]) {
          assertThat(title).isEqualTo(CodeDescription("MR", "Mr"))
          assertThat(suffix).isEqualTo(CodeDescription("JR", "Junior"))
          assertThat(middleName1).isEqualTo("Fred")
          assertThat(middleName2).isEqualTo("Johas")
          assertThat(dateOfBirth).isEqualTo(LocalDate.parse("1965-07-19"))
          assertThat(ethnicity).isEqualTo(CodeDescription("M1", "Mixed: White and Black Caribbean"))
          assertThat(sex).isEqualTo(CodeDescription("M", "Male"))
        }
      }

      @Test
      fun `will set working name to offender record linked to active booking`() {
        val result = webTestClient.get().uri("/core-person/${offender2.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offender2.nomsId)
        assertThat(result.offenders)
          .extracting("offenderId", "firstName", "lastName", "workingName")
          .containsExactly(
            tuple(offender2.id, "JOHN", "BARK", false),
            tuple(alias2.id, "AJOHN", "ABARK", true),
          )
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
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW"))).exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.offenders!![0].identifiers)
          .extracting("offenderId", "sequence", "identifier", "type", "issuedAuthority", "issuedDate", "verified")
          .containsExactly(
            tuple(offender.id, 1L, "20/0071818T", CodeDescription("PNC", "PNC Number"), "Met Police", LocalDate.parse("2020-01-01"), true),
            tuple(offender.id, 2L, "123", CodeDescription("STAFF", "Staff Pass/ Identity Card"), null, null, false),
          )
        assertThat(result.offenders!![1].identifiers)
          .extracting("offenderId", "sequence", "identifier", "type", "issuedAuthority", "issuedDate", "verified")
          .containsExactly(
            tuple(alias.id, 1L, "456", CodeDescription("STAFF", "Staff Pass/ Identity Card"), null, null, false),
          )
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class Addresses {
      private lateinit var offender: Offender
      private lateinit var offenderWithActiveAlias: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            address(
              premise = null,
              street = null,
              locality = null,
              postcode = "S1 3GG",
            )
            address(
              flat = "3B",
              premise = "Brown Court",
              street = "Scotland Street",
              locality = "Hunters Bar",
              postcode = "S1 3GG",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
              noFixedAddress = false,
              primaryAddress = true,
              mailAddress = true,
              comment = "Not to be used",
              startDate = "2024-10-01",
              endDate = "2024-11-01",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              whoModified = "MODIFIER",
              whenModified = LocalDateTime.parse("2021-02-02T11:00"),
            ) {
              phone(
                phoneType = "MOB",
                phoneNo = "07399999999",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
              usage(usageCode = "DAP", active = true)
              usage(usageCode = "CURFEW", active = false)
              // following row will be filtered out as invalid
              usage(usageCode = "NOT_FOUND", active = false)
            }
            address(
              noFixedAddress = true,
              primaryAddress = false,
              premise = null,
              street = null,
              locality = null,
            )
          }
          offenderWithActiveAlias = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BK",
          ) {
            address(
              type = "HOME",
              flat = "3B",
              premise = "Brown Court",
              postcode = "S1 3GG",
            )
            booking(bookingSequence = 2) { release() }
            alias {
              booking(bookingSequence = 1) { }
            }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return addresses`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.addresses!![0]) {
          assertThat(addressId).isEqualTo(offender.addresses[0].addressId)
          assertThat(flat).isNull()
          assertThat(premise).isNull()
          assertThat(street).isNull()
          assertThat(locality).isNull()
          assertThat(city).isNull()
          assertThat(county).isNull()
          assertThat(country).isNull()
          assertThat(noFixedAddress).isNull()
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isFalse()
          assertThat(comment).isNull()
          assertThat(startDate).isNull()
          assertThat(endDate).isNull()
          assertThat(phoneNumbers).isNull()
        }
        with(result.addresses!![1]) {
          assertThat(addressId).isEqualTo(offender.addresses[1].addressId)
          assertThat(flat).isEqualTo("3B")
          assertThat(premise).isEqualTo("Brown Court")
          assertThat(street).isEqualTo("Scotland Street")
          assertThat(locality).isEqualTo("Hunters Bar")
          assertThat(postcode).isEqualTo("S1 3GG")
          assertThat(city).isEqualTo(CodeDescription("25343", "Sheffield"))
          assertThat(county).isEqualTo(CodeDescription("S.YORKSHIRE", "South Yorkshire"))
          assertThat(country).isEqualTo(CodeDescription("ENG", "England"))
          assertThat(noFixedAddress).isFalse()
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(comment).isEqualTo("Not to be used")
          assertThat(startDate).isEqualTo(LocalDate.parse("2024-10-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2024-11-01"))
          assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
          assertThat(createdByUsername).isEqualTo("KOFEADDY")
          assertThat(lastUpdatedDateTime).isEqualTo(LocalDateTime.parse("2021-02-02T11:00:00"))
          assertThat(lastUpdatedByUsername).isEqualTo("MODIFIER")
        }
        assertThat(result.addresses!![2].noFixedAddress).isTrue()
      }

      @Test
      fun `will return addresses for offender with active alias`() {
        val result = webTestClient.get().uri("/core-person/${offenderWithActiveAlias.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.addresses!![0]) {
          assertThat(addressId).isEqualTo(offenderWithActiveAlias.addresses[0].addressId)
          assertThat(flat).isEqualTo("3B")
          assertThat(premise).isEqualTo("Brown Court")
          assertThat(postcode).isEqualTo("S1 3GG")
        }
      }

      @Test
      fun `will return phone numbers associated with addresses`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.addresses!![0].phoneNumbers).isNull()
        val phoneNumbers = result.addresses!![1].phoneNumbers!!
        with(phoneNumbers[0]) {
          assertThat(phoneId).isEqualTo(offender.addresses[1].phones[0].phoneId)
          assertThat(type).isEqualTo(CodeDescription("MOB", "Mobile"))
          assertThat(number).isEqualTo("07399999999")
          assertThat(extension).isNull()
          assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
          assertThat(createdByUsername).isEqualTo("KOFEADDY")
          assertThat(lastUpdatedDateTime).isNull()
          assertThat(lastUpdatedByUsername).isNull()
        }
        with(phoneNumbers[1]) {
          assertThat(phoneId).isEqualTo(offender.addresses[1].phones[1].phoneId)
          assertThat(type).isEqualTo(CodeDescription("HOME", "Home"))
          assertThat(number).isEqualTo("01142561919")
          assertThat(extension).isEqualTo("123")
        }
      }

      @Test
      fun `will return address usages associated with addresses`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.addresses!![0].usages).isNull()
        val usages = result.addresses!![1].usages!!
        with(usages[0]) {
          assertThat(addressId).isEqualTo(offender.addresses[1].addressId)
          assertThat(usage).isEqualTo("CURFEW")
          assertThat(active).isFalse()
          assertThat(createdDateTime).isNotNull()
          assertThat(createdByUsername).isNotEmpty()
          assertThat(lastUpdatedDateTime).isNull()
          assertThat(lastUpdatedByUsername).isNull()
        }
        with(usages[1]) {
          assertThat(addressId).isEqualTo(offender.addresses[1].addressId)
          assertThat(usage).isEqualTo("DAP")
          assertThat(active).isTrue()
        }
        assertThat(usages[2].usage).isEqualTo("NOT_FOUND")
      }

      @Test
      fun `is able to re-hydrate the core person`() {
        val person = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<CorePerson>().responseBody.blockFirst()!!

        assertThat(person.prisonNumber).isEqualTo(offender.nomsId)
        assertThat(person.addresses?.get(0)?.usages).isNull()
        assertThat(person.addresses?.get(1)?.usages).hasSize(3)
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class OffenderPhoneNumbers {
      private lateinit var offender: Offender
      private lateinit var offenderWithActiveAlias: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            phone(
              phoneType = "MOB",
              phoneNo = "07399999999",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
          }
          offenderWithActiveAlias = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BD",
          ) {
            phone(phoneType = "HOME", phoneNo = "01142561919")
            booking(bookingSequence = 2) { release() }
            alias {
              booking(bookingSequence = 1) { }
            }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return phone numbers`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.phoneNumbers!![0]) {
          assertThat(phoneId).isEqualTo(offender.phones[0].phoneId)
          assertThat(type).isEqualTo(CodeDescription("MOB", "Mobile"))
          assertThat(number).isEqualTo("07399999999")
          assertThat(extension).isNull()
          assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
          assertThat(createdByUsername).isEqualTo("KOFEADDY")
          assertThat(lastUpdatedDateTime).isNull()
          assertThat(lastUpdatedByUsername).isNull()
        }
        with(result.phoneNumbers!![1]) {
          assertThat(phoneId).isEqualTo(offender.phones[1].phoneId)
          assertThat(type).isEqualTo(CodeDescription("HOME", "Home"))
          assertThat(number).isEqualTo("01142561919")
          assertThat(extension).isEqualTo("123")
        }
      }

      @Test
      fun `will return phone numbers for offender with active alias`() {
        val result = webTestClient.get().uri("/core-person/${offenderWithActiveAlias.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.phoneNumbers!![0]) {
          assertThat(phoneId).isEqualTo(offenderWithActiveAlias.phones[0].phoneId)
          assertThat(type).isEqualTo(CodeDescription("HOME", "Home"))
          assertThat(number).isEqualTo("01142561919")
        }
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class OffenderEmailAddresses {
      private lateinit var offender: Offender
      private lateinit var offenderWithActiveAlias: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            email(
              emailAddress = "john.bog@justice.gov.uk",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            email(emailAddress = "john.bog@gmail.com")
          }
          offenderWithActiveAlias = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BE",
          ) {
            email(emailAddress = "0114@2561919.com")
            booking(bookingSequence = 2) { release() }
            alias {
              booking(bookingSequence = 1) { }
            }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return email address`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.emailAddresses!![0]) {
          assertThat(emailAddressId).isEqualTo(offender.internetAddresses[0].internetAddressId)
          assertThat(email).isEqualTo("john.bog@justice.gov.uk")
          assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
          assertThat(createdByUsername).isEqualTo("KOFEADDY")
          assertThat(lastUpdatedDateTime).isNull()
          assertThat(lastUpdatedByUsername).isNull()
        }
        with(result.emailAddresses!![1]) {
          assertThat(emailAddressId).isEqualTo(offender.internetAddresses[1].internetAddressId)
          assertThat(email).isEqualTo("john.bog@gmail.com")
        }
      }

      @Test
      fun `will return email address for offender with active alias`() {
        val result = webTestClient.get().uri("/core-person/${offenderWithActiveAlias.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        with(result.emailAddresses!![0]) {
          assertThat(emailAddressId).isEqualTo(offenderWithActiveAlias.internetAddresses[0].internetAddressId)
          assertThat(email).isEqualTo("0114@2561919.com")
        }
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
            booking {
              belief3 = belief(
                beliefCode = "DRU",
                startDate = LocalDate.parse("2023-01-01"),
                changeReason = false,
                verified = false,
              )
              release()
            }
          }
          offender2 = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BF",
          ) {
            booking(bookingSequence = 2) {
              belief(
                beliefCode = "JAIN",
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
              )
              release()
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
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.beliefs)
          .extracting("beliefId", "belief", "startDate", "changeReason", "comments")
          .containsExactly(
            tuple(belief3.beliefId, CodeDescription("DRU", "Druid"), LocalDate.parse("2023-01-01"), false, null),
            tuple(belief2.beliefId, CodeDescription("JAIN", "Jain"), LocalDate.parse("2021-01-01"), true, "No longer believes in Zoroastrianism"),
            tuple(belief1.beliefId, CodeDescription("ZORO", "Zoroastrian"), LocalDate.parse("2018-01-01"), null, null),
          )
        with(result.beliefs!![1]) {
          assertThat(endDate).isNull()
          assertThat(audit.createUsername).isNotEmpty()
          assertThat(audit.createDatetime).isNotNull()
        }
        with(result.beliefs!![2]) {
          assertThat(endDate).isEqualTo(LocalDate.parse("2019-02-03"))
          assertThat(audit.createUsername).isEqualTo("KOFEADDY")
          assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
        }
      }

      @Test
      fun `will return beliefs when current alias is different from root offender`() {
        val result = webTestClient.get().uri("/core-person/${offender2.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.beliefs)
          .extracting("belief", "startDate", "endDate", "changeReason", "comments")
          .containsExactly(
            tuple(CodeDescription("JAIN", "Jain"), LocalDate.parse("2021-01-01"), null, true, "No longer believes in Zoroastrianism"),
          )
      }
    }
  }

  @DisplayName("GET /core-person/{prisonNumber}/reconciliation")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderForReconciliation {
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
          nomsId = "R1234BC",
          firstName = "JOHN",
          lastName = "BOG",
        )
        offenderFull = offender(
          nomsId = "R1234BD",
          firstName = "JANE",
          lastName = "NARK",
        ) {
          booking {
            belief = belief(
              beliefCode = "JAIN",
              changeReason = true,
              comments = "No longer believes in Zoroastrianism",
              verified = true,
            )
          }
          phone(phoneType = "MOB", phoneNo = "07399999999")
          email(emailAddress = "jane.nark@justice.gov.uk")
          address(
            type = "HOME",
            flat = "3B",
            premise = "Brown Court",
            street = "Scotland Street",
            locality = "Hunters Bar",
            postcode = "S1 3GG",
            primaryAddress = true,
            mailAddress = true,
          ) {
            usage(usageCode = "DAP", active = true)
            // following row is a discharge address usage and will be filtered out
            usage(usageCode = "DISC", active = true)
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
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/reconciliation")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic reconciliation data when there is no related data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderMinimal.nomsId)
          .jsonPath("inOutStatus").doesNotExist()
          .jsonPath("activeFlag").isEqualTo(false)
          .jsonPath("offenders").doesNotExist()
          .jsonPath("addresses").doesNotExist()
          .jsonPath("phoneNumbers").doesNotExist()
          .jsonPath("emailAddresses").doesNotExist()
          .jsonPath("beliefs").doesNotExist()
      }

      @Test
      fun `will return addresses, phone numbers, email addresses and beliefs`() {
        val result = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offenderFull.nomsId)
        assertThat(result.inOutStatus).isNull()
        assertThat(result.activeFlag).isFalse()
        assertThat(result.offenders).isNull()
        with(result.addresses!![0]) {
          assertThat(addressId).isEqualTo(offenderFull.addresses[0].addressId)
          assertThat(flat).isEqualTo("3B")
          assertThat(premise).isEqualTo("Brown Court")
          assertThat(street).isEqualTo("Scotland Street")
          assertThat(locality).isEqualTo("Hunters Bar")
          assertThat(postcode).isEqualTo("S1 3GG")
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
        }
        with(result.phoneNumbers!![0]) {
          assertThat(phoneId).isEqualTo(offenderFull.phones[0].phoneId)
          assertThat(type.code).isEqualTo("MOB")
          assertThat(number).isEqualTo("07399999999")
        }
        with(result.emailAddresses!![0]) {
          assertThat(emailAddressId).isEqualTo(offenderFull.internetAddresses[0].internetAddressId)
          assertThat(email).isEqualTo("jane.nark@justice.gov.uk")
        }
        assertThat(result.beliefs)
          .extracting("beliefId", "belief", "comments")
          .containsExactly(
            tuple(belief.beliefId, CodeDescription("JAIN", "Jain"), "No longer believes in Zoroastrianism"),
          )
      }

      @Test
      fun `will not return DISC address usages`() {
        val result = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.addresses!![0].usages)
          .extracting("usage")
          .containsExactly("DAP")
          .doesNotContain("DISC")
      }

      @Test
      fun `is able to re-hydrate the core person for reconciliation`() {
        val person = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<CorePerson>().responseBody.blockFirst()!!

        assertThat(person.prisonNumber).isEqualTo(offenderFull.nomsId)
        assertThat(person.inOutStatus).isNull()
        assertThat(person.activeFlag).isFalse()
        assertThat(person.offenders).isNull()
        assertThat(person.addresses).hasSize(1)
        assertThat(person.phoneNumbers).hasSize(1)
        assertThat(person.emailAddresses).hasSize(1)
        assertThat(person.beliefs).hasSize(1)
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class WithAlias {
      private lateinit var offender: Offender

      @BeforeAll
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            nomsId = "R1234BE",
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            address(
              premise = "Root House",
              street = "Root Street",
              locality = "Root Locality",
            )
            booking(bookingSequence = 2) { release() }
            alias(
              firstName = "AJOHN",
              lastName = "ABARK",
            ) {
              booking(bookingSequence = 1) { }
            }
          }
        }
      }

      @AfterAll
      fun tearDown(): Unit = deleteAll()

      @Test
      fun `will return addresses from the root offender even when current alias is different`() {
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}/reconciliation")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePerson>()
          .returnResult().responseBody!!

        assertThat(result.prisonNumber).isEqualTo(offender.nomsId)
        with(result.addresses!![0]) {
          assertThat(addressId).isEqualTo(offender.addresses[0].addressId)
          assertThat(premise).isEqualTo("Root House")
        }
        assertThat(result.offenders).isNull()
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
          booking(bookingSequence = 2) { release() }
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
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}/aliases-identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<CoreOffender>>()
          .returnResult().responseBody!!

        assertThat(result)
          .extracting("offenderId", "firstName", "lastName", "workingName")
          .containsExactly(
            tuple(offender.id, "JANE", "NARK", false),
            tuple(alias.id, "AJOHN", "ABARK", true),
          )
        assertThat(result[0].identifiers)
          .extracting("sequence", "identifier", "type", "issuedAuthority", "issuedDate", "verified")
          .containsExactly(
            tuple(1L, "20/0071818T", CodeDescription("PNC", "PNC Number"), "Met Police", LocalDate.parse("2020-01-01"), true),
          )
        assertThat(result[1].identifiers)
          .extracting("offenderId", "sequence", "identifier", "type", "issuedAuthority", "issuedDate", "verified")
          .containsExactly(
            tuple(alias.id, 1L, "123", CodeDescription("STAFF", "Staff Pass/ Identity Card"), null, null, false),
          )
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
        val result = webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<OffenderBeliefCorePerson>>()
          .returnResult().responseBody!!

        assertThat(result).isEmpty()
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
            booking {
              belief3 = belief(
                beliefCode = "DRU",
                startDate = LocalDate.parse("2023-01-01"),
                changeReason = false,
                verified = false,
              )
              release()
            }
          }
          offender2 = offender(
            firstName = "JOHN",
            lastName = "BOG",
            nomsId = "A1234BF",
          ) {
            booking(bookingSequence = 2) {
              belief(
                beliefCode = "JAIN",
                changeReason = true,
                comments = "No longer believes in Zoroastrianism",
                verified = true,
              )
              release()
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
        val result = webTestClient.get().uri("/core-person/${offender.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<OffenderBeliefCorePerson>>()
          .returnResult().responseBody!!

        assertThat(result)
          .extracting("beliefId", "belief", "startDate")
          .containsExactly(
            tuple(belief3.beliefId, CodeDescription("DRU", "Druid"), LocalDate.parse("2023-01-01")),
            tuple(belief2.beliefId, CodeDescription("JAIN", "Jain"), LocalDate.parse("2018-01-01")),
            tuple(belief4.beliefId, CodeDescription("SATN", "Satanism"), LocalDate.parse("2018-01-01")),
            tuple(belief1.beliefId, CodeDescription("ZORO", "Zoroastrian"), LocalDate.parse("2018-01-01")),
          )
        with(result[0]) {
          assertThat(changeReason).isFalse()
          assertThat(comments).isNull()
        }
        with(result[1]) {
          assertThat(endDate).isNull()
          assertThat(changeReason).isTrue()
          assertThat(comments).isEqualTo("No longer believes in Zoroastrianism")
          assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2022-01-01T10:00:00"))
        }
        with(result[2]) {
          assertThat(endDate).isEqualTo(LocalDate.parse("2018-01-01"))
          assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2021-01-01T10:00:00"))
        }
        with(result[3]) {
          assertThat(endDate).isEqualTo(LocalDate.parse("2019-02-03"))
          assertThat(changeReason).isNull()
          assertThat(comments).isNull()
          assertThat(audit.createUsername).isEqualTo("KOFEADDY")
          assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
        }
      }

      @Test
      fun `will return beliefs when current alias is different from root offender`() {
        val result = webTestClient.get().uri("/core-person/${offender2.nomsId}/religions")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<OffenderBeliefCorePerson>>()
          .returnResult().responseBody!!

        assertThat(result)
          .extracting("belief", "startDate", "endDate", "changeReason", "comments")
          .containsExactly(
            tuple(CodeDescription("JAIN", "Jain"), LocalDate.parse("2021-01-01"), null, true, "No longer believes in Zoroastrianism"),
          )
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/religion")
  @Nested
  @TestInstance(PER_CLASS)
  inner class InsertOffenderReligion {
    private lateinit var offender: Offender
    private lateinit var existingBelief: OffenderBelief

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(
          nomsId = "C1234DE",
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          booking {
            existingBelief = belief(
              beliefCode = "JAIN",
              startDate = LocalDate.parse("2018-01-01"),
            )
          }
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    private fun request() = CorePersonInsertReligionRequest(
      beliefCode = "ZORO",
      startDate = LocalDate.parse("2024-01-01"),
      comments = "Change of belief",
    )

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request())
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.post().uri("/core-person/UNKNOWN/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request())
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when belief code not found`() {
        webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request().copy(beliefCode = "UNKNOWN"))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Belief code UNKNOWN does not exist")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `inserts a new belief and ends the existing belief`() {
        val religionRequest = request()

        val beliefId = webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(religionRequest)
          .exchange()
          .expectStatus().isCreated
          .expectBody<Long>()
          .returnResult()
          .responseBody!!

        val beliefs = repository.offenderBeliefRepository.findBeliefsByPrisonNumber(offender.nomsId)
        assertThat(beliefs).hasSize(2)
        assertThat(repository.offenderBeliefRepository.findByIdOrNull(existingBelief.beliefId)!!.endDate)
          .isEqualTo(religionRequest.startDate)
        val insertedBelief = beliefs.first { it.beliefId != existingBelief.beliefId }
        assertThat(beliefId).isEqualTo(insertedBelief.beliefId)
        assertThat(insertedBelief.beliefCode.id.code).isEqualTo(religionRequest.beliefCode)
        assertThat(insertedBelief.startDate).isEqualTo(religionRequest.startDate)
        assertThat(insertedBelief.changeReason).isTrue()
        assertThat(insertedBelief.comments).isEqualTo(religionRequest.comments)
      }

      @Test
      fun `comments are truncated to the maximum length`() {
        val religionRequest = request().copy(comments = "A".repeat(4001))

        webTestClient.post().uri("/core-person/${offender.nomsId}/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(religionRequest)
          .exchange()
          .expectStatus().isCreated

        val insertedBelief = repository.offenderBeliefRepository.findBeliefsByPrisonNumber(offender.nomsId).first()
        assertThat(insertedBelief.comments).isEqualTo("${"A".repeat(3975)}... see DPS for full text")
        assertThat(insertedBelief.comments).hasSize(4000)
      }
    }
  }

  @DisplayName("GET /core-person/{prisonNumber}/addresses-contacts")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderAddressesContacts {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        offenderMinimal = offender(
          nomsId = "H5678JK",
          firstName = "JOHN",
          lastName = "BOG",
        )
        offenderFull = offender(
          nomsId = "H5678JL",
          firstName = "JANE",
          lastName = "NARK",
        ) {
          phone(phoneType = "MOB", phoneNo = "07399999999")
          email(
            emailAddress = "jane.nark@justice.gov.uk",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          email(emailAddress = "jane.nark@gmail.com")
          address(
            premise = null,
            street = null,
            locality = null,
            postcode = "S1 3GG",
          )
          address(
            flat = "3B",
            premise = "Brown Court",
            street = "Scotland Street",
            locality = "Hunters Bar",
            postcode = "S1 3GG",
            city = SHEFFIELD,
            county = "S.YORKSHIRE",
            country = "ENG",
            noFixedAddress = false,
            primaryAddress = true,
            mailAddress = true,
            comment = "Not to be used",
            startDate = "2024-10-01",
            endDate = "2024-11-01",
          ) {
            phone(phoneType = "MOB", phoneNo = "07399999999")
            phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
            usage(usageCode = "DAP", active = true)
            usage(usageCode = "CURFEW", active = false)
            usage(usageCode = "NOT_FOUND", active = false)
            // following row is a discharge address usage and will be filtered out
            usage(usageCode = "DISC", active = true)
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

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/addresses-contacts")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return empty list if no data`() {
        val result = webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePersonAddressContact>()
          .returnResult().responseBody!!

        assertThat(result.addresses).isNull()
        assertThat(result.phoneNumbers).isNull()
        assertThat(result.emailAddresses).isNull()
      }

      @Test
      fun `will return addresses`() {
        val result = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePersonAddressContact>()
          .returnResult().responseBody!!

        with(result.addresses!![0]) {
          assertThat(addressId).isEqualTo(offenderFull.addresses[0].addressId)
          assertThat(flat).isNull()
          assertThat(premise).isNull()
          assertThat(street).isNull()
          assertThat(locality).isNull()
          assertThat(city).isNull()
          assertThat(county).isNull()
          assertThat(country).isNull()
          assertThat(noFixedAddress).isNull()
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isFalse()
          assertThat(comment).isNull()
          assertThat(startDate).isNull()
          assertThat(endDate).isNull()
          assertThat(phoneNumbers).isNull()
        }
        with(result.addresses!![1]) {
          assertThat(addressId).isEqualTo(offenderFull.addresses[1].addressId)
          assertThat(flat).isEqualTo("3B")
          assertThat(premise).isEqualTo("Brown Court")
          assertThat(street).isEqualTo("Scotland Street")
          assertThat(locality).isEqualTo("Hunters Bar")
          assertThat(postcode).isEqualTo("S1 3GG")
          assertThat(city).isEqualTo(CodeDescription("25343", "Sheffield"))
          assertThat(county).isEqualTo(CodeDescription("S.YORKSHIRE", "South Yorkshire"))
          assertThat(country).isEqualTo(CodeDescription("ENG", "England"))
          assertThat(noFixedAddress).isFalse()
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(comment).isEqualTo("Not to be used")
          assertThat(startDate).isEqualTo(LocalDate.parse("2024-10-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2024-11-01"))
          assertThat(phoneNumbers)
            .extracting("phoneId", "type", "number", "extension")
            .containsExactly(
              tuple(offenderFull.addresses[1].phones[0].phoneId, CodeDescription("MOB", "Mobile"), "07399999999", null),
              tuple(offenderFull.addresses[1].phones[1].phoneId, CodeDescription("HOME", "Home"), "01142561919", "123"),
            )
          assertThat(usages!!.take(2))
            .extracting("addressId", "usage", "active")
            .containsExactly(
              tuple(offenderFull.addresses[1].addressId, "CURFEW", false),
              tuple(offenderFull.addresses[1].addressId, "DAP", true),
            )
          assertThat(usages!![2].usage).isEqualTo("NOT_FOUND")
        }
        assertThat(result.addresses!![2].noFixedAddress).isTrue()
        with(result.phoneNumbers!![0]) {
          assertThat(phoneId).isEqualTo(offenderFull.phones[0].phoneId)
          assertThat(type).isEqualTo(CodeDescription("MOB", "Mobile"))
          assertThat(number).isEqualTo("07399999999")
          assertThat(extension).isNull()
        }
        with(result.emailAddresses!![0]) {
          assertThat(emailAddressId).isEqualTo(offenderFull.internetAddresses[0].internetAddressId)
          assertThat(email).isEqualTo("jane.nark@justice.gov.uk")
          assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2020-01-01T10:00:00"))
          assertThat(createdByUsername).isEqualTo("KOFEADDY")
          assertThat(lastUpdatedDateTime).isNull()
          assertThat(lastUpdatedByUsername).isNull()
        }
        with(result.emailAddresses!![1]) {
          assertThat(emailAddressId).isEqualTo(offenderFull.internetAddresses[1].internetAddressId)
          assertThat(email).isEqualTo("jane.nark@gmail.com")
        }
      }

      @Test
      fun `will not return DISC address usages`() {
        val result = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CorePersonAddressContact>()
          .returnResult().responseBody!!

        assertThat(result.addresses!![1].usages)
          .extracting("usage")
          .containsExactly("CURFEW", "DAP", "NOT_FOUND")
          .doesNotContain("DISC")
      }

      @Test
      fun `is able to re-hydrate the addresses`() {
        val addressesAndContacts = webTestClient.get().uri("/core-person/${offenderFull.nomsId}/addresses-contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<CorePersonAddressContact>().responseBody.blockFirst()!!

        val addresses = addressesAndContacts.addresses!!
        assertThat(addresses).hasSize(3)
        assertThat(addresses[1].postcode).isEqualTo("S1 3GG")
        assertThat(addresses[1].phoneNumbers).hasSize(2)
        assertThat(addresses[1].usages).hasSize(3)
        assertThat(addressesAndContacts.phoneNumbers).hasSize(1)
        assertThat(addressesAndContacts.emailAddresses).hasSize(2)
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
          .jsonPath("offenderId").isEqualTo(offender.id)
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
          .jsonPath("offenderId").isEqualTo(offender.id)
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
          .jsonPath("offenderId").isEqualTo(alias.id)
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

        assertThat(identifier.offenderId).isEqualTo(offender.id)
        assertThat(identifier.sequence).isEqualTo(1)
        assertThat(identifier.identifier).isEqualTo("20/0071818T")
        assertThat(identifier.type).isEqualTo(CodeDescription(code = "PNC", description = "PNC Number"))
        assertThat(identifier.issuedAuthority).isEqualTo("Met Police")
        assertThat(identifier.issuedDate).isEqualTo(LocalDate.parse("2020-01-01"))
        assertThat(identifier.verified).isTrue()
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/identifiers")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetIdentifiers {
    private lateinit var offender: Offender
    private lateinit var alias: Offender
    private lateinit var aliasWithoutIdentifiers: Offender

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
          aliasWithoutIdentifiers = alias()
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifiers")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifiers")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offender.id}/identifiers")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/999999/identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all identifiers for offender in sequence order`() {
        val result = webTestClient.get().uri("/core-person/${offender.id}/identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<Identifier>>()
          .returnResult().responseBody!!

        assertThat(result)
          .extracting("offenderId", "sequence", "identifier", "type", "issuedAuthority", "issuedDate", "verified")
          .containsExactly(
            tuple(offender.id, 1L, "20/0071818T", CodeDescription("PNC", "PNC Number"), "Met Police", LocalDate.parse("2020-01-01"), true),
            tuple(offender.id, 2L, "123", CodeDescription("STAFF", "Staff Pass/ Identity Card"), null, null, false),
          )
      }

      @Test
      fun `will return only identifiers for the alias`() {
        val result = webTestClient.get().uri("/core-person/${alias.id}/identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<Identifier>>()
          .returnResult().responseBody!!

        assertThat(result)
          .extracting("offenderId", "sequence", "identifier", "type.code")
          .containsExactly(
            tuple(alias.id, 1L, "NARK991222", "DL"),
          )
      }

      @Test
      fun `will return empty list when offender has no identifiers`() {
        val result = webTestClient.get().uri("/core-person/${aliasWithoutIdentifiers.id}/identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<Identifier>>()
          .returnResult().responseBody!!

        assertThat(result).isEmpty()
      }

      @Test
      fun `is able to re-hydrate the identifiers`() {
        val identifiers = webTestClient.get().uri("/core-person/${offender.id}/identifiers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyList(Identifier::class.java)
          .returnResult().responseBody!!

        assertThat(identifiers).hasSize(2)
        assertThat(identifiers.map { it.sequence }).containsExactly(1, 2)
        assertThat(identifiers[0].type).isEqualTo(CodeDescription(code = "PNC", description = "PNC Number"))
        assertThat(identifiers[1].type).isEqualTo(CodeDescription(code = "STAFF", description = "Staff Pass/ Identity Card"))
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/alias")
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
          booking(bookingSequence = 2) { release() }
          alias {
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
        webTestClient.get().uri("/core-person/${offenderWithoutBooking.id}/alias")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderWithoutBooking.id}/alias")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderWithoutBooking.id}/alias")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/999999/alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns the alias when there is no booking`() {
        val result = webTestClient.get().uri("/core-person/${offenderWithoutBooking.id}/alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CoreOffender>()
          .returnResult().responseBody!!

        assertThat(result.offenderId).isEqualTo(offenderWithoutBooking.id)
        assertThat(result.firstName).isEqualTo("JANE")
        assertThat(result.lastName).isEqualTo("NARK")
        assertThat(result.workingName).isTrue()
        assertThat(result.identifiers).isEmpty()
      }

      @Test
      fun `marks the alias as not working name (not active) when a different alias is current`() {
        val result = webTestClient.get().uri("/core-person/${offenderWithActiveAlias.id}/alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<CoreOffender>()
          .returnResult().responseBody!!

        assertThat(result.offenderId).isEqualTo(offenderWithActiveAlias.id)
        assertThat(result.firstName).isEqualTo("JOHN")
        assertThat(result.lastName).isEqualTo("BARK")
        assertThat(result.workingName).isFalse()
        assertThat(result.identifiers).isEmpty()
      }

      @Test
      fun `is able to re-hydrate the alias offender`() {
        val alias = webTestClient.get().uri("/core-person/${offenderWithoutBooking.id}/alias")
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

  @DisplayName("GET /core-person/{offenderId}/email/{emailAddressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderEmail {
    private lateinit var existingOffender: Offender
    private lateinit var existingEmail: OffenderInternetAddress

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmail = email(
            emailAddress = "test@justice.gov.uk",
          )
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/email/${existingEmail.internetAddressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.get().uri("/core-person/99999/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when email does not exist`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/email/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return an email`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("emailAddressId").isEqualTo(existingEmail.internetAddressId)
          .jsonPath("email").isEqualTo("test@justice.gov.uk")
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("createdByUsername").isNotEmpty
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/email")
  @Nested
  @TestInstance(PER_CLASS)
  inner class CreateOffenderEmail {
    private val validEmailRequest = CreateOffenderEmailRequest(
      email = "test@test.com",
    )

    private lateinit var existingOffender: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        )
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/email")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/email")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.post().uri("/core-person/A9999ZZ/email")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a email`() {
        val response: CreateOffenderEmailResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/email")
          .bodyValue(
            validEmailRequest.copy(
              email = "test@email.com",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderEmailResponse>()
          .returnResult()
          .responseBody!!

        val email = offenderInternetAddressRepository.findByIdOrNull(response.emailAddressId)!!

        with(email) {
          assertThat(internetAddressId).isEqualTo(response.emailAddressId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
          assertThat(email.internetAddress).isEqualTo("test@email.com")
          assertThat(email.internetAddressClass).isEqualTo("EMAIL")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.internetAddresses).anyMatch { it.internetAddressId == email.internetAddressId }
        }
      }
    }
  }

  @DisplayName("PUT /core-person/{prisonNumber}/email/{emailAddressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class UpdateOffenderEmail {
    private val validEmailRequest = UpdateOffenderEmailRequest(
      email = "test@test.com",
    )

    private lateinit var existingOffender: Offender
    private lateinit var existingEmail: OffenderInternetAddress

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmail = email(emailAddress = "test@justice.gov.uk")
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.put().uri("/core-person/A9999ZZ/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when email does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/email/9999")
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
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .bodyValue(
            validEmailRequest.copy(
              email = "test@email.com",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk

        val email = offenderInternetAddressRepository.findByIdOrNull(existingEmail.internetAddressId)!!

        with(email) {
          assertThat(internetAddressId).isEqualTo(existingEmail.internetAddressId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
          assertThat(email.internetAddress).isEqualTo("test@email.com")
          assertThat(email.internetAddressClass).isEqualTo("EMAIL")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.internetAddresses).anyMatch { it.internetAddressId == email.internetAddressId }
        }
      }
    }
  }

  @DisplayName("DELETE /core-person/{prisonNumber}/email/{emailAddressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class DeleteOffenderEmail {
    private lateinit var existingOffender: Offender
    private lateinit var existingEmail: OffenderInternetAddress
    private lateinit var differentEmail: OffenderInternetAddress

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmail = email(emailAddress = "test@justice.gov.uk")
        }
        offender(
          nomsId = "A1234BC",
          firstName = "FRED",
          lastName = "BOG",
        ) {
          differentEmail = email(emailAddress = "test2@justice.gov.uk")
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when emails exists but not on the offender`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/${differentEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when email does not exist`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a email`() {
        assertThat(offenderInternetAddressRepository.existsById(existingEmail.internetAddressId)).isTrue()
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(offenderInternetAddressRepository.existsById(existingEmail.internetAddressId)).isFalse()
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderPhone {
    private lateinit var existingOffender: Offender
    private lateinit var existingPhone: OffenderPhone

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.get().uri("/core-person/99999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/phone/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return a phone`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("phoneId").isEqualTo(existingPhone.phoneId)
          .jsonPath("number").isEqualTo("0113 4546 4646")
          .jsonPath("extension").isEqualTo("ext 567")
          .jsonPath("type.code").isEqualTo("HOME")
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("createdByUsername").isNotEmpty
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/phone")
  @Nested
  @TestInstance(PER_CLASS)
  inner class CreateOffenderPhone {
    private val validPhoneRequest = CreateOffenderPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingOffender: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        )
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.post().uri("/core-person/A9999ZZ/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/phone")
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
        val response: CreateOffenderPhoneResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/phone")
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
          .expectBody<CreateOffenderPhoneResponse>()
          .returnResult()
          .responseBody!!

        val phone = offenderPhoneRepository.findByIdOrNull(response.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(response.phoneId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("PUT /core-person/{prisonNumber}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class UpdateOffenderPhone {
    private val validPhoneRequest = UpdateOffenderPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingOffender: Offender
    private lateinit var existingPhone: OffenderPhone

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.put().uri("/core-person/A9999ZZ/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
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
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
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
          .isOk

        val phone = offenderPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(existingPhone.phoneId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("DELETE /core-person/{prisonNumber}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class DeleteOffenderPhone {
    private lateinit var existingOffender: Offender
    private lateinit var existingPhone: OffenderPhone
    private lateinit var differentPhone: OffenderPhone

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
        }
        offender(
          nomsId = "A1234BC",
          firstName = "JAMES",
          lastName = "BOG",
        ) {
          differentPhone = phone(phoneType = "BUS", phoneNo = "0113 4546 4647", extNo = "ext 567")
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when phone exists but not on that offender`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/${differentPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the phone`() {
        assertThat(offenderPhoneRepository.existsById(existingPhone.phoneId)).isTrue()
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(offenderPhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/address/{addressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderAddress {
    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address(
            primaryAddress = true,
            mailAddress = true,
          )
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.get().uri("/core-person/99999/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return an address`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addressId").isEqualTo(existingAddress.addressId)
          .jsonPath("premise").isEqualTo("41")
          .jsonPath("street").isEqualTo("High Street")
          .jsonPath("locality").isEqualTo("Sheffield")
          .jsonPath("primaryAddress").isEqualTo(true)
          .jsonPath("mailAddress").isEqualTo(true)
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("createdByUsername").isNotEmpty
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/address")
  @Nested
  @TestInstance(PER_CLASS)
  inner class CreateOffenderAddress {
    private val validAddressRequest = CreateOffenderAddressRequest(
      mailAddress = true,
      primaryAddress = true,
    )

    private lateinit var existingOffender: Offender

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        )
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.post().uri("/core-person/A9999ZZ/address")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when address usage code does not exist`() {
        val addressCountBefore = offenderAddressRepository.count()

        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(addressUsages = setOf("DAP", "ZZZZ")))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Address usage with code ZZZZ does not exist")

        assertThat(offenderAddressRepository.count()).isEqualTo(addressCountBefore)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a address`() {
        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(
            validAddressRequest.copy(
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              city = "Sheffield",
              county = "South Yorkshire",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        val address = offenderAddressRepository.findByIdOrNull(response.addressId)!!

        with(address) {
          assertThat(addressId).isEqualTo(response.addressId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
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
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.addresses).anyMatch { it.addressId == address.addressId }
        }

        verifyNoInteractions(telemetryClient)
      }

      @Test
      fun `will create address usages when supplied`() {
        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(
            validAddressRequest.copy(
              addressUsages = setOf("DAP", "CURFEW"),
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        nomisDataBuilder.runInTransaction {
          val address = offenderAddressRepository.findByIdOrNull(response.addressId)!!
          assertThat(address.usages).hasSize(2)
          with(address.usages.single { it.id.usageCode == "DAP" }) {
            assertThat(active).isTrue()
            assertThat(id.address.addressId).isEqualTo(response.addressId)
            assertThat(addressUsage?.description).isNotNull()
          }
          with(address.usages.single { it.id.usageCode == "CURFEW" }) {
            assertThat(active).isTrue()
            assertThat(id.address.addressId).isEqualTo(response.addressId)
          }
        }
      }

      @Test
      fun `will create address with no usages when none supplied`() {
        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(addressUsages = null))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        nomisDataBuilder.runInTransaction {
          val address = offenderAddressRepository.findByIdOrNull(response.addressId)!!
          assertThat(address.usages).isEmpty()
        }
      }

      @Test
      fun `will match city and county descriptions case insensitively`() {
        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(
            validAddressRequest.copy(
              city = "SHEFFIELD",
              county = "south yorkshire",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        with(offenderAddressRepository.findByIdOrNull(response.addressId)!!) {
          assertThat(city?.code).isEqualTo(SHEFFIELD)
          assertThat(county?.code).isEqualTo("S.YORKSHIRE")
        }

        verifyNoInteractions(telemetryClient)
      }

      @Test
      fun `will set city and county to null and record lookup failures in telemetry when descriptions do not match`() {
        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address")
          .bodyValue(
            validAddressRequest.copy(
              city = "Rubbish City",
              county = "Rubbish County",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        with(offenderAddressRepository.findByIdOrNull(response.addressId)!!) {
          assertThat(city).isNull()
          assertThat(county).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("offender-address-lookup-failed"),
          check {
            assertThat(it).containsEntry("prisonNumber", existingOffender.nomsId)
            assertThat(it).containsEntry("addressId", response.addressId.toString())
            assertThat(it).containsEntry("cityLookupFailure", "Rubbish City")
            assertThat(it).containsEntry("countyLookupFailure", "Rubbish County")
          },
          isNull(),
        )
      }

      @Test
      fun `will unset primary and mail flags on other addresses when new address is primary and mail`() {
        lateinit var offender: Offender
        lateinit var otherAddress: OffenderAddressJpa
        nomisDataBuilder.build {
          offender = offender(nomsId = "B1000AA") {
            otherAddress = address(primaryAddress = true, mailAddress = true)
          }
        }

        val response: CreateOffenderAddressResponse = webTestClient.post().uri("/core-person/${offender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(primaryAddress = true, mailAddress = true))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<CreateOffenderAddressResponse>()
          .returnResult()
          .responseBody!!

        with(offenderAddressRepository.findByIdOrNull(otherAddress.addressId)!!) {
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isFalse()
        }
        with(offenderAddressRepository.findByIdOrNull(response.addressId)!!) {
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
        }
      }

      @Test
      fun `will only unset the flags on other addresses that are set on the new address`() {
        lateinit var offender: Offender
        lateinit var otherAddress: OffenderAddressJpa
        nomisDataBuilder.build {
          offender = offender(nomsId = "B1001AA") {
            otherAddress = address(primaryAddress = true, mailAddress = true)
          }
        }

        webTestClient.post().uri("/core-person/${offender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(primaryAddress = true, mailAddress = false))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated

        with(offenderAddressRepository.findByIdOrNull(otherAddress.addressId)!!) {
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isTrue()
        }
      }

      @Test
      fun `will not change flags on other addresses when new address is neither primary nor mail`() {
        lateinit var offender: Offender
        lateinit var otherAddress: OffenderAddressJpa
        nomisDataBuilder.build {
          offender = offender(nomsId = "B1002AA") {
            otherAddress = address(primaryAddress = true, mailAddress = true)
          }
        }

        webTestClient.post().uri("/core-person/${offender.nomsId}/address")
          .bodyValue(validAddressRequest.copy(primaryAddress = false, mailAddress = false))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated

        with(offenderAddressRepository.findByIdOrNull(otherAddress.addressId)!!) {
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
        }
      }
    }
  }

  @DisplayName("PUT /core-person/{prisonNumber}/address/{addressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class UpdateOffenderAddress {
    private val validAddressRequest = UpdateOffenderAddressRequest(
      mailAddress = true,
      primaryAddress = true,
    )

    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address()
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.put().uri("/core-person/A9999ZZ/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/99999")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a address`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .bodyValue(
            validAddressRequest.copy(
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              city = "Sheffield",
              county = "South Yorkshire",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk

        val address = offenderAddressRepository.findByIdOrNull(existingAddress.addressId)!!

        with(address) {
          assertThat(addressId).isEqualTo(existingAddress.addressId)
          assertThat(offender.id).isEqualTo(existingOffender.id)
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
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          assertThat(offender?.addresses).anyMatch { it.addressId == address.addressId }
        }

        verifyNoInteractions(telemetryClient)
      }

      @Test
      fun `will set city and county to null and record lookup failures in telemetry when descriptions do not match`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .bodyValue(
            validAddressRequest.copy(
              city = "Rubbish City",
              county = "Rubbish County",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk

        with(offenderAddressRepository.findByIdOrNull(existingAddress.addressId)!!) {
          assertThat(city).isNull()
          assertThat(county).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("offender-address-lookup-failed"),
          check {
            assertThat(it).containsEntry("prisonNumber", existingOffender.nomsId)
            assertThat(it).containsEntry("addressId", existingAddress.addressId.toString())
            assertThat(it).containsEntry("cityLookupFailure", "Rubbish City")
            assertThat(it).containsEntry("countyLookupFailure", "Rubbish County")
          },
          isNull(),
        )
      }

      @Test
      fun `will unset primary and mail flags on other addresses but not on the updated address`() {
        lateinit var offender: Offender
        lateinit var otherAddress: OffenderAddressJpa
        lateinit var addressToUpdate: OffenderAddressJpa
        nomisDataBuilder.build {
          offender = offender(nomsId = "B1003AA") {
            otherAddress = address(primaryAddress = true, mailAddress = true)
            addressToUpdate = address(primaryAddress = true, mailAddress = true)
          }
        }

        webTestClient.put().uri("/core-person/${offender.nomsId}/address/${addressToUpdate.addressId}")
          .bodyValue(validAddressRequest.copy(primaryAddress = true, mailAddress = true))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk

        with(offenderAddressRepository.findByIdOrNull(otherAddress.addressId)!!) {
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isFalse()
        }
        with(offenderAddressRepository.findByIdOrNull(addressToUpdate.addressId)!!) {
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
        }
      }

      @Test
      fun `will only unset the flags on other addresses that are set on the updated address`() {
        lateinit var offender: Offender
        lateinit var otherAddress: OffenderAddressJpa
        lateinit var addressToUpdate: OffenderAddressJpa
        nomisDataBuilder.build {
          offender = offender(nomsId = "B1004AA") {
            otherAddress = address(primaryAddress = true, mailAddress = true)
            addressToUpdate = address()
          }
        }

        webTestClient.put().uri("/core-person/${offender.nomsId}/address/${addressToUpdate.addressId}")
          .bodyValue(validAddressRequest.copy(primaryAddress = false, mailAddress = true))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk

        with(offenderAddressRepository.findByIdOrNull(otherAddress.addressId)!!) {
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isFalse()
        }
      }
    }
  }

  @DisplayName("DELETE /core-person/{prisonNumber}/address/{addressId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class DeleteOffenderAddress {
    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa
    private lateinit var differentAddress: OffenderAddressJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address()
        }
        offender(
          nomsId = "A1234BC",
          firstName = "JAMES",
          lastName = "BOG",
        ) {
          differentAddress = address()
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when address exists but not on that offender`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${differentAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when address does not exist`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the address`() {
        assertThat(offenderAddressRepository.existsById(existingAddress.addressId)).isTrue()
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(offenderAddressRepository.existsById(existingAddress.addressId)).isFalse()
      }
    }
  }

  @DisplayName("GET /core-person/{offenderId}/address/{addressId}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetOffenderAddressPhone {
    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa
    private lateinit var existingPhone: OffenderAddressPhoneJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
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
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.get().uri("/core-person/99999/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/99999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}/phone/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return an address phone`() {
        webTestClient.get().uri("/core-person/${existingOffender.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("phoneId").isEqualTo(existingPhone.phoneId)
          .jsonPath("number").isEqualTo("0113 4546 4646")
          .jsonPath("extension").isEqualTo("ext 567")
          .jsonPath("type.code").isEqualTo("HOME")
          .jsonPath("createdDateTime").isNotEmpty
          .jsonPath("createdByUsername").isNotEmpty
      }
    }
  }

  @DisplayName("POST /core-person/{prisonNumber}/address/{addressId}/phone")
  @Nested
  @TestInstance(PER_CLASS)
  inner class CreateOffenderAddressPhone {
    private val validPhoneRequest = CreateOffenderPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingOffender: Offender
    private lateinit var anotherOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        anotherOffender = offender(
          nomsId = "A1234BC",
          firstName = "JANE",
          lastName = "BOG",
        )
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address()
        }
      }
    }

    @AfterAll
    fun tearDown(): Unit = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.post().uri("/core-person/A9999ZZ/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/999/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist on offender`() {
        webTestClient.post().uri("/core-person/${anotherOffender.nomsId}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone")
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
        val response: CreateOffenderPhoneResponse = webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone")
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
          .expectBody<CreateOffenderPhoneResponse>()
          .returnResult()
          .responseBody!!

        val phone = addressPhoneRepository.findByIdOrNull(response.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(response.phoneId)
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          val address = offender!!.addresses.find { it.addressId == existingAddress.addressId }
          assertThat(address?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("PUT /core-person/{prisonNumber}/address/{addressId}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class UpdateOffenderAddressPhone {
    private val validPhoneRequest = UpdateOffenderPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa
    private lateinit var existingPhone: OffenderAddressPhoneJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
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
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.put().uri("/core-person/A9999ZZ/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/99999/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
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
        webTestClient.put().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
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
          .isOk

        val phone = addressPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(existingPhone.phoneId)
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val offender = repository.offenderRepository.findByIdOrNull(existingOffender.id)
          val address = offender!!.addresses.find { it.addressId == existingAddress.addressId }
          assertThat(address?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("DELETE /core-person/{prisonNumber}/address/{addressId}/phone/{phoneId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class DeleteOffenderAddressPhone {
    private lateinit var existingOffender: Offender
    private lateinit var anotherOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa
    private lateinit var existingPhone: OffenderAddressPhoneJpa

    @BeforeAll
    fun setUp() {
      nomisDataBuilder.build {
        anotherOffender = offender(
          nomsId = "A1234BC",
          firstName = "JANE",
          lastName = "BOG",
        )
        existingOffender = offender(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
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
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when offender does not exist`() {
        webTestClient.delete().uri("/core-person/A9999ZZ/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone exists on address but address does not belong to offender`() {
        webTestClient.delete().uri("/core-person/${anotherOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when phone exists but not on address`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/99999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/99999")
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
        webTestClient.delete().uri("/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(addressPhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("Address usage endpoints")
  @Nested
  @TestInstance(PER_CLASS)
  inner class OffenderAddressUsage {
    private lateinit var existingOffender: Offender
    private lateinit var existingAddress: OffenderAddressJpa
    private val syncRole = "NOMIS_PRISONER_API__SYNCHRONISATION__RW"

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingOffender = offender(firstName = "JOHN", lastName = "BOG") {
          existingAddress = address {
            usage(usageCode = "CURFEW", active = false)
            usage(usageCode = "NOT_FOUND", active = false)
          }
        }
      }
    }

    @AfterEach
    fun tearDown(): Unit = deleteAll()

    @Test
    fun `access is forbidden without the required role`() {
      webTestClient.get().uri(getUsageUri("CURFEW"))
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access is unauthorised without an auth token`() {
      webTestClient.get().uri(getUsageUri("CURFEW"))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get returns 404 when usage does not exist`() {
      webTestClient.get().uri(getUsageUri("DAP"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get returns usage code when reference code does not exist`() {
      webTestClient.get().uri(getUsageUri("NOT_FOUND"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("addressId").isEqualTo(existingAddress.addressId)
        .jsonPath("usage").isEqualTo("NOT_FOUND")
        .jsonPath("active").isEqualTo(false)
    }

    @Test
    fun `get returns 404 when address does not belong to offender`() {
      webTestClient.get().uri("/core-person/99999/address/${existingAddress.addressId}/usage/CURFEW")
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get returns an address usage`() {
      webTestClient.get().uri(getUsageUri("CURFEW"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("addressId").isEqualTo(existingAddress.addressId)
        .jsonPath("usage").isEqualTo("CURFEW")
        .jsonPath("active").isEqualTo(false)
    }

    @Test
    fun `create returns 400 when usage code does not exist`() {
      webTestClient.post().uri(baseUsageUri())
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateOffenderAddressUsageRequest(usageCode = "UNKNOWN", active = true))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `create returns 400 when usage already exists`() {
      webTestClient.post().uri(baseUsageUri())
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateOffenderAddressUsageRequest(usageCode = "CURFEW", active = true))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `create returns 404 when address does not exist`() {
      webTestClient.post().uri("/core-person/${existingOffender.nomsId}/address/99999/usage")
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateOffenderAddressUsageRequest(usageCode = "DAP", active = true))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `create adds an address usage`() {
      webTestClient.post().uri(baseUsageUri())
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateOffenderAddressUsageRequest(usageCode = "DAP", active = true))
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("addressId").isEqualTo(existingAddress.addressId)
        .jsonPath("usage").isEqualTo("DAP")
        .jsonPath("active").isEqualTo(true)

      nomisDataBuilder.runInTransaction {
        val address = offenderAddressRepository.findByIdOrNull(existingAddress.addressId)!!
        assertThat(address.usages).anyMatch { it.id.usageCode == "DAP" && it.active }
      }
    }

    @Test
    fun `update changes whether an address usage is active`() {
      webTestClient.put().uri(usageUri("CURFEW"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(UpdateOffenderAddressUsageRequest(active = true))
        .exchange()
        .expectStatus().isOk

      nomisDataBuilder.runInTransaction {
        val address = offenderAddressRepository.findByIdOrNull(existingAddress.addressId)!!
        assertThat(address.usages.single { it.id.usageCode == "CURFEW" }.active).isTrue()
      }
    }

    @Test
    fun `update returns 404 when usage does not exist`() {
      webTestClient.put().uri(usageUri("DAP"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(UpdateOffenderAddressUsageRequest(active = true))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete removes an address usage`() {
      webTestClient.delete().uri(usageUri("CURFEW"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isNoContent

      nomisDataBuilder.runInTransaction {
        val address = offenderAddressRepository.findByIdOrNull(existingAddress.addressId)!!
        assertThat(address.usages).noneMatch { it.id.usageCode == "CURFEW" }
      }
    }

    @Test
    fun `delete returns 404 when usage does not exist`() {
      webTestClient.delete().uri(usageUri("DAP"))
        .headers(setAuthorisation(roles = listOf(syncRole)))
        .exchange()
        .expectStatus().isNotFound
    }

    private fun baseUsageUri() = "/core-person/${existingOffender.nomsId}/address/${existingAddress.addressId}/usage"

    private fun usageUri(usageCode: String) = "${baseUsageUri()}/$usageCode"

    private fun getUsageUri(usageCode: String) = "/core-person/${existingOffender.id}/address/${existingAddress.addressId}/usage/$usageCode"
  }
}
