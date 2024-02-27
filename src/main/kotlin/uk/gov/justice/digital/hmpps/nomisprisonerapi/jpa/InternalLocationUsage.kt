package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@Table(name = "INTERNAL_LOCATION_USAGES")
@EntityOpen
data class InternalLocationUsage(
  @Id
  val internalLocationUsageId: Long,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agency: AgencyLocation,

  // This should really be a reference to a ref domain value, but possible values include 'OTH' which is not in the ref code list.
  val internalLocationUsage: String,

  // EVENT_SUB_TYPE not used.
)
