package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.IN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.OUT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationMultiRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository
import java.time.LocalDate
import java.time.LocalDateTime

class TemporaryAbsenceRepositoryIntTest(
  @Autowired private val nomisDataBuilder: NomisDataBuilder,
  @Autowired private val movementApplicationRepository: OffenderMovementApplicationRepository,
  @Autowired private val movementApplicationMultiRepository: OffenderMovementApplicationMultiRepository,
  @Autowired private val scheduledTemporaryAbsenceRepository: OffenderScheduledTemporaryAbsenceRepository,
  @Autowired private val scheduledTemporaryAbsenceReturnRepository: OffenderScheduledTemporaryAbsenceReturnRepository,
  @Autowired private val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  @Autowired private val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
  @Autowired private val offenderBookingRepository: OffenderBookingRepository,
  @Autowired private val repository: Repository,
  @Autowired private val jdbcTemplate: JdbcTemplate,
) : IntegrationTestBase() {

  @Nested
  inner class TemporaryAbsenceRepositoryTest {
    lateinit var offender: Offender
    lateinit var booking: OffenderBooking
    lateinit var application: OffenderMovementApplication
    lateinit var scheduledAbsence: OffenderScheduledTemporaryAbsence
    lateinit var scheduledReturn: OffenderScheduledTemporaryAbsenceReturn
    lateinit var absenceMovement: OffenderTemporaryAbsence
    lateinit var absenceReturnMovement: OffenderTemporaryAbsenceReturn

    @AfterEach
    fun `reset data`() {
      repository.delete(offender)
    }

    @Test
    fun `should save and load movement application`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            application = temporaryAbsenceApplication(
              eventSubType = "C5",
              applicationDate = LocalDateTime.now(),
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
        assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
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
        offender = offender {
          booking = booking {
            application = temporaryAbsenceApplication(
              applicationDate = LocalDateTime.now(),
              applicationTime = LocalDateTime.now(),
            ) {
              scheduledAbsence = scheduledTemporaryAbsence(
                eventDate = LocalDate.now(),
                startTime = LocalDateTime.now(),
                eventSubType = "C5",
                eventStatus = "SCH",
                fromPrison = "LEI",
                toAgency = "HAZLWD",
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
        assertThat(fromAgency?.id).isEqualTo("LEI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(comment).isEqualTo("Some comment")
        assertThat(escort.code).isEqualTo("U")
        assertThat(transportType?.code).isEqualTo("VAN")
        assertThat(returnDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(applicationTime?.toLocalDate()).isEqualTo(LocalDate.now())
      }
    }

    @Test
    fun `should save and load linked TAP OUT and TAP IN`() {
      nomisDataBuilder.build {
        offender = offender {
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
                  fromAgency = "HAZLWD",
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
        assertThat(toAgency?.id).isEqualTo("LEI")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(comment).isEqualTo("Some comment IN")
        assertThat(escort.code).isEqualTo("U")
      }
    }

    @Test
    fun `should save and load movement application with multiple movements`() {
      lateinit var movement1: OffenderMovementApplicationMulti
      lateinit var movement2: OffenderMovementApplicationMulti

      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            application = temporaryAbsenceApplication {
              movement1 = outsideMovement(
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
              movement2 = outsideMovement(
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
        offender = offender {
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

    @Test
    fun `should save and load an application's outside movement addresses`() {
      lateinit var corporateAddress: CorporateAddress
      lateinit var offenderAddress: OffenderAddress
      lateinit var agencyAddress: AgencyAddress
      lateinit var movement1: OffenderMovementApplicationMulti
      lateinit var movement2: OffenderMovementApplicationMulti
      lateinit var movement3: OffenderMovementApplicationMulti

      nomisDataBuilder.build {
        corporate("Kwikfit") {
          corporateAddress = address()
        }
        agencyAddress = agencyAddress()
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              movement1 = outsideMovement(toAddress = corporateAddress)
              movement2 = outsideMovement(toAddress = offenderAddress)
              movement3 = outsideMovement(toAddress = agencyAddress)
            }
          }
        }
      }

      val movements = movementApplicationMultiRepository.findByOffenderMovementApplication(application)
      assertThat(movements).hasSize(3)

      with(movements.first { it.movementApplicationMultiId == movement1.movementApplicationMultiId }) {
        assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(movements.first { it.movementApplicationMultiId == movement2.movementApplicationMultiId }) {
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(movements.first { it.movementApplicationMultiId == movement3.movementApplicationMultiId }) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("AGY")
      }
    }

    @Test
    fun `should save and load TAP OUT addresses`() {
      lateinit var corporateAddress: CorporateAddress
      lateinit var offenderAddress: OffenderAddress
      lateinit var agencyAddress: AgencyAddress
      lateinit var scheduledAbsence2: OffenderScheduledTemporaryAbsence
      lateinit var scheduledAbsence3: OffenderScheduledTemporaryAbsence

      nomisDataBuilder.build {
        corporate("Kwikfit") {
          corporateAddress = address()
        }
        agencyAddress = agencyAddress()
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence(toAddress = corporateAddress)
            }
            application = temporaryAbsenceApplication {
              scheduledAbsence2 = scheduledTemporaryAbsence(toAddress = offenderAddress)
            }
            application = temporaryAbsenceApplication {
              scheduledAbsence3 = scheduledTemporaryAbsence(toAddress = agencyAddress)
            }
          }
        }
      }

      with(scheduledTemporaryAbsenceRepository.findByIdOrNull(scheduledAbsence.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(scheduledTemporaryAbsenceRepository.findByIdOrNull(scheduledAbsence2.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(scheduledTemporaryAbsenceRepository.findByIdOrNull(scheduledAbsence3.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("AGY")
      }
    }

    // This models some strange data in NOMIS where the to address owner class appears to be incorrectly set as the agency location id.
    // Just need to make sure we can read that data from the table
    @Test
    fun `should load a temporary absence application with where address class is the agency id`() {
      lateinit var agencyAddress: AgencyAddress

      nomisDataBuilder.build {
        agencyAddress = agencyAddress()
        offender = offender {
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence(toAddress = agencyAddress)
            }
          }
        }
      }

      jdbcTemplate.update("update offender_ind_schedules set TO_ADDRESS_OWNER_CLASS='${agencyAddress.agencyLocationId}'")

      with(scheduledTemporaryAbsenceRepository.findByIdOrNull(scheduledAbsence.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo(agencyAddress.agencyLocationId)
      }
    }

    @Test
    fun `should save and load external movement from scheduled temporary absence`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence {
                absenceMovement = externalMovement(
                  date = LocalDateTime.now(),
                  fromPrison = "BXI",
                  toAgency = "HAZLWD",
                  movementReason = "C5",
                  arrestAgency = "POL",
                  escort = "U",
                  escortText = "SE",
                  comment = "TAP OUT comment",
                  toCity = SHEFFIELD,
                  toAddress = offenderAddress,
                )
              }
            }
          }
        }
      }

      with(temporaryAbsenceRepository.findByIdOrNull(absenceMovement.id)!!) {
        assertThat(movementDate).isEqualTo(LocalDate.now())
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(OUT)
        assertThat(arrestAgency?.code).isEqualTo("POL")
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("BXI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(commentText).isEqualTo("TAP OUT comment")
        assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
      }
    }

    @Test
    fun `should save and load external movement without schedule`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            absenceMovement = temporaryAbsence(
              date = LocalDateTime.now(),
              fromPrison = "BXI",
              toAgency = "HAZLWD",
              movementReason = "C5",
              arrestAgency = "POL",
              escort = "U",
              escortText = "SE",
              comment = "Tap OUT comment",
              toCity = SHEFFIELD,
              toAddress = offenderAddress,
            )
          }
        }
      }

      with(temporaryAbsenceRepository.findByIdOrNull(absenceMovement.id)!!) {
        assertThat(movementDate).isEqualTo(LocalDate.now())
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(OUT)
        assertThat(arrestAgency?.code).isEqualTo("POL")
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("BXI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(commentText).isEqualTo("Tap OUT comment")
        assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
      }
    }

    @Test
    fun `should save and load external movement from scheduled temporary absence return`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking {
            temporaryAbsenceApplication {
              scheduledAbsence = scheduledTemporaryAbsence {
                absenceMovement = externalMovement()
                scheduledReturn = scheduledReturn {
                  absenceReturnMovement = externalMovement(
                    date = LocalDateTime.now().plusDays(1),
                    fromAgency = "HAZLWD",
                    toPrison = "BXI",
                    movementReason = "C5",
                    escort = "U",
                    escortText = "SE",
                    comment = "TAP IN comment",
                    fromCity = SHEFFIELD,
                    fromAddress = offenderAddress,
                  )
                }
              }
            }
          }
        }
      }

      jdbcTemplate.query("select MOVEMENT_TYPE,DIRECTION_CODE,event_id,parent_event_id from offender_external_movements") {
        println("done $it")
      }

      with(temporaryAbsenceReturnRepository.findByIdOrNull(absenceReturnMovement.id)!!) {
        assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(IN)
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(toAgency?.id).isEqualTo("BXI")
        assertThat(commentText).isEqualTo("TAP IN comment")
        assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(scheduledTemporaryAbsenceReturn?.eventId).isEqualTo(scheduledReturn.eventId)
        assertThat(scheduledTemporaryAbsenceReturn?.scheduledTemporaryAbsence?.eventId).isEqualTo(scheduledAbsence.eventId)
        assertThat(scheduledTemporaryAbsence?.eventId).isEqualTo(scheduledAbsence.eventId)
        assertThat(scheduledTemporaryAbsence?.temporaryAbsence?.id).isEqualTo(absenceMovement.id)
      }
    }

    @Test
    fun `should save and load external movement return without schedule`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            temporaryAbsence()
            absenceReturnMovement = temporaryAbsenceReturn(
              date = LocalDateTime.now().plusDays(1),
              fromAgency = "HAZLWD",
              toPrison = "BXI",
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "TAP IN comment",
              fromCity = SHEFFIELD,
              fromAddress = offenderAddress,
            )
          }
        }
      }

      with(temporaryAbsenceReturnRepository.findByIdOrNull(absenceReturnMovement.id)!!) {
        assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(IN)
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(toAgency?.id).isEqualTo("BXI")
        assertThat(commentText).isEqualTo("TAP IN comment")
        assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(scheduledTemporaryAbsenceReturn).isNull()
        assertThat(scheduledTemporaryAbsence?.eventId).isNull()
      }
    }

    @Test
    fun `should handle missing direction for TAP external movements (bad data exists in NOMIS)`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking()
        }
      }

      jdbcTemplate.update(
        """
        insert into OFFENDER_EXTERNAL_MOVEMENTS
        (OFFENDER_BOOK_ID, MOVEMENT_SEQ, MOVEMENT_DATE, MOVEMENT_TIME, MOVEMENT_TYPE, MOVEMENT_REASON_CODE, DIRECTION_CODE)
        values (${booking.bookingId}, 2, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', 'OUT'),
               (${booking.bookingId}, 3, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', 'IN'),
               (${booking.bookingId}, 4, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', null),
               (${booking.bookingId}, 5, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TRN', 'NOTR', 'OUT')
        """.trimIndent(),
      )

      repository.runInTransaction {
        with(offenderBookingRepository.findByIdOrNull(booking.bookingId)!!) {
          assertThat(externalMovements.find { it.id.sequence == 2L }).isExactlyInstanceOf(OffenderTemporaryAbsence::class.java)
          assertThat(externalMovements.find { it.id.sequence == 3L }).isExactlyInstanceOf(OffenderTemporaryAbsenceReturn::class.java)
          // The TAP missing a direction still comes out as an external movement
          assertThat(externalMovements.find { it.id.sequence == 4L }).isExactlyInstanceOf(OffenderExternalMovement::class.java)
          assertThat(externalMovements.find { it.id.sequence == 5L }).isExactlyInstanceOf(OffenderExternalMovement::class.java)
        }
      }
    }
  }
}
