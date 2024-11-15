package com.commerce.service.product.controller

import com.commerce.common.jwt.application.service.TokenType
import com.commerce.common.jwt.application.usecase.TokenUseCase
import com.commerce.common.jwt.config.JwtAuthenticationFilter
import com.commerce.common.model.address.Address
import com.commerce.common.model.member.Member
import com.commerce.common.model.member.MemberRepository
import com.commerce.common.model.review.ReviewRepository
import com.commerce.common.model.review.ReviewWithMember
import com.commerce.common.model.review.ReviewWithProduct
import com.commerce.common.util.ObjectMapperConfig
import com.commerce.service.product.config.SecurityConfig
import com.commerce.service.product.application.usecase.ReviewUseCase
import com.commerce.service.product.application.usecase.command.AddReviewCommand
import com.commerce.service.product.controller.request.ReviewCreateRequest
import com.commerce.service.product.controller.request.ReviewUpdateRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDateTime

@Import(ReviewController::class, ObjectMapperConfig::class, JwtAuthenticationFilter::class)
@ExtendWith(RestDocumentationExtension::class)
@ContextConfiguration(classes = [SecurityConfig::class])
@WebMvcTest(controllers = [ReviewController::class])
class ReviewControllerTest(
    @Autowired
    private val objectMapper: ObjectMapper
) {
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var tokenUseCase: TokenUseCase

    @MockBean
    private lateinit var reviewUseCase: ReviewUseCase

    @MockBean
    private lateinit var reviewRepository: ReviewRepository

    @MockBean
    private lateinit var memberRepository: MemberRepository

    private val testAccessToken =
        "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzI0NTIwNDc5LCJleHAiOjE3MjU3MzAwNzl9.i1WjcNXU2wBYjikGu5u0r41XmciafAfaMF3nNheb9cc7TUpai-tnMZCg3NUcTWP9"
    private val testMember = Member(
        id = 1,
        email = "commerce@example.com",
        password = "123!@#qwe",
        name = "홍길동",
        phone = "01012345678",
        address = Address(
            postalCode = "12345",
            streetAddress = "서울 종로구 테스트동",
            detailAddress = "123-45"
        )
    )

    @BeforeEach
    fun setUp(
        applicationContext: WebApplicationContext,
        restDocumentation: RestDocumentationContextProvider
    ) {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()

        // JwtAuthenticationFilter
        given(
            tokenUseCase.getTokenSubject(
                testAccessToken,
                TokenType.ACCESS_TOKEN
            )
        ).willReturn(testMember.id.toString())
        given(memberRepository.findById(testMember.id)).willReturn(testMember)
    }

    @Test
    fun getReviewsByProductId() {
        val productId = 1L
        given(reviewUseCase.getProductReviews(productId))
            .willReturn(
            listOf(
                ReviewWithMember(
                    id = 1,
                    content = "이 책 너무 재밌어요",
                    score = BigDecimal(5),
                    email = "abc@naver.com",
                    productId = 1L,
                    createdAt = LocalDateTime.now(),
                    lastModifiedByUserAt = LocalDateTime.now(),
                ),
                ReviewWithMember(
                    id = 2L,
                    content = "이 책 재밌긴 한데 아쉬워요",
                    score = BigDecimal(3),
                    email = "bbb@naver.com",
                    productId = 1L,
                    createdAt = LocalDateTime.now(),
                    lastModifiedByUserAt = LocalDateTime.now(),
                ),
                ReviewWithMember(
                    id = 3L,
                    content = "어려워요 읽다 말았어요",
                    score = BigDecimal(1),
                    email = "ddd@naver.com",
                    productId = 1L,
                    createdAt = LocalDateTime.now(),
                    lastModifiedByUserAt = LocalDateTime.now(),
                ),
            )
        )
        val queryParams = LinkedMultiValueMap<String, String>()
        queryParams["productId"] = "1"


            mockMvc.perform(get("/product/v1/reviews")
                .params(queryParams))
                .andExpect(status().isOk)
                .andDo(
                    document(
                        "reviews/v1/reviews",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("productId").description("상품 ID"),
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.reviews").type(JsonFieldType.ARRAY).description("리뷰 리스트"),
                            fieldWithPath("data.reviews[].reviewId").type(JsonFieldType.NUMBER).description("리뷰 아이디"),
                            fieldWithPath("data.reviews[].content").type(JsonFieldType.STRING).description("리뷰 내용"),
                            fieldWithPath("data.reviews[].score").type(JsonFieldType.NUMBER).description("작성자 상품 리뷰 평점"),
                            fieldWithPath("data.reviews[].email").type(JsonFieldType.STRING).description("리뷰 작성자 이메일"),
                            fieldWithPath("data.reviews[].productId").type(JsonFieldType.NUMBER).description("상품 ID"),
                            fieldWithPath("data.reviews[].createdAt").type(JsonFieldType.STRING).description("리뷰 작성일"),
                            fieldWithPath("data.reviews[].lastModifiedByUserAt").type(JsonFieldType.STRING).description("리뷰 최종 수정일"),
                            fieldWithPath("data.reviews[].orderProductId").type(JsonFieldType.NUMBER).optional().description("주문 - 상품 ID"),
                            fieldWithPath("error").type(JsonFieldType.ARRAY).optional().description("오류 정보")
                        ),
                    )
                )
    }

    @Test
    fun addReviewToProduct() {
        given(reviewUseCase.addReviewToProduct(
            testMember,
            AddReviewCommand(
                productId = 1L,
                content = "재미있어요!",
                score = BigDecimal(5),
            )
        )).willReturn(1L)

        val request = ReviewCreateRequest(
            productId = 1L,
            content = "재미있어요!",
            score = BigDecimal(5),
        )

        mockMvc.perform(
            post("/product/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "reviews/v1/add",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("productId").type(JsonFieldType.NUMBER).description("상품 ID"),
                        fieldWithPath("content").type(JsonFieldType.STRING).description("리뷰 내용"),
                        fieldWithPath("score").type(JsonFieldType.NUMBER).description("리뷰 평점"),
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("데이터"),
                        fieldWithPath("data.reviewId").type(JsonFieldType.NUMBER).description("생성된 리뷰 ID"),
                        fieldWithPath("error").type(JsonFieldType.ARRAY).optional().description("오류 정보"),
                    )
                )
            )
    }

    @Test
    fun updateReview() {
        val request = ReviewUpdateRequest(
            content = "재미있어요!",
            score = BigDecimal(5),
        )

        mockMvc.perform(
            patch("/product/v1/reviews/{reviewId}", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "reviews/v1/update",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("reviewId").description("리뷰 ID"),
                    ),
                    requestFields(
                        fieldWithPath("content").type(JsonFieldType.STRING).description("리뷰 내용"),
                        fieldWithPath("score").type(JsonFieldType.NUMBER).description("리뷰 평점"),
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("데이터"),
                        fieldWithPath("data.reviewId").type(JsonFieldType.NUMBER).description("리뷰 ID"),
                        fieldWithPath("error").type(JsonFieldType.ARRAY).optional().description("오류 정보"),
                    )
                )
            )
    }

    @Test
    fun deleteReview() {
        mockMvc.perform(
            delete("/product/v1/reviews/{reviewId}", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "reviews/v1/delete",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("reviewId").description("리뷰 ID"),
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("데이터"),
                        fieldWithPath("error").type(JsonFieldType.ARRAY).optional().description("오류 정보"),
                    )
                )
            )
    }

    @Test
    fun getReviewsByMember() {
        val memberId = 1L
        given(reviewUseCase.getMemberReviews(memberId))
            .willReturn(
                listOf(
                    ReviewWithProduct(
                        id = 1,
                        content = "이 책 너무 재밌어요",
                        score = BigDecimal(5),
                        productId = 1L,
                        productTitle = "상품1",
                        productAuthor = "작성자1",
                        productPublisher = "출판사1",
                        productCoverImage = "https://image1.com",
                        createdAt = LocalDateTime.now(),
                        lastModifiedByUserAt = LocalDateTime.now()
                    ),
                    ReviewWithProduct(
                        id = 2,
                        content = "이 책 재밌긴 한데 아쉬워요",
                        score = BigDecimal(5),
                        productId = 2L,
                        productTitle = "상품2",
                        productAuthor = "작성자2",
                        productPublisher = "출판사2",
                        productCoverImage = "https://image2.com",
                        createdAt = LocalDateTime.now(),
                        lastModifiedByUserAt = LocalDateTime.now()
                    ),
                )
            )

        mockMvc.perform(get("/product/v1/reviews/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "reviews/v1/reviews/me",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                        fieldWithPath("data.reviews").type(JsonFieldType.ARRAY).description("리뷰 리스트"),
                        fieldWithPath("data.reviews[].reviewId").type(JsonFieldType.NUMBER).description("리뷰 아이디"),
                        fieldWithPath("data.reviews[].content").type(JsonFieldType.STRING).description("리뷰 내용"),
                        fieldWithPath("data.reviews[].score").type(JsonFieldType.NUMBER).description("작성자 상품 리뷰 평점"),
                        fieldWithPath("data.reviews[].productId").type(JsonFieldType.NUMBER).description("상품 ID"),
                        fieldWithPath("data.reviews[].productTitle").type(JsonFieldType.STRING).description("상품 이름"),
                        fieldWithPath("data.reviews[].productAuthor").type(JsonFieldType.STRING).description("상품 저자"),
                        fieldWithPath("data.reviews[].productPublisher").type(JsonFieldType.STRING).description("상품 출판사"),
                        fieldWithPath("data.reviews[].productCoverImage").type(JsonFieldType.STRING).description("상품 이미지"),
                        fieldWithPath("data.reviews[].createdAt").type(JsonFieldType.STRING).description("리뷰 작성일"),
                        fieldWithPath("data.reviews[].lastModifiedByUserAt").type(JsonFieldType.STRING).description("리뷰 최종 수정일"),
                        fieldWithPath("error").type(JsonFieldType.ARRAY).optional().description("오류 정보")
                    ),
                )
            )
    }
}