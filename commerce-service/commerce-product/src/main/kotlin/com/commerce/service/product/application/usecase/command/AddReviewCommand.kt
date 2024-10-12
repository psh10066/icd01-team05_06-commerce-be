package com.commerce.service.product.application.usecase.command

import java.math.BigDecimal

data class AddReviewCommand(
    val productId: Long,
    val email: String,
    val content: String,
    val score: BigDecimal,
)