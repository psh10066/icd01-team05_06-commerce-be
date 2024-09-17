package com.commerce.common.persistence.product

import com.commerce.common.model.product.Product
import com.commerce.common.model.product.ProductRepository
import com.commerce.common.persistence.category.CategoryJpaRepository
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl (
    private val productJpaRepository: ProductJpaRepository,
    private val categoryJpaRepository: CategoryJpaRepository,
    private val entityManager: EntityManager,
) : ProductRepository {

    override fun findByCategoryId(categoryId: Long, page: Int, size: Int): List<Product> {

        return productJpaRepository.findByCategoryId(categoryId, PageRequest.of(page, size))
            .map { product ->
                val category = product.categoryId?.let { categoryId ->
                    categoryJpaRepository.findById(categoryId)
                        .map{ it.toProductModel() }
                        .orElse(null)
                }
                product.toModel(category)
            }
            .toList()
    }

    override fun findByProductIdIn(ids: List<Long>): List<Product> {
        return productJpaRepository.findByIdIn(ids)
            .map { product ->
                val category = product.categoryId?.let { categoryId ->
                    categoryJpaRepository.findById(categoryId)
                        .map { it.toProductModel() }
                        .orElse(null)
                }
                product.toModel(category)
            }
            .toList()
    }

    override fun findById(productId: Long): Product {
        val product = productJpaRepository.findById(productId)
            .orElseThrow{ throw EntityNotFoundException("해당 제품이 존재하지 않습니다.") }

        val category = product.categoryId?.let { categoryId ->
            categoryJpaRepository.findById(categoryId)
                .map { it.toProductModel() }
                .orElse(null)
        }

        return product.toModel(category)
    }

    override fun findBySearchWord(keyword: String?, categoryId: Long?, page: Int, size: Int): List<Product> {
        return emptyList()
    }
}