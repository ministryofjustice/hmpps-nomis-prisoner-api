package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Entity
@Table(name = "IWP_DOCUMENTS")
@EntityOpen
class IWPDocument(
  @Id
  @Column(name = "DOCUMENT_ID")
  @SequenceGenerator(name = "DOCUMENT_ID", sequenceName = "DOCUMENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "DOCUMENT_ID")
  val id: Long = 0,

  @Column(name = "DOCUMENT_NAME")
  val fileName: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TEMPLATE_ID")
  val template: IWPTemplate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + DocumentStatus.DOCUMENT_STS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "DOCUMENT_STATUS", referencedColumnName = "code", nullable = true)),
    ],
  )
  val status: DocumentStatus,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @Column
  val objectId: String? = null,

  @Column
  val objectType: String? = null,

  @Lob
  @Column(name = "DOCUMENT_BODY", columnDefinition = "BLOB")
  val body: ByteArray? = null,

) {
  //  UNMAPPED FIELDS:
  //  LOCATION column has all null entries in preprod - so not mapped
  //  DATE_CREATED
  //  DATE_MODIFIED
  //  USER_CREATED
  //  USER_MODIFIED
  //  MODIFY_DATETIME
  //  MODIFY_USER_ID
  //  AUDIT_TIMESTAMP
  //  AUDIT_USER_ID
  //  AUDIT_MODULE_NAME
  //  AUDIT_CLIENT_USER_ID
  //  AUDIT_CLIENT_IP_ADDRESS
  //  AUDIT_CLIENT_WORKSTATION_NAME
  //  AUDIT_ADDITIONAL_INFO

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  open lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  open lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as IWPDocument
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
