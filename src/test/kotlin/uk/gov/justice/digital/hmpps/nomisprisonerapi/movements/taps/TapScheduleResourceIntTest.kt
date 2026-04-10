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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application.UpsertTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application.UpsertTapApplicationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.TapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.TapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.UpsertTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.UpsertTapScheduleOutResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TapScheduleResourceIntTest(
  @Autowired val applicationRepository: OffenderTapApplicationRepository,
  @Autowired val scheduleOutRepository: OffenderTapScheduleOutRepository,
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
  private lateinit var scheduleIn: OffenderTapScheduleIn
  private lateinit var movementOut: OffenderTapMovementOut
  private lateinit var movementIn: OffenderTapMovementIn
  private lateinit var agencyLocation: AgencyLocation

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val yesterday: LocalDateTime = today.minusDays(1)
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)

  @AfterEach
  fun `tear down`() {
    offenderRepository.deleteAll()
    if (this::agencyLocation.isInitialized) {
      agencyLocationRepository.delete(agencyLocation)
    }
    corporateRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/schedule/out/{eventId}")
  inner class GetTapScheduleOut {

    @Nested
    @DisplayName("With offender address")
    inner class WithOffenderAddress {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                tapType = "RR",
                tapSubType = "RDR",
              ) {
                scheduleOut = tapScheduleOut(
                  eventDate = twoDaysAgo.toLocalDate(),
                  startTime = twoDaysAgo,
                  eventSubType = "C5",
                  eventStatus = "COMP",
                  comment = "tap schedule out",
                  escort = "L",
                  fromPrison = "LEI",
                  toAgency = "HAZLWD",
                  transportType = "VAN",
                  returnDate = yesterday.toLocalDate(),
                  returnTime = yesterday,
                  toAddress = offenderAddress,
                  contactPersonName = "Jeff",
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn(
                    eventStatus = "SCH",
                  ) {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve tap schedule out`() {
        webTestClient.getTapScheduleOut()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(startTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(eventSubType).isEqualTo("C5")
            assertThat(eventStatus).isEqualTo("COMP")
            assertThat(inboundEventStatus).isEqualTo("SCH")
            assertThat(comment).isEqualTo("tap schedule out")
            assertThat(escort).isEqualTo("L")
            assertThat(fromPrison).isEqualTo("LEI")
            assertThat(toAgency).isEqualTo("HAZLWD")
            assertThat(transportType).isEqualTo("VAN")
            assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
            assertThat(returnTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
            assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(contactPersonName).isEqualTo("Jeff")
            assertThat(tapAbsenceType).isEqualTo("RR")
            assertThat(tapSubType).isEqualTo("RDR")
          }
      }
    }

    @Nested
    @DisplayName("With corporate address")
    inner class WithCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(
            corporateName = "Boots",
          ) {
            corporateAddress = address(
              type = "BUS",
              flat = null,
              premise = "Boots",
              street = "Scotland Street",
              locality = "Hunters Bar",
              postcode = "S1 3GG",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = corporateAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve corporate address`() {
        webTestClient.getTapScheduleOut()
          .apply {
            assertThat(toAddressId).isEqualTo(corporateAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("CORP")
            assertThat(toAddressDescription).isEqualTo("Boots")
            assertThat(toFullAddress).isEqualTo("Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
            assertThat(toAddressPostcode).isEqualTo("S1 3GG")
          }
      }
    }

    @Nested
    @DisplayName("With agency address")
    inner class WithAgencyAddress {
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
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = agencyAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve agency address`() {
        webTestClient.getTapScheduleOut()
          .apply {
            assertThat(toAddressId).isEqualTo(agencyAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("AGY")
            assertThat(toAddressDescription).isEqualTo("Northern General Hospital")
            assertThat(toFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
            assertThat(toAddressPostcode).isEqualTo("S5 7AU")
          }
      }
    }

    @Nested
    @DisplayName("With updated address")
    inner class WithUpdatedAddress {
      private val addressModifyDateTime = LocalDateTime.now().plusHours(1)
      private val addressModifyUser = "ADDRESS_MODIFY_USER"

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              whenModified = addressModifyDateTime,
              whoModified = addressModifyUser,
            )
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = offenderAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve audit details from updated address`() {
        webTestClient.getTapScheduleOut()
          .apply {
            assertThat(audit.modifyDatetime).isEqualTo(addressModifyDateTime)
            assertThat(audit.modifyUserId).isEqualTo(addressModifyUser)
          }
      }
    }

    @Nested
    inner class Validation {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/schedule/out/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/schedule/out/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/schedule/out/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/taps/schedule/out/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if tap schedule out not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/schedule/out/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/schedule/in/{eventId}")
  inner class GetTapScheduleIn {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn(
                  eventDate = yesterday.toLocalDate(),
                  startTime = yesterday,
                  eventSubType = "C5",
                  eventStatus = "SCH",
                  comment = "tap schedule in",
                  escort = "L",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                ) {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/schedule/in/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/schedule/in/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/schedule/in/1")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/taps/schedule/in/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("UNKNOWN").contains("not found")
        }
    }

    @Test
    fun `should return not found if tap schedule in not found`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps/schedule/in/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("9999").contains("not found")
        }
    }

    @Test
    fun `should retrieve tap schedule in`() {
      webTestClient.getTapScheduleIn()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(eventId).isEqualTo(scheduleIn.eventId)
          assertThat(parentEventId).isEqualTo(scheduleOut.eventId)
          assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
          assertThat(startTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
          assertThat(eventSubType).isEqualTo("C5")
          assertThat(eventStatus).isEqualTo("SCH")
          assertThat(comment).isEqualTo("tap schedule in")
          assertThat(escort).isEqualTo("L")
          assertThat(fromAgency).isEqualTo("HAZLWD")
          assertThat(toPrison).isEqualTo("LEI")
        }
    }
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/taps/schedule/out")
  inner class UpsertTapScheduleOutTest {

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class CreateScheduleWithOffenderAddress {
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

      @Test
      fun `should create tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment tap schedule out")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(tapScheduleIns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.upsertTapScheduleOutOk(
          // comment is 300 long
          anUpsertTapScheduleOut(application.tapApplicationId, comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithOffenderAddressCreatedInNomis {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "Permanent",
              street = "16 Main Road",
              locality = null,
              city = "28710",
              postcode = "BD10 9TT",
              county = null,
              country = "ENG",
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with existing address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            toAddress = UpsertTapAddress(
              name = null,
              addressText = "Permanent, 16 Main Road, Bradford, England",
              postalCode = "BD10 9TT",
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithOffenderAddressFromDpsWithTrailingBlanks {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "RDR, Preston Town Centre , Lancashire",
              street = null,
              locality = null,
              city = null,
              postcode = null,
              county = null,
              country = null,
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with existing address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            toAddress = UpsertTapAddress(
              name = null,
              addressText = "RDR, Preston Town Centre , Lancashire ",
              postalCode = null,
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithOffenderAddressThatOverflowIntoStreet {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              // Based on a production bug where last character of premise is a comma
              premise = "Unit 4, Nantcribbau Barns, Private Street From Junction With A490 By Nantcribba Cottages To B4388 By Dol-Y-Maen, Forden, Welshpool,",
              street = "Powys",
              locality = null,
              city = null,
              postcode = "SY21 8NW",
              county = null,
              country = null,
            )
            booking = booking {
              application = tapApplication(
                toAddress = offenderAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with existing address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            toAddress = UpsertTapAddress(
              name = null,
              addressText = "Unit 4, Nantcribbau Barns, Private Street From Junction With A490 By Nantcribba Cottages To B4388 By Dol-Y-Maen, Forden, Welshpool, Powys",
              postalCode = "SY21 8NW",
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithDuplicateOffenderAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            address(premise = "1 Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
            address(premise = "1 Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = null, addressText = "1 Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("1 Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
              }
              // Should not create another address
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                assertThat(filter { it.premise == "1 Scotland Street, Sheffield" }.size).isEqualTo(2)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWhereOffenderHasEmptyAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            // The existence of this address should not prevent the schedule being created
            address(
              flat = null,
              premise = null,
              street = null,
              locality = null,
              city = null,
              county = null,
              country = null,
              postcode = null,
            )
            offenderAddress = address()
            booking = booking {
              application = tapApplication(toAddress = offenderAddress)
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out despite existing empty address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            tapApplicationId = application.tapApplicationId,
            toAddress = UpsertTapAddress(addressText = "41 High Street, Sheffield"),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            corporateAddress = address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class ShouldFindOffenderAddressWhereCorporateNameSameAsAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "Boston",
              street = null,
              locality = null,
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with offender address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "Boston", addressText = "Boston", postalCode = null)),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("Boston")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }
    }

    @Nested
    inner class ShouldFindCorporateAddressWhereCorporateNameInAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create tap schedule out with corporate address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTapAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
          ),
        ).also {
          repository.runInTransaction {
            application = applicationRepository.findByIdOrNull(it.tapApplicationId)!!
            corporateAddress = corporateRepository.findAllByCorporateName("HSL").first().addresses.first()
          }
        }

        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("HSL, Bowness, Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithCorporateAddressCreatedInNomis {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Serving Thyme, HMP Ford") {
            corporateAddress = address(
              premise = "Serving Thyme, HMP Ford",
              street = "Ford Road",
              locality = null,
              city = "28389",
              county = "W.SUSSEX",
              country = "ENG",
              postcode = "BN18 0BX",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with existing address`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "Serving Thyme, HMP Ford", addressText = "Ford Road, Arundel, West Sussex, England", postalCode = "BN18 0BX")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithCorporateAddressTrailingBlanks {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "HSL") {
            corporateAddress = address(
              premise = "Bowness , Cumbria",
              street = null,
              locality = null,
              postcode = "LA23 3AS",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should find address despite trailing blanks`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(
            toAddress = UpsertTapAddress(name = "HSL ", addressText = "Bowness , Cumbria ", postalCode = "LA23 3AS "),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutWithDuplicatedCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            corporateAddress = address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(
          request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutAndIn {
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

      @Test
      fun `should create tap schedule and its tap schedule in`() {
        val request = anUpsertTapScheduleOut().copy(
          eventStatus = "COMP",
          returnEventStatus = "SCH",
        )

        webTestClient.upsertTapScheduleOutOk(request)
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleValidation {
      @Test
      fun `should return conflict if adding a schedule to an unapproved application`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationStatus = "PEN")
            }
          }
        }

        webTestClient.upsertTapScheduleOut()
          .isEqualTo(409)
      }

      @Test
      fun `should return conflict if adding a schedule to a single application that already has a schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationType = "SINGLE") {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertTapScheduleOut()
          .isEqualTo(409)
      }

      @Test
      fun `should allow adding a schedule to a repeating application`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationType = "REPEATING") {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertTapScheduleOutOk()
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(tapScheduleOuts.size).isEqualTo(2)
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateSchedule {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }
      }

      @Test
      fun `should update tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(anUpsertTapScheduleOut(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment tap schedule out")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(tapScheduleIns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertTapScheduleOutOk(
          // comment is 300 long
          anUpsertTapScheduleOut(
            tapApplicationId = application.tapApplicationId,
            eventId = scheduleOut.eventId,
            comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateTapScheduleOutAndCreateTapScheduleIn {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }
      }

      @Test
      fun `should update tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(anUpsertTapScheduleOut(eventId = scheduleOut.eventId, eventStatus = "COMP"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateTapScheduleOutAndIn {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut {
                  scheduleIn = tapScheduleIn()
                }
              }
            }
          }
        }
      }

      @Test
      fun `should update tap schedule out`() {
        webTestClient.upsertTapScheduleOutOk(anUpsertTapScheduleOut(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment tap schedule out")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
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

      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.upsertTapScheduleOut(offenderNo = "UNKNOWN")
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

        webTestClient.upsertTapScheduleOut()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if tap application does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking()
          }
        }

        webTestClient.upsertTapScheduleOut(request = anUpsertTapScheduleOut(tapApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid event status`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(eventStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid transport type`() {
        webTestClient.upsertTapScheduleOutBadRequestUnknown(anUpsertTapScheduleOut().copy(transportType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        val invalidAddress = UpsertTapAddress(id = 9999)
        webTestClient.upsertTapScheduleOutBadRequest(anUpsertTapScheduleOut().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }

      @Test
      fun `should fail if offender address does not exist`() {
        webTestClient.upsertTapScheduleOutBadRequest(request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should fail if corporate entity does not exist`() {
        webTestClient.upsertTapScheduleOutBadRequest(request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "unknown", addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should fail if corporate address does not exist`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
        }

        webTestClient.upsertTapScheduleOutBadRequest(request = anUpsertTapScheduleOut(toAddress = UpsertTapAddress(name = "Boots", addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should return bad request if address text not passed`() {
        val invalidAddress = UpsertTapAddress(addressText = null, name = "Business")
        webTestClient.upsertTapScheduleOutBadRequest(anUpsertTapScheduleOut().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).containsIgnoringCase("address text")
          }
      }
    }

    @Nested
    inner class Security {
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

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/taps/schedule/out")
          .bodyValue(anUpsertTapScheduleOut(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/taps/schedule/out")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(anUpsertTapScheduleOut(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/taps/schedule/out")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(anUpsertTapScheduleOut(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("DELETE /movements/{offenderNo}/taps/schedule/out/{eventId}")
  inner class DeleteTapScheduleOut {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                eventStatus = "SCH",
              )
            }
          }
        }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `should delete schedule`() {
        webTestClient.deleteTapScheduleOut()
          .expectStatus().isNoContent

        repository.runInTransaction {
          assertThat(scheduleOutRepository.findByIdOrNull(scheduleOut.eventId)).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 204 if unknown application id sent`() {
        webTestClient.deleteTapScheduleOut(eventId = 9999)
          .expectStatus().isNoContent
      }

      @Test
      fun `should return conflict for unknown offender`() {
        webTestClient.deleteTapScheduleOut(offenderNo = "UNKNOWN")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 for wrong offender`() {
        nomisDataBuilder.build {
          offender(nomsId = "A7897WW")
        }

        webTestClient.deleteTapScheduleOut(offenderNo = "A7897WW")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if scheduled has a movement`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "SCH",
                ) {
                  tapMovementOut()
                }
              }
            }
          }
        }

        webTestClient.deleteTapScheduleOut()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if status is completed`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "COMP",
                )
              }
            }
          }
        }

        webTestClient.deleteTapScheduleOut()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if there is an inbound schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "SCH",
                ) {
                  tapScheduleIn()
                }
              }
            }
          }
        }

        webTestClient.deleteTapScheduleOut()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/schedule/out/${scheduleOut.eventId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/schedule/out/${scheduleOut.eventId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/taps/schedule/out/${scheduleOut.eventId}")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getTapScheduleIn() = get()
    .uri("/movements/${offender.nomsId}/taps/schedule/in/${scheduleIn.eventId}")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapScheduleIn>()

  private fun WebTestClient.getTapScheduleOut() = get()
    .uri("/movements/${offender.nomsId}/taps/schedule/out/${scheduleOut.eventId}")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapScheduleOut>()

  private fun WebTestClient.deleteTapScheduleOut(offenderNo: String = offender.nomsId, eventId: Long = scheduleOut.eventId): WebTestClient.ResponseSpec = delete()
    .uri("/movements/$offenderNo/taps/schedule/out/$eventId")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.upsertTapScheduleOutOk(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
  ) = upsertTapScheduleOut(request)
    .isOk
    .expectBodyResponse<UpsertTapScheduleOutResponse>()

  private fun WebTestClient.upsertTapScheduleOutBadRequest(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
  ) = upsertTapScheduleOut(request)
    .isBadRequest

  private fun WebTestClient.upsertTapScheduleOutBadRequestUnknown(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
  ) = upsertTapScheduleOutBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.upsertApplicationOk(request: UpsertTapApplication = anUpsertApplicationRequest()) = upsertApplication(request)
    .isOk
    .expectBodyResponse<UpsertTapApplicationResponse>()

  private fun WebTestClient.upsertApplication(
    request: UpsertTapApplication = anUpsertApplicationRequest(),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/taps/application")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun WebTestClient.upsertTapScheduleOut(
    request: UpsertTapScheduleOut = anUpsertTapScheduleOut(application.tapApplicationId),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/taps/schedule/out")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

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
}
