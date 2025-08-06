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
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class SentencePurposeId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID", nullable = false)
  var order: CourtOrder,
  // always 'CRT' on prod
  var orderPartyCode: String,
  var purposeCode: String,
) : Serializable

@Entity
@Table(name = "ORDER_PURPOSES")
@EntityOpen
class SentencePurpose(
  @EmbeddedId
  val id: SentencePurposeId,
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SentencePurpose
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
