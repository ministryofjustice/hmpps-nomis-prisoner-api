package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationMultiRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import java.time.LocalDate
import java.time.LocalDateTime

class TemporaryAbsenceIntTest(
  @Autowired private val nomisDataBuilder: NomisDataBuilder,
  @Autowired private val movementApplicationRepository: OffenderMovementApplicationRepository,
  @Autowired private val movementApplicationMultiRepository: OffenderMovementApplicationMultiRepository,
  @Autowired private val scheduledTemporaryAbsenceRepository: OffenderScheduledTemporaryAbsenceRepository,
  @Autowired private val scheduledTemporaryAbsenceReturnRepository: OffenderScheduledTemporaryAbsenceReturnRepository,
) : IntegrationTestBase() {
  // SDIT-2872 This is a temporary test class to prove that the database is modeled correctly - to be replaced by full integration tests later
  @Nested
  inner class TemporaryAbsenceRepositoryTest {
    lateinit var booking: OffenderBooking
    lateinit var application: OffenderMovementApplication
    lateinit var scheduledAbsence: OffenderScheduledTemporaryAbsence
    lateinit var scheduledReturn: OffenderScheduledTemporaryAbsenceReturn

    @Test
    fun `should save and load movement application`() {
      nomisDataBuilder.build {
        offender {
          booking = booking {
            application = temporaryAbsenceApplication(
              eventSubType = "C5",
              applicationDate = LocalDate.now(),
              applicationTime = LocalDateTime.now(),
              fromDate = LocalDate.now(),
              releaseTime = LocalDateTime.now(),
              toDate = LocalDate.now().plusDays(1),
              returnTime = LocalDateTime.now().plusDays(1),
              applicationType = "SINGLE",
              applicationStatus = "APP-SCH",
              escort = "L",
              transportType = "VAN",
              comment = "Some comment application",
              prison = "LEI",
              toAgency = "HAZLWD",
              contactPersonName = "Derek",
              temporaryAbsenceType = "RR",
              temporaryAbsenceSubType = "RDR",
            )
          }
        }
      }

      with(movementApplicationRepository.findByIdOrNull(application.movementApplicationId)!!) {
        assertThat(movementApplicationId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(eventClass).isEqualTo("EXT_MOV")
        assertThat(eventType).isEqualTo(EventType.TAP)
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(applicationDate).isEqualTo(LocalDate.now())
        assertThat(applicationTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(fromDate).isEqualTo(LocalDate.now())
        assertThat(releaseTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(toDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(applicationType.code).isEqualTo("SINGLE")
        assertThat(applicationStatus.code).isEqualTo("APP-SCH")
        assertThat(escort?.code).isEqualTo("L")
        assertThat(transportType?.code).isEqualTo("VAN")
        assertThat(comment).isEqualTo("Some comment application")
        assertThat(prison?.id).isEqualTo("LEI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(contactPersonName).isEqualTo("Derek")
        assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
        assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
      }
    }

    @Test
    fun `should save and load TAP OUT`() {
      nomisDataBuilder.build {
        offender {
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence(
                eventDate = LocalDate.now(),
                startTime = LocalDateTime.now(),
                eventSubType = "C5",
                eventStatus = "SCH",
                prison = "LEI",
                comment = "Some comment",
                escort = "U",
                transportType = "VAN",
                returnDate = LocalDate.now().plusDays(1),
                returnTime = LocalDateTime.now().plusDays(1),
              )
            }
          }
        }
      }

      with(scheduledTemporaryAbsenceRepository.findByIdOrNull(scheduledAbsence.eventId)!!) {
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(scheduledAbsence.temporaryAbsenceApplication.movementApplicationId)
        assertThat(eventDate).isEqualTo(LocalDate.now())
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(prison.id).isEqualTo("LEI")
        assertThat(comment).isEqualTo("Some comment")
        assertThat(escort.code).isEqualTo("U")
        assertThat(transportType.code).isEqualTo("VAN")
        assertThat(returnDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
      }
    }

    @Test
    fun `should save and load linked TAP OUT and TAP IN`() {
      nomisDataBuilder.build {
        offender {
          booking = booking {
            temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence {
                scheduledReturn = scheduledReturn(
                  eventDate = LocalDate.now().plusDays(1),
                  startTime = LocalDateTime.now().plusDays(1),
                  eventSubType = "R25",
                  eventStatus = "SCH",
                  comment = "Some comment IN",
                  escort = "U",
                  toPrison = "LEI",
                )
              }
            }
          }
        }
      }

      with(scheduledTemporaryAbsenceReturnRepository.findByIdOrNull(scheduledReturn.eventId)!!) {
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(scheduledTemporaryAbsence.eventId).isEqualTo(scheduledAbsence.eventId)
        assertThat(eventDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(eventSubType.code).isEqualTo("R25")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(toPrison.id).isEqualTo("LEI")
        assertThat(comment).isEqualTo("Some comment IN")
        assertThat(escort.code).isEqualTo("U")
      }
    }

    @Test
    fun `should save and load movement application with multiple movements`() {
      lateinit var movement1: OffenderMovementApplicationMulti
      lateinit var movement2: OffenderMovementApplicationMulti

      nomisDataBuilder.build {
        offender {
          booking = booking {
            application = temporaryAbsenceApplication {
              movement1 = movement(
                eventSubType = "C5",
                fromDate = LocalDate.now(),
                releaseTime = LocalDateTime.now(),
                toDate = LocalDate.now().plusDays(1),
                returnTime = LocalDateTime.now().plusDays(1),
                comment = "First movement",
                toAgency = "HAZLWD",
                contactPersonName = "Contact Person 1",
                temporaryAbsenceType = "RR",
                temporaryAbsenceSubType = "RDR",
              )
              movement2 = movement(
                eventSubType = "C6",
                fromDate = LocalDate.now().plusDays(2),
                releaseTime = LocalDateTime.now().plusDays(2),
                toDate = LocalDate.now().plusDays(3),
                returnTime = LocalDateTime.now().plusDays(3),
                comment = "Second movement",
                toAgency = "ARNOLD",
                contactPersonName = "Contact Person 2",
                temporaryAbsenceType = "SR",
                temporaryAbsenceSubType = "ROR",
              )
            }
          }
        }
      }

      val movements = movementApplicationMultiRepository.findByOffenderMovementApplication(application)
      assertThat(movements).hasSize(2)

      with(movements.first { it.movementApplicationMultiId == movement1.movementApplicationMultiId }) {
        assertThat(movementApplicationMultiId).isGreaterThan(0L)
        assertThat(offenderMovementApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(fromDate).isEqualTo(LocalDate.now())
        assertThat(releaseTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(toDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(comment).isEqualTo("First movement")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(contactPersonName).isEqualTo("Contact Person 1")
        assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
        assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
      }

      with(movements.first { it.movementApplicationMultiId == movement2.movementApplicationMultiId }) {
        assertThat(movementApplicationMultiId).isGreaterThan(0L)
        assertThat(offenderMovementApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
        assertThat(eventSubType.code).isEqualTo("C6")
        assertThat(fromDate).isEqualTo(LocalDate.now().plusDays(2))
        assertThat(releaseTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(2))
        assertThat(toDate).isEqualTo(LocalDate.now().plusDays(3))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(3))
        assertThat(comment).isEqualTo("Second movement")
        assertThat(toAgency?.id).isEqualTo("ARNOLD")
        assertThat(contactPersonName).isEqualTo("Contact Person 2")
        assertThat(temporaryAbsenceType?.code).isEqualTo("SR")
        assertThat(temporaryAbsenceSubType?.code).isEqualTo("ROR")
      }
    }

    @Test
    fun `Should save and load application address`() {
      lateinit var corporateAddress: CorporateAddress
      lateinit var offenderAddress: OffenderAddress
      lateinit var agencyAddress: AgencyAddress
      lateinit var application1: OffenderMovementApplication
      lateinit var application2: OffenderMovementApplication
      lateinit var application3: OffenderMovementApplication
      nomisDataBuilder.build {
        corporate("Kwikfit") {
          corporateAddress = address()
        }
        agencyAddress = agencyAddress()
        offender {
          offenderAddress = address()
          booking = booking {
            application1 = temporaryAbsenceApplication(toAddress = corporateAddress)
            application2 = temporaryAbsenceApplication(toAddress = offenderAddress)
            application3 = temporaryAbsenceApplication(toAddress = agencyAddress)
          }
        }
      }

      with(movementApplicationRepository.findByIdOrNull(application1.movementApplicationId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(movementApplicationRepository.findByIdOrNull(application2.movementApplicationId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(movementApplicationRepository.findByIdOrNull(application3.movementApplicationId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("AGY")
      }
    }
  }
}
