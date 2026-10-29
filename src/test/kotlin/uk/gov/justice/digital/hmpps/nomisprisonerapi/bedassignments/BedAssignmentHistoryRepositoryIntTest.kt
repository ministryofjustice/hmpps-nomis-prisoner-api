package uk.gov.justice.digital.hmpps.nomisprisonerapi.bedassignments

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.BedAssignmentHistoryRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Temporary test to exercise the new BedAssignmentHistory JPA entity/repository using the new test data
 * builder. This will be superseded by resource-level tests in later PRs.
 */
class BedAssignmentHistoryRepositoryIntTest(
  @Autowired private val bedAssignmentHistoryRepository: BedAssignmentHistoryRepository,
  @Autowired private val offenderBookingRepository: OffenderBookingRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var booking: OffenderBooking
  private lateinit var firstCell: BedAssignmentHistory
  private lateinit var secondCell: BedAssignmentHistory

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Test
  @Transactional
  fun `should save and load bed assignment history for a booking`() {
    val firstAssigned = LocalDateTime.parse("2023-05-01T09:15:00")
    val moved = LocalDateTime.parse("2023-06-10T14:30:00")

    nomisDataBuilder.build {
      offender {
        booking = booking {
          firstCell = bedAssignmentHistory(
            sequence = 1,
            livingUnitId = -3009,
            assignmentDateTime = firstAssigned,
            assignmentReasonCode = "ADM",
            assignmentEndDateTime = moved,
          )
          secondCell = bedAssignmentHistory(
            sequence = 2,
            livingUnitId = -3010,
            assignmentDateTime = moved,
            assignmentReasonCode = "BEH",
          )
        }
      }
    }
    entityManager.flush()
    entityManager.clear()

    with(bedAssignmentHistoryRepository.findByIdOrNull(firstCell.id)!!) {
      assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
      assertThat(id.sequence).isEqualTo(1)
      assertThat(livingUnitId).isEqualTo(-3009)
      assertThat(livingUnit?.description).isEqualTo("BXI-A-1-016")
      assertThat(assignmentDate).isEqualTo(LocalDate.parse("2023-05-01"))
      assertThat(assignmentTime).isEqualTo(firstAssigned)
      assertThat(assignmentReasonCode).isEqualTo("ADM")
      assertThat(assignmentReason?.description).isEqualTo("Administrative")
      assertThat(assignmentEndDate).isEqualTo(LocalDate.parse("2023-06-10"))
      assertThat(assignmentEndTime).isEqualTo(moved)
      assertThat(createUsername).isNotBlank()
      assertThat(createDatetime).isNotNull()
    }

    with(bedAssignmentHistoryRepository.findByIdOrNull(secondCell.id)!!) {
      assertThat(livingUnit?.description).isEqualTo("BXI-A-1-017")
      assertThat(assignmentReason?.description).isEqualTo("Behaviour")
      assertThat(assignmentEndDate).isNull()
      assertThat(assignmentEndTime).isNull()
    }
  }

  @Test
  @Transactional
  fun `should find all history for a booking in sequence order`() {
    nomisDataBuilder.build {
      offender {
        booking = booking {
          bedAssignmentHistory(sequence = 2, livingUnitId = -3010)
          bedAssignmentHistory(sequence = 1, livingUnitId = -3009, assignmentEndDateTime = LocalDateTime.now())
        }
      }
    }
    entityManager.flush()
    entityManager.clear()

    val loadedBooking = offenderBookingRepository.findByIdOrNull(booking.bookingId)!!
    assertThat(bedAssignmentHistoryRepository.findAllByIdOffenderBookingOrderByIdSequence(loadedBooking).map { it.id.sequence })
      .containsExactly(1, 2)
    assertThat(loadedBooking.bedAssignmentHistories).hasSize(2)
  }

  @Test
  @Transactional
  fun `should keep the raw values when the living unit or reason are not in reference data`() {
    nomisDataBuilder.build {
      offender {
        booking = booking {
          firstCell = bedAssignmentHistory(livingUnitId = 999999, assignmentReasonCode = "UNKNOWN")
        }
      }
    }
    entityManager.flush()
    entityManager.clear()

    with(bedAssignmentHistoryRepository.findByIdOrNull(firstCell.id)!!) {
      assertThat(livingUnitId).isEqualTo(999999)
      assertThat(livingUnit).isNull()
      assertThat(assignmentReasonCode).isEqualTo("UNKNOWN")
      assertThat(assignmentReason).isNull()
    }
  }
}
