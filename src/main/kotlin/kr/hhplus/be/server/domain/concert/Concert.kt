package kr.hhplus.be.server.domain.concert

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.BaseEntity


@Entity
@Table(name = "concerts")
class Concert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    val concertId: Long = 0,

    @Column(name = "concert_name", nullable = false, length = 100)
    val concertName: String,

    @Column(name = "location", nullable = false, length = 100)
    val location: String,

    @Column(name = "description", length = 500)
    val description: String? = null
) : BaseEntity() {
}
