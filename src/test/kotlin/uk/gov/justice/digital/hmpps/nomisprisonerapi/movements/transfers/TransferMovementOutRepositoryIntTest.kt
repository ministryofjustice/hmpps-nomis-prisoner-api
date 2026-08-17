package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferMovementOutRepository
import java.time.LocalDateTime

/**
 * Temporary test to exercise the new OffenderTransferMovementOut JPA entity/repository using the new test data
 * builder. This will be superseded by resource-level tests in later PRs.
 */
class TransferMovementOutRepositoryIntTest(
  @Autowired private val transferMovementOutRepository: OffenderTransferMovementOutRepository,
  @Autowired private val offenderExternalMovementRepository: OffenderExternalMovementRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var movementOut: OffenderTransferMovementOut

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Test
  fun `should save and load a transfer movement out`() {
    val date = LocalDateTime.parse("2023-05-01T09:15:00")

    nomisDataBuilder.build {
      offender = offender {
        booking = booking {
          movementOut = transferMovementOut(
            date = date,
            fromPrison = "BXI",
            toPrison = "LEI",
            movementReason = "28",
            escort = "U",
            comment = "Some comment",
            transferScheduleOutId = 12345,
          )
        }
      }
    }

    with(transferMovementOutRepository.findByIdOrNull(movementOut.id)!!) {
      assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
      assertThat(id.sequence).isEqualTo(movementOut.id.sequence)
      assertThat(movementDirection).isEqualTo(MovementDirection.OUT)
      assertThat(getMovementDateAndTime()).isEqualTo(date)
      assertThat(movementReason.id.reasonCode).isEqualTo("28")
      assertThat(escort?.code).isEqualTo("U")
      assertThat(fromAgency?.id).isEqualTo("BXI")
      assertThat(toAgency?.id).isEqualTo("LEI")
      assertThat(commentText).isEqualTo("Some comment")
      assertThat(transferScheduleOutId).isEqualTo(12345)
    }
  }

  @Test
  fun `should update a transfer movement out`() {
    nomisDataBuilder.build {
      offender = offender {
        booking = booking {
          movementOut = transferMovementOut(fromPrison = "BXI", toPrison = "LEI")
        }
      }
    }

    transferMovementOutRepository.findByIdOrNull(movementOut.id)!!.apply {
      commentText = "Updated comment"
    }.also { transferMovementOutRepository.saveAndFlush(it) }

    with(transferMovementOutRepository.findByIdOrNull(movementOut.id)!!) {
      assertThat(commentText).isEqualTo("Updated comment")
    }
  }

  @Test
  fun `should not load a corrupt transfer movement out with a null from prison as the specific transfer type`() {
    nomisDataBuilder.build {
      offender = offender {
        booking = booking {
          movementOut = transferMovementOut(fromPrison = "BXI", toPrison = "LEI")
        }
      }
    }

    repository.runInTransaction {
      /*
       * Corrupt the data with a null FROM_AGY_LOC_ID, as found for a small number of production records - these
       * should not be classified as OffenderTransferMovementOut and so should be excluded from this repository.
       */
      entityManager.createNativeQuery(
        """
          update OFFENDER_EXTERNAL_MOVEMENTS set FROM_AGY_LOC_ID = null
          where OFFENDER_BOOK_ID = ${movementOut.id.offenderBooking.bookingId}
          and MOVEMENT_SEQ = ${movementOut.id.sequence}
        """.trimIndent(),
      ).executeUpdate()
    }

    assertThat(transferMovementOutRepository.findByIdOrNull(movementOut.id)).isNull()
    assertThat(
      offenderExternalMovementRepository.findByIdOrNull(
        OffenderExternalMovementId(movementOut.id.offenderBooking, movementOut.id.sequence),
      ),
    ).isNotInstanceOf(OffenderTransferMovementOut::class.java)
  }
}
