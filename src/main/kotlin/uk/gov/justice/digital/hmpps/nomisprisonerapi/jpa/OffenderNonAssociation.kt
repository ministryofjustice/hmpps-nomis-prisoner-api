package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable

@Embeddable
data class OffenderNonAssociationId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  val offender: Offender,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "NS_OFFENDER_ID", nullable = false, referencedColumnName = "OFFENDER_ID")
  val nsOffender: Offender,
) : Serializable

@Entity
@Table(name = "OFFENDER_NON_ASSOCIATIONS")
data class OffenderNonAssociation(

  @EmbeddedId
  val id: OffenderNonAssociationId,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "NS_OFFENDER_BOOK_ID", nullable = false)
  val nsOffenderBooking: OffenderBooking,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'" + NonAssociationReason.DOMAIN + "'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "NS_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var nonAssociationReason: NonAssociationReason? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'" + NonAssociationReason.DOMAIN + "'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "RECIP_NS_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var recipNonAssociationReason: NonAssociationReason? = null,

  @OneToMany(mappedBy = "nonAssociation", cascade = [CascadeType.ALL], orphanRemoval = true)
  val offenderNonAssociationDetails: MutableList<OffenderNonAssociationDetail> = mutableListOf(),

) {
  fun getOpenNonAssociationDetail(): OffenderNonAssociationDetail? =
    offenderNonAssociationDetails.firstOrNull { it.expiryDate == null }

  fun nextAvailableSequence(): Int = offenderNonAssociationDetails.maxByOrNull {
    it.id.typeSequence
  }?.id?.typeSequence?.plus(1) ?: 1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderNonAssociation
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String =
    this::class.simpleName +
      "(id = (${id.offender.nomsId},${id.nsOffender.nomsId}), offenderBooking=${offenderBooking.bookingId}, nsOffenderBooking=${nsOffenderBooking.bookingId}, nonAssociationReason=${nonAssociationReason?.code}, recipNonAssociationReason=${recipNonAssociationReason?.code}, offenderNonAssociationDetails=$offenderNonAssociationDetails)"
}

@Entity
@DiscriminatorValue(NonAssociationReason.DOMAIN)
class NonAssociationReason(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {
  companion object {
    const val DOMAIN = "NON_ASSO_RSN"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}

@Entity
@DiscriminatorValue(NonAssociationType.DOMAIN)
class NonAssociationType(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {
  companion object {
    const val DOMAIN = "NON_ASSO_TYP"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}
