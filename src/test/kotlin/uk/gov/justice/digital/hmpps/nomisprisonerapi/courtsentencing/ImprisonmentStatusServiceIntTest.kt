package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
class ImprisonmentStatusServiceIntTest {
  @Autowired
  private lateinit var imprisonmentStatusService: ImprisonmentStatusService

  @Autowired
  lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  lateinit var offenderRepository: OffenderRepository

  @Autowired
  lateinit var staffRepository: StaffRepository

  @Nested
  inner class ChargeHasNoOutcome {
    lateinit var prisoner: Offender
    lateinit var staff: Staff

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS")
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = null)
              courtEvent {
                courtEventCharge(
                  resultCode1 = null,
                  offenderCharge = offenderCharge,
                  plea = "NG",
                )
              }
            }
          }
        }
      }
    }

    @AfterEach
    fun afterEach() {
      if (::prisoner.isInitialized) {
        offenderRepository.deleteById(prisoner.id)
      }
      if (::staff.isInitialized) {
        staffRepository.deleteById(staff.id)
      }
    }

    @Test
    fun `current imprisonment status will be set to unknown`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now())
        }
        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Disposal Not Known")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
        }
      }
    }

    @Test
    open fun `imprisonment status will be set only once`() {
      repeat(10) {
        imprisonmentStatusService.recalculateImprisonmentStatus(
          offenderNo = prisoner.nomsId,
          ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
        )
      }

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
      }
    }
  }
}
