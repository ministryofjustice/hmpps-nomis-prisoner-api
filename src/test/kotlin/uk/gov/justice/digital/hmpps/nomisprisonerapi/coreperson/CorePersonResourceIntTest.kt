package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
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
          birthCountryCode = "ATA",
          ethnicityCode = "M3",
          genderCode = "F",
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          booking { }
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
      fun `will return full offender data`() {
        webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
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
          .jsonPath("offenders[0].workingName").isEqualTo(true)
      }
    }

    @Nested
    inner class Offenders {
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
            ) {}
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              issuedDate = LocalDate.parse("2020-01-01"),
              verified = true,
            )
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
    }

    @Nested
    inner class Identifiers {
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

      @Test
      fun `will return identifiers`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON"))).exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("identifiers.length()").isEqualTo(3)
          .jsonPath("identifiers[0].sequence").isEqualTo(1)
          .jsonPath("identifiers[0].offenderId").isEqualTo(offender.id)
          .jsonPath("identifiers[0].identifier").isEqualTo("20/0071818T")
          .jsonPath("identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("identifiers[0].issuedDate").isEqualTo("2020-01-01")
          .jsonPath("identifiers[0].verified").isEqualTo(true)
          .jsonPath("identifiers[1].sequence").isEqualTo(2)
          .jsonPath("identifiers[1].offenderId").isEqualTo(offender.id)
          .jsonPath("identifiers[1].identifier").isEqualTo("123")
          .jsonPath("identifiers[1].type.code").isEqualTo("STAFF")
          .jsonPath("identifiers[1].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("identifiers[1].issuedAuthority").doesNotExist()
          .jsonPath("identifiers[1].issuedDate").doesNotExist()
          .jsonPath("identifiers[1].verified").isEqualTo(false)
          .jsonPath("identifiers[2].sequence").isEqualTo(1)
          .jsonPath("identifiers[2].offenderId").isEqualTo(alias.id)
          .jsonPath("identifiers[2].identifier").isEqualTo("456")
          .jsonPath("identifiers[2].type.code").isEqualTo("STAFF")
          .jsonPath("identifiers[2].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("identifiers[2].issuedAuthority").doesNotExist()
          .jsonPath("identifiers[2].issuedDate").doesNotExist()
          .jsonPath("identifiers[2].verified").isEqualTo(false)
      }
    }

    @Nested
    inner class Addresses {
      private lateinit var offender: Offender

      @BeforeEach
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
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[0].addressId").isEqualTo(offender.addresses[0].addressId)
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
          .jsonPath("addresses[0].comment").doesNotExist()
          .jsonPath("addresses[0].startDate").doesNotExist()
          .jsonPath("addresses[0].endDate").doesNotExist()
          .jsonPath("addresses[0].phoneNumbers").doesNotExist()
          .jsonPath("addresses[1].addressId").isEqualTo(offender.addresses[1].addressId)
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
          .jsonPath("addresses[2].noFixedAddress").isEqualTo(true)
      }

      @Test
      fun `will return phone numbers associated with addresses`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[1].phoneNumbers[0].phoneId").isEqualTo(offender.addresses[1].phones[0].phoneId)
          .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
          .jsonPath("addresses[1].phoneNumbers[1].phoneId").isEqualTo(offender.addresses[1].phones[1].phoneId)
          .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class OffenderPhoneNumbers {
      private lateinit var offender: Offender

      @BeforeEach
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
        }
      }

      @Test
      fun `will return phone numbers`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("phoneNumbers[0].phoneId").isEqualTo(offender.phones[0].phoneId)
          .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("phoneNumbers[0].extension").doesNotExist()
          .jsonPath("phoneNumbers[1].phoneId").isEqualTo(offender.phones[1].phoneId)
          .jsonPath("phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class OffenderEmailOffenderAddress {
      private lateinit var offender: Offender

      @BeforeEach
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
        }
      }

      @Test
      fun `will return email address`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("emailAddresses[0].emailAddressId").isEqualTo(offender.internetAddresses[0].internetAddressId)
          .jsonPath("emailAddresses[0].email").isEqualTo("john.bog@justice.gov.uk")
          .jsonPath("emailAddresses[1].emailAddressId").isEqualTo(offender.internetAddresses[1].internetAddressId)
          .jsonPath("emailAddresses[1].email").isEqualTo("john.bog@gmail.com")
      }
    }

    @Nested
    inner class OffenderNationalities {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              profileDetail(profileType = "NAT", profileCode = "MG")
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              profileDetail(profileType = "NAT", profileCode = "BRIT")
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                profileDetail(profileType = "NAT", profileCode = null)
              }
            }
          }
        }
      }

      @Test
      fun `will return nationalities`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("nationalities[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("nationalities[0].nationality.code").isEqualTo("MG")
          .jsonPath("nationalities[0].nationality.description").isEqualTo("Malagasy")
          .jsonPath("nationalities[0].startDateTime").isEqualTo("2024-02-03T12:20:30")
          .jsonPath("nationalities[0].endDateTime").doesNotExist()
          .jsonPath("nationalities[0].latestBooking").isEqualTo(true)
          .jsonPath("nationalities[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("nationalities[1].nationality.code").isEqualTo("BRIT")
          .jsonPath("nationalities[1].nationality.description").isEqualTo("British")
          .jsonPath("nationalities[1].startDateTime").isEqualTo("2022-02-03T12:20:30")
          .jsonPath("nationalities[1].endDateTime").isEqualTo("2023-01-23T00:00:00")
          .jsonPath("nationalities[1].latestBooking").isEqualTo(false)
          .jsonPath("nationalities[2].bookingId").isEqualTo(aliasBooking.bookingId)
          .jsonPath("nationalities[2].nationality").doesNotExist()
          .jsonPath("nationalities[2].startDateTime").isEqualTo("2020-02-03T12:20:30")
          .jsonPath("nationalities[2].endDateTime").doesNotExist()
      }
    }

    @Nested
    inner class OffenderNationalityDetails {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              profileDetail(profileType = "NATIO", profileCode = "Claims to be from Madagascar")
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              profileDetail(profileType = "NATIO", profileCode = "ROTL 23/01/2023")
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                profileDetail(profileType = "NATIO", profileCode = null)
              }
            }
          }
        }
      }

      @Test
      fun `will return nationalities`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("nationalityDetails[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("nationalityDetails[0].details").isEqualTo("Claims to be from Madagascar")
          .jsonPath("nationalityDetails[0].startDateTime").isEqualTo("2024-02-03T12:20:30")
          .jsonPath("nationalityDetails[0].endDateTime").doesNotExist()
          .jsonPath("nationalityDetails[0].latestBooking").isEqualTo(true)
          .jsonPath("nationalityDetails[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("nationalityDetails[1].details").isEqualTo("ROTL 23/01/2023")
          .jsonPath("nationalityDetails[1].startDateTime").isEqualTo("2022-02-03T12:20:30")
          .jsonPath("nationalityDetails[1].endDateTime").isEqualTo("2023-01-23T00:00:00")
          .jsonPath("nationalityDetails[1].latestBooking").isEqualTo(false)
          .jsonPath("nationalityDetails[2].bookingId").isEqualTo(aliasBooking.bookingId)
          .jsonPath("nationalityDetails[2].details").doesNotExist()
          .jsonPath("nationalityDetails[2].startDateTime").isEqualTo("2020-02-03T12:20:30")
          .jsonPath("nationalityDetails[2].endDateTime").doesNotExist()
      }
    }

    @Nested
    inner class OffenderSexualOrientations {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              profileDetail(profileType = "SEXO", profileCode = "HET")
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              profileDetail(profileType = "SEXO", profileCode = "ND")
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                profileDetail(profileType = "SEXO", profileCode = null)
              }
            }
          }
        }
      }

      @Test
      fun `will return sexual orientations`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("sexualOrientations[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("sexualOrientations[0].sexualOrientation.code").isEqualTo("HET")
          .jsonPath("sexualOrientations[0].sexualOrientation.description").isEqualTo("Heterosexual / Straight")
          .jsonPath("sexualOrientations[0].startDateTime").isEqualTo("2024-02-03T12:20:30")
          .jsonPath("sexualOrientations[0].endDateTime").doesNotExist()
          .jsonPath("sexualOrientations[0].latestBooking").isEqualTo(true)
          .jsonPath("sexualOrientations[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("sexualOrientations[1].sexualOrientation.code").isEqualTo("ND")
          .jsonPath("sexualOrientations[1].sexualOrientation.description").isEqualTo("Not Disclosed")
          .jsonPath("sexualOrientations[1].startDateTime").isEqualTo("2022-02-03T12:20:30")
          .jsonPath("sexualOrientations[1].endDateTime").isEqualTo("2023-01-23T00:00:00")
          .jsonPath("sexualOrientations[1].latestBooking").isEqualTo(false)
          .jsonPath("sexualOrientations[2].bookingId").isEqualTo(aliasBooking.bookingId)
          .jsonPath("sexualOrientations[2].sexualOrientation").doesNotExist()
          .jsonPath("sexualOrientations[2].startDateTime").isEqualTo("2020-02-03T12:20:30")
          .jsonPath("sexualOrientations[2].endDateTime").doesNotExist()
      }
    }

    @Nested
    inner class OffenderDisabilities {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              profileDetail(profileType = "DISABILITY", profileCode = "Y")
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              profileDetail(profileType = "DISABILITY", profileCode = "N")
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                profileDetail(profileType = "DISABILITY", profileCode = null)
              }
            }
          }
        }
      }

      @Test
      fun `will return disabilities`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("disabilities[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("disabilities[0].disability").isEqualTo(true)
          .jsonPath("disabilities[0].startDateTime").isEqualTo("2024-02-03T12:20:30")
          .jsonPath("disabilities[0].endDateTime").doesNotExist()
          .jsonPath("disabilities[0].latestBooking").isEqualTo(true)
          .jsonPath("disabilities[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("disabilities[1].disability").isEqualTo(false)
          .jsonPath("disabilities[1].startDateTime").isEqualTo("2022-02-03T12:20:30")
          .jsonPath("disabilities[1].endDateTime").isEqualTo("2023-01-23T00:00:00")
          .jsonPath("disabilities[1].latestBooking").isEqualTo(false)
          .jsonPath("disabilities[2].bookingId").isEqualTo(aliasBooking.bookingId)
          .jsonPath("disabilities[2].disability").doesNotExist()
          .jsonPath("disabilities[2].startDateTime").isEqualTo("2020-02-03T12:20:30")
          .jsonPath("disabilities[2].endDateTime").doesNotExist()
      }
    }

    @Nested
    inner class OffenderInterestsToImmigration {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              profileDetail(profileType = "IMM", profileCode = "Y")
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              profileDetail(profileType = "IMM", profileCode = "N")
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                profileDetail(profileType = "IMM", profileCode = null)
              }
            }
          }
        }
      }

      @Test
      fun `will return interests to immigration`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("interestsToImmigration[0].bookingId").isEqualTo(booking1.bookingId)
          .jsonPath("interestsToImmigration[0].interestToImmigration").isEqualTo(true)
          .jsonPath("interestsToImmigration[0].startDateTime").isEqualTo("2024-02-03T12:20:30")
          .jsonPath("interestsToImmigration[0].endDateTime").doesNotExist()
          .jsonPath("interestsToImmigration[0].latestBooking").isEqualTo(true)
          .jsonPath("interestsToImmigration[1].bookingId").isEqualTo(booking2.bookingId)
          .jsonPath("interestsToImmigration[1].interestToImmigration").isEqualTo(false)
          .jsonPath("interestsToImmigration[1].startDateTime").isEqualTo("2022-02-03T12:20:30")
          .jsonPath("interestsToImmigration[1].endDateTime").isEqualTo("2023-01-23T00:00:00")
          .jsonPath("interestsToImmigration[1].latestBooking").isEqualTo(false)
          .jsonPath("interestsToImmigration[2].bookingId").isEqualTo(aliasBooking.bookingId)
          .jsonPath("interestsToImmigration[2].interestToImmigration").doesNotExist()
          .jsonPath("interestsToImmigration[2].startDateTime").isEqualTo("2020-02-03T12:20:30")
          .jsonPath("interestsToImmigration[2].endDateTime").doesNotExist()
      }
    }

    @Nested
    inner class OffenderSentenceStartDates {
      private lateinit var offender: Offender
      private lateinit var booking1: OffenderBooking
      private lateinit var booking2: OffenderBooking
      private lateinit var aliasBooking: OffenderBooking

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            booking1 = booking(bookingBeginDate = LocalDateTime.parse("2024-02-03T12:20:30")) {
              sentence(startDate = LocalDate.parse("2023-01-01"))
              sentence(startDate = LocalDate.parse("2020-01-01"))
            }
            booking2 = booking(
              active = false,
              bookingBeginDate = LocalDateTime.parse("2022-02-03T12:20:30"),
              bookingEndDate = LocalDate.parse("2023-01-23"),
            ) {
              sentence(startDate = LocalDate.parse("2020-01-01"))
            }
            alias {
              aliasBooking = booking(bookingBeginDate = LocalDateTime.parse("2020-02-03T12:20:30")) {
                sentence(startDate = LocalDate.parse("2020-01-02"))
              }
            }
          }
        }
      }

      @Test
      fun `will return sentence start dates`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("sentenceStartDates.length()").isEqualTo(3)
          .jsonPath("sentenceStartDates[0]").isEqualTo("2020-01-01")
          .jsonPath("sentenceStartDates[1]").isEqualTo("2020-01-02")
          .jsonPath("sentenceStartDates[2]").isEqualTo("2023-01-01")
      }
    }

    @Nested
    inner class OffenderBeliefs {
      private lateinit var offender: Offender
      private lateinit var belief1: OffenderBelief
      private lateinit var belief2: OffenderBelief
      private lateinit var belief3: OffenderBelief

      @BeforeEach
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
        }
      }

      @Test
      fun `will return beliefs`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
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
          .jsonPath("beliefs[0].verified").isEqualTo(false)
          .jsonPath("beliefs[1].beliefId").isEqualTo(belief2.beliefId)
          .jsonPath("beliefs[1].belief.code").isEqualTo("JAIN")
          .jsonPath("beliefs[1].belief.description").isEqualTo("Jain")
          .jsonPath("beliefs[1].startDate").isEqualTo("2021-01-01")
          .jsonPath("beliefs[1].endDate").doesNotExist()
          .jsonPath("beliefs[1].changeReason").isEqualTo(true)
          .jsonPath("beliefs[1].comments").isEqualTo("No longer believes in Zoroastrianism")
          .jsonPath("beliefs[1].verified").isEqualTo(true)
          .jsonPath("beliefs[1].audit.createUsername").isNotEmpty
          .jsonPath("beliefs[1].audit.createDatetime").isNotEmpty
          .jsonPath("beliefs[2].beliefId").isEqualTo(belief1.beliefId)
          .jsonPath("beliefs[2].belief.code").isEqualTo("ZORO")
          .jsonPath("beliefs[2].belief.description").isEqualTo("Zoroastrian")
          .jsonPath("beliefs[2].startDate").isEqualTo("2018-01-01")
          .jsonPath("beliefs[2].endDate").isEqualTo("2019-02-03")
          .jsonPath("beliefs[2].changeReason").doesNotExist()
          .jsonPath("beliefs[2].comments").doesNotExist()
          .jsonPath("beliefs[2].verified").isEqualTo(false)
          .jsonPath("beliefs[2].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("beliefs[2].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("beliefs[2].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }
  }
}
