package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

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
    fun `service does nothing yet`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(offenderNo = prisoner.nomsId, ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT)
    }
  }
}
