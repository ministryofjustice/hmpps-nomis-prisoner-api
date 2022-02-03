package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(AgencyLocationType.AGY_LOC_TYPE)
class AgencyLocationType : ReferenceCode {
  constructor(code: String, description: String) : super(AGY_LOC_TYPE, code, description)

  val isCourt: Boolean
    get() = COURT_TYPE.code == code
  val isPrison: Boolean
    get() = PRISON_TYPE.code == code
  val isHospital: Boolean
    get() = HOSPITAL_TYPE.code == code || HS_HOSPITAL_TYPE.code == code

  companion object {
    const val AGY_LOC_TYPE = "AGY_LOC_TYPE"
    val INST = pk("INST")
    val CRT = pk("CRT")
    val HSHOSP = pk("HSHOSP")
    val HOSPITAL = pk("HOSPITAL")
    val COURT_TYPE = AgencyLocationType(CRT.code!!, "Court")
    val PRISON_TYPE = AgencyLocationType(INST.code!!, "Prison")
    val HS_HOSPITAL_TYPE = AgencyLocationType(HSHOSP.code!!, "Secure Hospital")
    val HOSPITAL_TYPE = AgencyLocationType(HOSPITAL.code!!, "Hospital")
    fun pk(code: String): Pk = Pk(AGY_LOC_TYPE, code)
  }
}
