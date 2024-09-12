package com.commerce.common.persistence.orderProduct

import com.commerce.common.persistence.orders.OrdersJpaEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_product")
data class OrderProductJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: OrdersJpaEntity? = null,

    @Column(name = "product_id")
    val productId: Long,

    @Column(name = "quantity")
    val quantity: Long,

    @Column(name = "price")
    val price: BigDecimal,

    @Column(name = "discounted_price")
    val discountedPrice: BigDecimal,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)