package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(PayrollRegionType.PAYROLL_REG)
class PayrollRegionType(code: String, description: String) : ReferenceCode(PAYROLL_REG, code, description) {
  companion object {
    const val PAYROLL_REG = "PAYROLL_REG"
    fun pk(code: String): Pk = Pk(PAYROLL_REG, code)
  }
}
