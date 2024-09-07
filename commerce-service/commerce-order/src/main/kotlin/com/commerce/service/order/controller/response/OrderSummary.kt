package com.commerce.service.order.controller.response

import java.time.LocalDateTime

data class OrderSummary(
    val id: String,
    val orderNumber: String, // 주문 번호
    val content: String, // 주문 내역
    val orderDate: String, // 주문 일자
    val status: String, // 주문 상태
    val pricie: Double, // 가격
    val discoutedPrice: Double, // 할인된 가격
    val memberName: String, // 주문자 이름
    val recipient: String, // 수령인
)