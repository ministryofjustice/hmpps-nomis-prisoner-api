package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class OffenderNonAssociationDetailId(
  @Column(name = "OFFENDER_ID", nullable = false)
  val offenderId: Long = 0,

  @Column(name = "NS_OFFENDER_ID", nullable = false)
  val nsOffenderId: Long = 0,

  @Column(name = "TYPE_SEQ", nullable = false)
  val typeSequence: Int,
) : Serializable

@Entity
@Table(name = "OFFENDER_NA_DETAILS")
data class OffenderNonAssociationDetail(

  @EmbeddedId
  val id: OffenderNonAssociationDetailId,

  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBookingId: Long,

  @Column(name = "NS_OFFENDER_BOOK_ID", nullable = false)
  val nsOffenderBookingId: Long,

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'" + NonAssociationReason.DOMAIN + "'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "NS_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var nonAssociationReason: NonAssociationReason,

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'" + NonAssociationReason.DOMAIN + "'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "RECIP_NS_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var recipNonAssociationReason: NonAssociationReason? = null,

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'" + NonAssociationType.DOMAIN + "'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "NS_TYPE", referencedColumnName = "code")),
    ],
  )
  var nonAssociationType: NonAssociationType,

  @Column(name = "NS_EFFECTIVE_DATE", nullable = false)
  var effectiveDate: LocalDate,

  @Column(name = "NS_EXPIRY_DATE")
  var expiryDate: LocalDate? = null,

  @Column(name = "AUTHORIZED_STAFF")
  var authorisedBy: String? = null,

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  var createdBy: String = "",

  @Column(name = "MODIFY_USER_ID")
  var modifiedBy: String? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(column = JoinColumn(name = "OFFENDER_ID", referencedColumnName = "OFFENDER_ID", insertable = false, updatable = false)),
      JoinColumnOrFormula(column = JoinColumn(name = "NS_OFFENDER_ID", referencedColumnName = "NS_OFFENDER_ID", insertable = false, updatable = false)),
    ],
  )
  var nonAssociation: OffenderNonAssociation,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderNonAssociationDetail
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName +
    "((${id.offenderId},${id.nsOffenderId},${id.typeSequence}), offenderBooking=$offenderBookingId," +
    " nsOffenderBooking=$nsOffenderBookingId, nonAssociationReason=${nonAssociationReason.code}, " +
    "recipNonAssociationReason=$recipNonAssociationReason, nonAssociationType=${nonAssociationType.code}, " +
    "effectiveDate=$effectiveDate, expiryDate=$expiryDate, authorisedBy=$authorisedBy, modifiedBy=$modifiedBy, comment=$comment"
  // Omit offenderNonAssociation parent to avoid infinite recursion
}
