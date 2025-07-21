package kr.hhplus.be.server.domain.log

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.infrastructure.adapter.out.persistence.BaseEntity

@Table(name = "point_history")
@Entity
class PointHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column("point_history_id")
    val pointHistoryId : Long,

    @Column("user_id")
    val userId : String,

    @Column("point_history_type")
    val pointHistoryType : String,

    @Column("point_history_amount")
    val pointHistoryAmount : Int,

    @Column("description")
    val description : String,
) : BaseEntity() {
}