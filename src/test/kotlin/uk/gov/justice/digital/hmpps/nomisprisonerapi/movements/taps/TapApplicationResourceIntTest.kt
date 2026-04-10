package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SWANSEA
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application.TapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application.UpsertTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application.UpsertTapApplicationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.UpsertTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.UpsertTapScheduleOutResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TapApplicationResourceIntTest(
  @Autowired val applicationRepository: OffenderTapApplicationRepository,
  @Autowired val scheduleOutRepository: OffenderTapScheduleOutRepository,
  @Autowired val scheduleInRepository: OffenderTapScheduleInRepository,
  @Autowired val corporateRepository: CorporateRepository,
  @Autowired val offenderAddressRepository: OffenderAddressRepository,
  @Autowired val agencyLocationRepository: AgencyLocationRepository,
  @Autowired val offenderRepository: OffenderRepository,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var corporateAddress: CorporateAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderTapApplication
  private lateinit var scheduleOut: OffenderTapScheduleOut
  private lateinit var agencyLocation: AgencyLocation

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val yesterday: LocalDateTime = today.minusDays(1)
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)
  private lateinit var orphanedSchedule: OffenderTapScheduleOut
  private lateinit var orphanedScheduleReturn: OffenderTapScheduleIn

  @AfterEach
  fun `tear down`() {
    // This must be removed before the offender booking due to a foreign key constraint (Hibernate is no longer managing this entity)
    if (this::orphanedSchedule.isInitialized) {
      scheduleOutRepository.delete(orphanedSchedule)
    }
    if (this::orphanedScheduleReturn.isInitialized) {
      scheduleInRepository.delete(orphanedScheduleReturn)
    }
    offenderRepository.deleteAll()
    if (this::agencyLocation.isInitialized) {
      agencyLocationRepository.delete(agencyLocation)
    }
    corporateRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/application/{applicationId}")
  inner class GetTapsApplication {
    private lateinit var inactiveBooking: OffenderBooking
    private lateinit var inactiveBookingApplication: OffenderTapApplication

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking(bookingSequence = 1) {
            application = tapApplication(
              eventSubType = "C5",
              applicationDate = twoDaysAgo,
              applicationTime = twoDaysAgo,
              fromDate = twoDaysAgo.toLocalDate(),
              releaseTime = twoDaysAgo,
              toDate = yesterday.toLocalDate(),
              returnTime = yesterday,
              applicationType = "SINGLE",
              applicationStatus = "APP-SCH",
              escort = "L",
              transportType = "VAN",
              comment = "Some comment application",
              prison = "LEI",
              toAgency = "HAZLWD",
              toAddress = offenderAddress,
              contactPersonName = "Derek",
              tapType = "RR",
              tapSubType = "RDR",
            ) {
              tapScheduleOut {
                tapMovementOut()
                tapScheduleIn {
                  tapMovementIn()
                }
              }
            }
            tapMovementOut()
            tapMovementIn()
          }
          inactiveBooking = booking(bookingSequence = 2) {
            receive(twoDaysAgo)
            inactiveBookingApplication = tapApplication()
            release(yesterday)
          }
        }
      }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/application/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/application/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/application/1")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/taps/application/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("UNKNOWN").contains("not found")
        }
    }

    @Test
    fun `should return not found if application not found`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/application/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("9999").contains("not found")
        }
    }

    @Test
    fun `should retrieve application`() {
      webTestClient.getApplicationOk()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(activeBooking).isEqualTo(true)
          assertThat(latestBooking).isEqualTo(true)
          assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(eventSubType).isEqualTo("C5")
          assertThat(applicationDate).isEqualTo(twoDaysAgo.toLocalDate())
          assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
          assertThat(releaseTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
          assertThat(toDate).isEqualTo(yesterday.toLocalDate())
          assertThat(returnTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
          assertThat(applicationType).isEqualTo("SINGLE")
          assertThat(applicationStatus).isEqualTo("APP-SCH")
          assertThat(escortCode).isEqualTo("L")
          assertThat(transportType).isEqualTo("VAN")
          assertThat(comment).isEqualTo("Some comment application")
          assertThat(prisonId).isEqualTo("LEI")
          assertThat(toAgencyId).isEqualTo("HAZLWD")
          assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
          assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
          assertThat(toAddressDescription).isNull()
          assertThat(toFullAddress).isEqualTo("41 High Street, Sheffield")
          assertThat(toAddressPostcode).isEqualTo("S1 1AA")
          assertThat(contactPersonName).isEqualTo("Derek")
          assertThat(tapType).isEqualTo("RR")
          assertThat(tapSubType).isEqualTo("RDR")
        }
    }

    @Test
    fun `should retrieve application on inactive booking`() {
      webTestClient.getApplicationOk(inactiveBookingApplication.tapApplicationId)
        .apply {
          assertThat(bookingId).isEqualTo(inactiveBooking.bookingId)
          assertThat(activeBooking).isEqualTo(false)
          assertThat(latestBooking).isEqualTo(false)
          assertThat(tapApplicationId).isEqualTo(inactiveBookingApplication.tapApplicationId)
        }
    }

    @Test
    fun `should retrieve latest booking even if inactive`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = "A9843AA") {
          booking = booking(bookingSequence = 1, active = false) {
            application = tapApplication()
          }
        }
      }

      webTestClient.getApplicationOk()
        .apply {
          assertThat(activeBooking).isEqualTo(false)
          assertThat(latestBooking).isEqualTo(true)
        }
    }
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/taps/application")
  inner class UpsertTapApplicationTest {

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class Create {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(toAddresses = listOf(UpsertTapAddress(addressText = "some street", postalCode = "S1 9ZZ"))),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(applicationDate.toLocalDate()).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(applicationType.code).isEqualTo("SINGLE")
                assertThat(applicationStatus.code).isEqualTo("APP-SCH")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(comment).isEqualTo("Some comment application")
                assertThat(prison.id).isEqualTo("LEI")
                assertThat(toAgency).isNull()
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.postalCode).isEqualTo("S1 9ZZ")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(tapType?.code).isEqualTo("RR")
                assertThat(tapSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should create all addresses requested`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTapAddress(addressText = "some street"),
              UpsertTapAddress(name = "Kwikfit", addressText = "another street", postalCode = "S1 9ZZ"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // the first address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
              // the first address was created
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                val address = find { it.premise == "some street" }!!
                assertThat(address.street).isNull()
                assertThat(address.postalCode).isNull()
              }
              // the second address was created
              with(corporateRepository.findAllByCorporateName("Kwikfit").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S1 9ZZ")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }

      @Test
      fun `should create addresses with long corporate name`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTapAddress(name = "Hm Prison Service The Chief Estates Surveyor", addressText = "another street", postalCode = "S1 9ZZ"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // the first address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("another street")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              // the address and corporation were created
              with(corporateRepository.findAllByCorporateName("Hm Prison Service The Chief Estates Surv").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S1 9ZZ")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }

      @Test
      fun `should not create corporate or address if already exists`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = "A9876CA") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTapAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // the address is used on the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
              // the corporate was not duplicated
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(size).isEqualTo(1)
                // The corporate address was not duplicated
                assertThat(first().addresses.size).isEqualTo(1)
              }
            }
          }
      }

      @Test
      fun `should not create corporate or address where there is already a duplicate corporate`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Different address", street = null, locality = null, postcode = "S4 4SS")
          }
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = "A9876CB") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTapAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // the address is used on the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              // the corporate was not duplicated
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(size).isEqualTo(2)
                // The corporate address was not duplicated
                assertThat(this[0].addresses.size).isEqualTo(1)
                assertThat(this[1].addresses.size).isEqualTo(1)
              }
            }
          }
      }

      @Test
      fun `should handle multiple new addresses on a corporate`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Some address", street = null, locality = null, postcode = "S1 1XX")
          }
          offender = offender(nomsId = "A9876CB") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTapAddress(name = "Boots", addressText = "New address 1, Sheffield", postalCode = "S2 2XX"),
              UpsertTapAddress(name = "Boots", addressText = "New address 2, Sheffield", postalCode = "S3 3XX"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // one of the new addresses is used on the application
                assertThat(toAddress?.postalCode).isIn("S2 2XX", "S3 3XX")
              }
              // the new addresses have been saved on the corporates
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(flatMap { it.addresses.map { it.postalCode } })
                  .containsExactlyInAnyOrder("S1 1XX", "S2 2XX", "S3 3XX")
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        // comment is 300 long
        webTestClient.upsertApplicationOk(anUpsertApplicationRequest().copy(comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }

      @Test
      fun `should allow create of a pending application without an address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            applicationStatus = "PEN",
            toAddresses = listOf(UpsertTapAddress()),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(toAddress).isNull()
                assertThat(toAddressOwnerClass).isNull()
              }
            }
          }
      }

      @Test
      fun `should create a new offender address where corporate name same as address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTapAddress(name = "Boston", addressText = "Boston")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(toAddress?.premise).isEqualTo("Boston")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should handle trailing blanks in corporate address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTapAddress(name = "HSL ", addressText = "Bowness , Cumbria ", postalCode = "LA23 3AS ")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(toAddress?.premise).isEqualTo("Bowness , Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              assertThat(corporateRepository.findAllByCorporateName("HSL").size).isEqualTo(1)
            }
          }
      }

      @Test
      fun `should allow corporate name in address text`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTapAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(toAddress?.premise).isEqualTo("HSL, Bowness, Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              assertThat(corporateRepository.findAllByCorporateName("HSL").size).isEqualTo(1)
            }
          }
      }

      @Nested
      inner class Validation {
        @Test
        fun `should return not found if offender unknown`() {
          webTestClient.upsertApplication(offenderNo = "UNKNOWN")
            .isNotFound
            .expectBody().jsonPath("userMessage").value<String> {
              assertThat(it).contains("UNKNOWN").contains("not found")
            }
        }

        @Test
        fun `should return not found if offender has no bookings`() {
          nomisDataBuilder.build {
            offender = offender(nomsId = "C1234DE") {
              offenderAddress = address()
            }
          }

          webTestClient.upsertApplication()
            .isNotFound
            .expectBody().jsonPath("userMessage").value<String> {
              assertThat(it).contains("C1234DE").contains("not found")
            }
        }

        @Test
        fun `should return bad request for invalid event sub type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(eventSubType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(applicationType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application status`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(applicationStatus = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid escort code`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(escortCode = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid transport type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(transportType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid prison ID`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(prisonId = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid tap type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(tapType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid tap sub type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(tapSubType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for an approved application without an address`() {
          webTestClient.upsertApplicationBadRequest(
            request = anUpsertApplicationRequest().copy(
              applicationStatus = "APP-UNSCH",
              toAddresses = listOf(),
            ),
          )
        }

        @Test
        fun `should return bad request for an approved application with a null address`() {
          webTestClient.upsertApplicationBadRequest(
            request = anUpsertApplicationRequest().copy(
              applicationStatus = "APP-UNSCH",
              toAddresses = listOf(UpsertTapAddress()),
            ),
          )
        }
      }

      @Nested
      inner class Security {
        @Test
        fun `should return unauthorised for missing token`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/taps/application")
            .bodyValue(anUpsertApplicationRequest())
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `should return forbidden for missing role`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/taps/application")
            .headers(setAuthorisation(roles = listOf()))
            .bodyValue(anUpsertApplicationRequest())
        }

        @Test
        fun `should return forbidden for wrong role`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/taps/application")
            .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        }
      }
    }

    @Nested
    inner class Update {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                eventSubType = "C4",
                applicationDate = twoDaysAgo,
                applicationTime = twoDaysAgo,
                fromDate = yesterday.toLocalDate(),
                releaseTime = yesterday,
                toDate = today.toLocalDate(),
                returnTime = today,
                applicationType = "REPEATING",
                applicationStatus = "PEN",
                escort = "U",
                transportType = "TAX",
                comment = "Old comment application",
                prison = "LEI",
                toAgency = null,
                toAddress = offenderAddress,
                contactPersonName = "Adam",
                tapType = "PP",
                tapSubType = "ROR",
              ) {
                tapScheduleOut(
                  toAddress = offenderAddress,
                  toAgency = "HAZLWD",
                ) {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapMovementOut()
              tapMovementIn()
            }
          }
        }
      }

      @Test
      fun `should update application`() {
        webTestClient.upsertApplicationOk(request = anUpsertApplicationRequest(id = application.tapApplicationId, toAddresses = listOf(UpsertTapAddress(id = offenderAddress.addressId))))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(applicationDate.toLocalDate()).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(applicationType.code).isEqualTo("SINGLE")
                assertThat(applicationStatus.code).isEqualTo("APP-SCH")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(comment).isEqualTo("Some comment application")
                assertThat(prison.id).isEqualTo("LEI")
                assertThat(toAgency?.id).isNull()
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(tapType?.code).isEqualTo("RR")
                assertThat(tapSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should return not found if unknown application id sent`() {
        webTestClient.upsertApplication(request = anUpsertApplicationRequest(id = 9999))
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should return 423 if application is locked for update`() {
        repository.runInTransaction {
          applicationRepository.findByIdOrNullForUpdate(application.tapApplicationId)

          webTestClient.upsertApplication(request = anUpsertApplicationRequest(id = application.tapApplicationId))
            .isEqualTo(423)
            .expectBody<ErrorResponse>().returnResult().responseBody!!
            .apply {
              assertThat(this.status).isEqualTo(423)
            }
        }
      }
    }

    @Nested
    inner class UpdateAddress {
      private lateinit var scheduleAddress: OffenderAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            scheduleAddress = address()
            booking = booking {
              application = tapApplication(
                toAddress = offenderAddress,
              ) {
                tapScheduleOut(
                  toAddress = scheduleAddress,
                  toAgency = "HAZLWD",
                ) {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapMovementOut()
              tapMovementIn()
            }
          }
        }
      }

      @Test
      fun `should update address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = application.tapApplicationId,
            toAddresses = listOf(
              UpsertTapAddress(id = offenderAddress.addressId),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should update address and create new addresses`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = application.tapApplicationId,
            toAddresses = listOf(
              UpsertTapAddress(addressText = "some street", postalCode = "S1 9ZZ"),
              UpsertTapAddress(name = "Kwikfit", addressText = "another street", postalCode = "S2 8YY"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // The first new address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.postalCode).isEqualTo("S1 9ZZ")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
              // the first address was created
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                val address = find { it.premise == "some street" }!!
                assertThat(address.street).isNull()
                assertThat(address.postalCode).isEqualTo("S1 9ZZ")
              }
              // the second address was created
              with(corporateRepository.findAllByCorporateName("Kwikfit").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S2 8YY")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateButNotAgencyAddressAndNoSchedules {

      private lateinit var agencyAddress: AgencyLocationAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation = agencyLocation(
            agencyLocationId = "NGENHO",
            description = "Northern General Hospital",
            type = "HOSPITAL",
          ) {
            agencyAddress = address(
              type = "BUS",
              street = "Herries Road",
              postcode = "S5 7AU",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                toAgency = "NGENHO",
                toAddress = agencyAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should not update address`() {
        webTestClient.upsertApplicationOk(request = anUpsertApplicationRequest(id = application.tapApplicationId, toAddresses = listOf(UpsertTapAddress(id = agencyAddress.addressId))))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("AGY")
                assertThat(toAgency?.id).isEqualTo("NGENHO")
              }
            }
          }
      }
    }

    @Nested
    inner class UpdatePendingApplicationNoAddress {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                applicationStatus = "APP-UNSCH",
                toAddress = offenderAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should allow update and remove address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
            toAddresses = listOf(UpsertTapAddress()),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(toAddress).isNull()
                assertThat(toAddressOwnerClass).isNull()
              }
            }
          }
      }
    }

    @Nested
    inner class IgnoreSimilarNomisCorporateAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(
            corporateName = "Swansea (Town Visit)",
          ) {
            corporateAddress = address(
              type = "BUS",
              flat = null,
              premise = "Swansea",
              street = null,
              locality = null,
              postcode = null,
              city = SWANSEA,
              county = null,
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application with new address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTapAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // We didn't pick up the existing similar NOMIS address
                assertThat(toAddress?.addressId).isNotEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("Swansea")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }

      @Test
      fun `should create schedule with same address as application`() {
        var applicationAddressId: Long = 0
        var applicationId: Long = 0

        // Create the application
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTapAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // save the application address ID
                applicationAddressId = toAddress!!.addressId
                applicationId = tapApplicationId
              }
            }
          }

        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            eventId = null,
            tapApplicationId = applicationId,
            toAddress = UpsertTapAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
          ),
        ).apply {
          repository.runInTransaction {
            with(scheduleOutRepository.findByIdOrNull(eventId)!!) {
              // check the schedule address is the same as the application address
              assertThat(toAddress?.addressId).isEqualTo(applicationAddressId)
              assertThat(toAddress?.premise).isEqualTo("Swansea")
              assertThat(toAddressOwnerClass).isEqualTo("CORP")
            }
          }
        }
      }
    }

    @Nested
    inner class IgnoreSimilarNomisOffenderAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              flat = null,
              premise = "Swansea",
              street = null,
              locality = null,
              postcode = null,
              city = SWANSEA,
              county = null,
              country = "ENG",
            )
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application with new address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTapAddress(addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // We didn't pick up the existing similar NOMIS address
                assertThat(toAddress?.addressId).isNotEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("Swansea")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should create schedule with same address as application`() {
        var applicationAddressId: Long = 0
        var applicationId: Long = 0

        // Create the application
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTapAddress(addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(tapApplicationId)!!) {
                // save the application address ID
                applicationAddressId = toAddress!!.addressId
                applicationId = tapApplicationId
              }
            }
          }

        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            eventId = null,
            tapApplicationId = applicationId,
            toAddress = UpsertTapAddress(addressText = "Swansea"),
          ),
        ).apply {
          repository.runInTransaction {
            with(scheduleOutRepository.findByIdOrNull(eventId)!!) {
              // check the schedule address is the same as the application address
              assertThat(toAddress?.addressId).isEqualTo(applicationAddressId)
              assertThat(toAddress?.premise).isEqualTo("Swansea")
              assertThat(toAddressOwnerClass).isEqualTo("OFF")
            }
          }
        }
      }
    }

    @Nested
    inner class UpdateToNotApproved {
      @Test
      fun `should remove schedule for single`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "SINGLE",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(tapScheduleOuts).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should not remove schedule for repeating`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "REPEATING",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationType = "REPEATING",
            applicationStatus = "PEN",
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(tapScheduleOuts.first()).isNotNull()
              }
            }
          }
      }

      @Test
      fun `should return bad request if schedule has a movement`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "SINGLE",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn()
                }
              }
            }
          }
        }

        webTestClient.upsertApplicationBadRequest(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
          ),
        )
      }
    }
  }

  @Nested
  @DisplayName("DELETE /movements/{offenderNo}/taps/application/{applicationId}")
  inner class DeleteApplication {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication()
          }
        }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `should delete application`() {
        webTestClient.deleteApplication()
          .expectStatus().isNoContent

        repository.runInTransaction {
          assertThat(applicationRepository.findByIdOrNull(application.tapApplicationId)).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 204 if unknown application id sent`() {
        webTestClient.deleteApplication(applicationId = 9999)
          .expectStatus().isNoContent
      }

      @Test
      fun `should return conflict for unknown offender`() {
        webTestClient.deleteApplication(offenderNo = "UNKNOWN")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 for wrong offender`() {
        nomisDataBuilder.build {
          offender(nomsId = "A7897WW")
        }

        webTestClient.deleteApplication(offenderNo = "A7897WW")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if application has scheduled movements`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                tapScheduleOut()
              }
            }
          }
        }

        webTestClient.deleteApplication()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/application/${application.tapApplicationId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/application/${application.tapApplicationId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/application/${application.tapApplicationId}")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getApplicationOk(applicationId: Long = application.tapApplicationId) = get()
    .uri("/movements/${offender.nomsId}/taps/application/$applicationId")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapApplication>()

  private fun WebTestClient.deleteApplication(offenderNo: String = offender.nomsId, applicationId: Long = application.tapApplicationId): WebTestClient.ResponseSpec = delete()
    .uri("/movements/$offenderNo/taps/application/$applicationId")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.upsertApplicationOk(request: UpsertTapApplication = anUpsertApplicationRequest()) = upsertApplication(request)
    .isOk
    .expectBodyResponse<UpsertTapApplicationResponse>()

  private fun WebTestClient.upsertApplicationBadRequest(request: UpsertTapApplication = anUpsertApplicationRequest()) = upsertApplication(request)
    .isBadRequest

  private fun WebTestClient.upsertApplicationBadRequestUnknown(request: UpsertTapApplication = anUpsertApplicationRequest()) = upsertApplicationBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.upsertApplication(
    request: UpsertTapApplication = anUpsertApplicationRequest(),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/taps/application")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun WebTestClient.upsertTapScheduleOutOk(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
  ) = upsertTapScheduleOut(request)
    .isOk
    .expectBodyResponse<UpsertTapScheduleOutResponse>()

  private fun WebTestClient.upsertTapScheduleOut(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/taps/schedule/out")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun anUpsertApplicationRequest(
    id: Long? = null,
    toAddresses: List<UpsertTapAddress> = listOf(UpsertTapAddress(addressText = "some street")),
  ) = UpsertTapApplication(
    tapApplicationId = id,
    eventSubType = "C5",
    applicationDate = twoDaysAgo.toLocalDate(),
    fromDate = twoDaysAgo.toLocalDate(),
    releaseTime = twoDaysAgo,
    toDate = yesterday.toLocalDate(),
    returnTime = yesterday,
    applicationType = "SINGLE",
    applicationStatus = "APP-SCH",
    escortCode = "L",
    transportType = "VAN",
    comment = "Some comment application",
    prisonId = "LEI",
    contactPersonName = "Derek",
    tapType = "RR",
    tapSubType = "RDR",
    toAddresses = toAddresses,
  )

  private fun anUpsertTapScheduleOut(
    tapApplicationId: Long? = null,
    eventId: Long? = null,
    returnEventStatus: String? = null,
    eventStatus: String = "SCH",
    toAddress: UpsertTapAddress = UpsertTapAddress(id = offenderAddress.addressId),
    comment: String = "Some comment tap schedule out",
  ) = UpsertTapScheduleOut(
    eventId = eventId,
    tapApplicationId = tapApplicationId ?: application.tapApplicationId,
    eventDate = twoDaysAgo.toLocalDate(),
    startTime = twoDaysAgo,
    eventSubType = "C5",
    eventStatus = eventStatus,
    comment = comment,
    escort = "L",
    fromPrison = "LEI",
    toAgency = "HAZLWD",
    transportType = "VAN",
    returnDate = yesterday.toLocalDate(),
    returnTime = yesterday,
    toAddress = toAddress,
    applicationDate = twoDaysAgo,
    applicationTime = twoDaysAgo,
    returnEventStatus = returnEventStatus,
  )
}
