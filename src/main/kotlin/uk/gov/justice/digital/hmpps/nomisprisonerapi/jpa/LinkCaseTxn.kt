package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import kotlin.jvm.javaClass

@Embeddable
data class LinkCaseTxnId(
  @Column(name = "CASE_ID", nullable = false)
  var caseId: Long,

  @Column(name = "COMBINED_CASE_ID", nullable = false)
  var combinedCaseId: Long,

  @Column(name = "OFFENDER_CHARGE_ID", nullable = false)
  var offenderChargeId: Long,
) : Serializable

@EntityOpen
@Entity
@Table(name = "LINK_CASE_TXNS")
data class LinkCaseTxn(
  @EmbeddedId
  val id: LinkCaseTxnId,

  @MapsId("caseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "CASE_ID", nullable = false)
  var sourceCase: CourtCase,

  @MapsId("combinedCaseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "COMBINED_CASE_ID", nullable = false)
  var targetCase: CourtCase,

  @MapsId("offenderChargeId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "OFFENDER_CHARGE_ID", nullable = false)
  var offenderCharge: OffenderCharge,

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
    value = [
      JoinColumn(name = "OFFENDER_CHARGE_ID", referencedColumnName = "OFFENDER_CHARGE_ID", nullable = true, insertable = false, updatable = false),
      JoinColumn(name = "EVENT_ID", referencedColumnName = "EVENT_ID", nullable = true, insertable = false, updatable = false),
    ],
  )
  var courtEventCharge: CourtEventCharge,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "EVENT_ID")
  var courtEvent: CourtEvent,
) : NomisAuditableEntityWithStaff() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as LinkCaseTxn

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
