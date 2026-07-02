package webirr

import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class WeBirrClientTests {
    private val exampleCursor = "20251231"
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun preferredConstructorSetsBillMerchantIdBeforeSending() {
        server.enqueue(apiErrorResponse())
        val api = testClient()
        val bill = sampleBill().also { it.merchantID = "merchant-on-bill" }

        runSuspend { api.createBill(bill) }

        val body = requestBody(server.takeRequest())
        assertEquals("merchant-from-client", body["merchantID"])
    }

    @Test
    fun emptyMerchantIdOverwritesExistingBillMerchantId() {
        server.enqueue(apiErrorResponse())
        val api = emptyMerchantTestClient()
        val bill = sampleBill().also { it.merchantID = "merchant-on-bill" }

        runSuspend { api.createBill(bill) }

        val body = requestBody(server.takeRequest())
        assertEquals("", body["merchantID"])
    }

    @Test
    fun injectedOkHttpClientIsUsedForRequests() {
        server.enqueue(successResponse())
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-Test-Client", "injected")
                        .build()
                )
            }
            .build()
        val api = WeBirrClient(
            "merchant-from-client",
            "api-key",
            WeBirrApiAdapter.createWeBirrApi(server.url("/").toString(), okHttpClient)
        )

        val response = runSuspend { api.deleteBill("123 456 789") }

        val request = server.takeRequest()
        assertEquals("OK", response.res)
        assertEquals("injected", request.getHeader("X-Test-Client"))
        assertEquals("merchant-from-client", request.requestUrl!!.queryParameter("merchant_id"))
    }

    @Test
    fun testEnvDefaultsToApiWebirrDev() {
        System.clearProperty("GATEWAY_URL")
        val captured = AtomicReference<HttpUrl>()
        val api = WeBirrClient(
            "merchant-from-client",
            "api-key",
            true,
            captureOnlyClient(captured)
        )

        val response = runSuspend { api.deleteBill("123 456 789") }

        assertEquals("OK", response.res)
        assertEquals("https", captured.get().scheme())
        assertEquals("api.webirr.dev", captured.get().host())
        assertEquals(443, captured.get().port())
    }

    @Test
    fun gatewayUrlOverridesTestEnvOnly() {
        System.setProperty("GATEWAY_URL", "http://127.0.0.1:9999/")
        try {
            val testCaptured = AtomicReference<HttpUrl>()
            val testApi = WeBirrClient(
                "merchant-from-client",
                "api-key",
                true,
                captureOnlyClient(testCaptured)
            )

            val testResponse = runSuspend { testApi.deleteBill("123 456 789") }

            assertEquals("OK", testResponse.res)
            assertEquals("http", testCaptured.get().scheme())
            assertEquals("127.0.0.1", testCaptured.get().host())
            assertEquals(9999, testCaptured.get().port())

            val prodCaptured = AtomicReference<HttpUrl>()
            val prodApi = WeBirrClient(
                "merchant-from-client",
                "api-key",
                false,
                captureOnlyClient(prodCaptured)
            )

            val prodResponse = runSuspend { prodApi.deleteBill("123 456 789") }

            assertEquals("OK", prodResponse.res)
            assertEquals("https", prodCaptured.get().scheme())
            assertEquals("api.webirr.net", prodCaptured.get().host())
            assertEquals(8080, prodCaptured.get().port())
        } finally {
            System.clearProperty("GATEWAY_URL")
        }
    }

    @Test
    fun endpointRequestsIncludeMerchantIdWhenConfigured() {
        for (endpoint in endpointCalls()) {
            server.enqueue(apiErrorResponse())
            val api = testClient()

            endpoint.invoke(api)

            val request = server.takeRequest()
            assertEquals(endpoint.method, request.method, endpoint.name)
            assertEquals(endpoint.path, request.requestUrl!!.encodedPath(), endpoint.name)
            assertEquals("api-key", request.requestUrl!!.queryParameter("api_key"), endpoint.name)
            assertEquals("merchant-from-client", request.requestUrl!!.queryParameter("merchant_id"), endpoint.name)
            endpoint.expectedQuery.forEach { (key, value) ->
                assertEquals(value, request.requestUrl!!.queryParameter(key), endpoint.name)
            }
        }
    }

    @Test
    fun endpointRequestsIncludeEmptyMerchantIdWhenClientMerchantIdIsEmpty() {
        for (endpoint in endpointCalls()) {
            server.enqueue(apiErrorResponse())
            val api = emptyMerchantTestClient()

            endpoint.invoke(api)

            val requestUrl = server.takeRequest().requestUrl!!
            assertEquals("", requestUrl.queryParameter("merchant_id"), endpoint.name)
            assertTrue(requestUrl.queryParameterNames().contains("merchant_id"), endpoint.name)
        }
    }

    @Test
    fun billDefaultsCustomerPhoneAndExtrasBeforeSending() {
        val bill = Bill(
            "cc01",
            "Elias Haileselassie",
            "kt/2021/130",
            "2021-07-22 22:14",
            "hotel booking",
            "270.90"
        )
        val json = Gson().fromJson(Gson().toJson(bill), Map::class.java)

        assertEquals("", json["customerPhone"])
        assertEquals(emptyMap<String, Any>(), json["extras"])
    }

    @Test
    fun billKeepsPopulatedExtrasAsAnObject() {
        val bill = sampleBill().also {
            it.extras = mapOf("invoiceNo" to "INV-001", "branch" to "main")
        }
        val json = Gson().fromJson(Gson().toJson(bill), Map::class.java)

        assertEquals(mapOf("invoiceNo" to "INV-001", "branch" to "main"), json["extras"])
    }

    @Test
    @Suppress("DEPRECATION")
    fun paymentDateIsPreferredWhileLegacyTimeAliasRemainsAvailable() {
        val detail = Gson().fromJson(
            """{"paymentDate":"2025-01-01 10:00:00","time":"2025-01-01 10:00:00"}""",
            PaymentDetail::class.java
        )

        assertEquals("2025-01-01 10:00:00", detail.paymentDate)
        assertEquals(detail.paymentDate, detail.time)
        detail.time = "2025-01-01 11:00:00"
        assertEquals("2025-01-01 11:00:00", detail.paymentDate)
    }

    @Test
    fun responseDTOsDeserializeBillPaymentBulkPaymentAndStats() {
        val gson = Gson()

        val bill = gson.fromJson(gson.toJson(billResponseJson()), BillResponse::class.java)
        assertEquals("123 456 789", bill.wbcCode)
        assertEquals(0, bill.paymentStatus)
        assertEquals("0911000000", bill.customerPhone)

        val payment = gson.fromJson(gson.toJson(paymentStatusJson()), Payment::class.java)
        assertTrue(payment.isPaid)
        assertEquals("2025-01-01 10:00:00", payment.data?.paymentDate)

        val bulkPayment = gson.fromJson(gson.toJson(paymentResponseJson()), PaymentResponse::class.java)
        assertTrue(bulkPayment.isReversed)
        assertEquals("20250101100100000001", bulkPayment.updateTimeStamp)

        val webhookPayload = gson.fromJson(gson.toJson(paymentWebhookPayloadJson()), PaymentWebhookPayload::class.java)
        assertEquals(2, webhookPayload.status)
        assertEquals(webhookPayload.status, webhookPayload.data.status)
        assertEquals("cbe_mobile", webhookPayload.data.bankID)
        assertEquals("FTC356A577695", webhookPayload.data.paymentReference)
        assertEquals("000 000 000", webhookPayload.data.wbcCode)
        assertEquals("2026062512000000000", webhookPayload.data.updateTimeStamp)
        assertEquals("2026-06-25 12:00:00", webhookPayload.data.paymentDate)

        val stat = gson.fromJson(
            """{"nBills":2,"nBillsPaid":1,"nBillsUnpaid":1,"amountBills":"548.00","amountPaid":"270.00","amountUnpaid":"278.00"}""",
            Stat::class.java
        )
        assertEquals(2.0, stat.nBills)
        assertEquals(548.0, stat.amountBills)

        val bank = gson.fromJson(
            """{"bankID":"cbe_mobile","name":"CBE Mobile Banking"}""",
            SupportedBank::class.java
        )
        assertEquals("cbe_mobile", bank.bankID)
        assertEquals("CBE Mobile Banking", bank.name)
    }

    @Test
    fun allEndpointsReturnApiErrorPayload() {
        for (endpoint in endpointCalls()) {
            server.enqueue(apiErrorResponse())
            val api = testClient()

            val response = endpoint.invoke(api)

            assertEquals("invalid api key", response.error, endpoint.name)
            assertEquals("ERROR_INVALID_API_KEY", response.errorCode, endpoint.name)
        }
    }

    @Test
    fun non2xxHttpThrowsPlatformExceptionWithStatus() {
        server.enqueue(MockResponse().setResponseCode(503).setStatus("HTTP/1.1 503 Service Unavailable"))
        val api = testClient()

        val error = assertThrows(Throwable::class.java) {
            runSuspend { api.deleteBill("123 456 789") }
        }
        assertTrue(error is WebirrPlatformException)
        error as WebirrPlatformException
        assertEquals(503, error.statusCode)
        assertEquals("Service Unavailable", error.status)
        assertTrue(error.isTransient())
        assertTrue(TransientErrors.isTransient(error))
    }

    @Test
    fun empty2xxBodyThrowsPlatformError() {
        server.enqueue(MockResponse().setResponseCode(200))
        val api = testClient()

        val error = assertThrows(Throwable::class.java) {
            runSuspend { api.deleteBill("123 456 789") }
        }
        assertNotNull(error)
    }

    @Test
    fun transportFailureThrows() {
        val api = testClient()
        server.shutdown()

        val error = assertThrows(Throwable::class.java) {
            runSuspend { api.deleteBill("123 456 789") }
        }
        assertNotNull(error)
        assertTrue(TransientErrors.isTransient(error))
    }

    @Test
    fun liveTestEnvSmokeAllEndpoints() {
        val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: ""
        val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: ""
        assumeTrue(
            merchantId.isNotEmpty() && apiKey.isNotEmpty(),
            "WEBIRR_TEST_ENV_MERCHANT_ID and WEBIRR_TEST_ENV_API_KEY are required"
        )

        val api = WeBirrClient(merchantId, apiKey, true)
        val billReference = "kt/test/${UUID.randomUUID()}"
        var paymentCode = ""
        var billDeleted = false

        try {
            val createResponse = runSuspend { api.createBill(liveSampleBill(billReference)) }
            assertNoApiError(createResponse, "createBill")
            paymentCode = createResponse.res ?: ""
            assertTrue(paymentCode.isNotEmpty())
            assertTrue(paymentCode.replace(" ", "").all { it.isDigit() })

            val updatedBill = liveSampleBill(billReference).also { it.amount = "278.00" }
            val updateResponse = runSuspend { api.updateBill(updatedBill) }
            assertNoApiError(updateResponse, "updateBill")
            assertEquals("ok", updateResponse.res?.lowercase())

            val statusResponse = runSuspend { api.getPaymentStatus(paymentCode) }
            assertNoApiError(statusResponse, "getPaymentStatus")
            assertEquals(0, statusResponse.res?.status)
            assertNull(statusResponse.res?.data)

            val byReference = runSuspend { api.getBillByReference(billReference) }
            assertNoApiError(byReference, "getBillByReference")
            assertCreatedBill(byReference.res, billReference, merchantId, paymentCode)
            assertEquals(278.0, byReference.res?.amount?.toDoubleOrNull() ?: 0.0, 0.01)
            val listCursor = cursorBefore(byReference.res?.updateTimeStamp ?: "", exampleCursor)

            val byPaymentCode = runSuspend { api.getBillByPaymentCode(paymentCode) }
            assertNoApiError(byPaymentCode, "getBillByPaymentCode")
            assertCreatedBill(byPaymentCode.res, billReference, merchantId, paymentCode)

            val bills = runSuspend { api.getBills(paymentStatus = 0, lastTimeStamp = listCursor, limit = 100) }
            assertNoApiError(bills, "getBills")
            val foundBill = bills.res?.firstOrNull {
                it.billReference.equals(billReference, ignoreCase = true)
            }
            assertCreatedBill(foundBill, billReference, merchantId, paymentCode)

            val payments = runSuspend { api.getPayments(lastTimeStamp = exampleCursor, limit = 10) }
            assertNoApiError(payments, "getPayments")
            assertNotNull(payments.res)

            val stat = runSuspend { api.getStat("2025-01-01", "2030-01-31") }
            assertNoApiError(stat, "getStat")
            assertNotNull(stat.res)

            val supportedBanks = runSuspend { api.getSupportedBanks() }
            assertNoApiError(supportedBanks, "getSupportedBanks")
            assertFalse(supportedBanks.res.isNullOrEmpty())
            supportedBanks.res?.forEach {
                assertFalse(it.bankID.isEmpty())
                assertFalse(it.name.isEmpty())
            }

            val deleteResponse = runSuspend { api.deleteBill(paymentCode) }
            assertNoApiError(deleteResponse, "deleteBill")
            assertEquals("ok", deleteResponse.res?.lowercase())
            billDeleted = true

            val deletedLookup = runSuspend { api.getBillByReference(billReference) }
            assertNotNull(deletedLookup.error)
        } finally {
            if (paymentCode.isNotEmpty() && !billDeleted) {
                runSuspend { api.deleteBill(paymentCode) }
            }
        }
    }

    private fun testClient(): WeBirrClient =
        WeBirrClient(
            "merchant-from-client",
            "api-key",
            WeBirrApiAdapter.createWeBirrApi(server.url("/").toString())
        )

    private fun emptyMerchantTestClient(): WeBirrClient =
        WeBirrClient(
            "",
            "api-key",
            WeBirrApiAdapter.createWeBirrApi(server.url("/").toString())
        )

    private fun captureOnlyClient(captured: AtomicReference<HttpUrl>): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                captured.set(request.url())
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        ResponseBody.create(
                            MediaType.parse("application/json"),
                            """{"error":null,"errorCode":null,"res":"OK"}"""
                        )
                    )
                    .build()
            }
            .build()

    private fun <T> runSuspend(operation: suspend () -> T): T {
        val latch = CountDownLatch(1)
        val outcome = AtomicReference<Result<T>>()

        operation.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome.set(result)
                latch.countDown()
            }
        })

        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw WebirrPlatformException("test timeout")
        }
        return outcome.get().getOrThrow()
    }

    private fun assertThrows(type: Class<out Throwable>, operation: () -> Unit): Throwable {
        try {
            operation()
        } catch (throwable: Throwable) {
            assertTrue(type.isInstance(throwable))
            return throwable
        }
        throw AssertionError("expected ${type.simpleName}")
    }

    private fun endpointCalls(): List<EndpointCall> =
        listOf(
            EndpointCall("createBill", "POST", "/einvoice/api/bill") { api ->
                runSuspend { api.createBill(sampleBill()) }
            },
            EndpointCall("updateBill", "PUT", "/einvoice/api/bill") { api ->
                runSuspend { api.updateBill(sampleBill()) }
            },
            EndpointCall(
                "deleteBill",
                "DELETE",
                "/einvoice/api/bill",
                mapOf("wbc_code" to "123 456 789")
            ) { api ->
                runSuspend { api.deleteBill("123 456 789") }
            },
            EndpointCall(
                "getPaymentStatus",
                "GET",
                "/einvoice/api/paymentStatus",
                mapOf("wbc_code" to "123 456 789")
            ) { api ->
                runSuspend { api.getPaymentStatus("123 456 789") }
            },
            EndpointCall(
                "getBillByReference",
                "GET",
                "/einvoice/api/bill",
                mapOf("bill_reference" to "kt/unit/1")
            ) { api ->
                runSuspend { api.getBillByReference("kt/unit/1") }
            },
            EndpointCall(
                "getBillByPaymentCode",
                "GET",
                "/einvoice/api/bill",
                mapOf("wbc_code" to "123 456 789")
            ) { api ->
                runSuspend { api.getBillByPaymentCode("123 456 789") }
            },
            EndpointCall(
                "getBills",
                "GET",
                "/einvoice/api/bills",
                mapOf("payment_status" to "-1", "last_timestamp" to exampleCursor, "limit" to "10")
            ) { api ->
                runSuspend { api.getBills(paymentStatus = -1, lastTimeStamp = exampleCursor, limit = 10) }
            },
            EndpointCall(
                "getPayments",
                "GET",
                "/einvoice/api/payments",
                mapOf("last_timestamp" to exampleCursor, "limit" to "10")
            ) { api ->
                runSuspend { api.getPayments(lastTimeStamp = exampleCursor, limit = 10) }
            },
            EndpointCall(
                "getStat",
                "GET",
                "/merchant/stat",
                mapOf("date_from" to "2025-01-01", "date_to" to "2030-01-31")
            ) { api ->
                runSuspend { api.getStat("2025-01-01", "2030-01-31") }
            },
            EndpointCall("getSupportedBanks", "GET", "/einvoice/api/banks") { api ->
                runSuspend { api.getSupportedBanks() }
            }
        )

    private fun requestBody(request: okhttp3.mockwebserver.RecordedRequest): Map<*, *> =
        Gson().fromJson(request.body.readUtf8(), Map::class.java)

    private fun sampleBill(): Bill =
        Bill(
            customerCode = "cc01",
            customerName = "Elias Haileselassie",
            billReference = "kt/2021/130",
            time = "2021-07-22 22:14",
            description = "hotel booking",
            amount = "270.90",
            merchantID = "x",
            customerPhone = "0911000000",
            extras = emptyMap()
        )

    private fun liveSampleBill(billReference: String): Bill =
        Bill(
            customerCode = "cc01",
            customerName = "Elias Haileselassie",
            billReference = billReference,
            time = "2021-07-22 22:14",
            description = "hotel booking",
            amount = "270.90",
            customerPhone = "0911000000",
            extras = emptyMap()
        )

    private fun assertNoApiError(response: ApiResponse<*>, operation: String) {
        assertNull(response.error, "$operation failed: ${response.error} ${response.errorCode}")
    }

    private fun assertCreatedBill(
        bill: BillResponse?,
        billReference: String,
        merchantId: String,
        paymentCode: String
    ) {
        assertEquals(billReference.lowercase(), bill?.billReference?.lowercase())
        assertEquals("cc01", bill?.customerCode?.lowercase())
        assertEquals("Elias Haileselassie", bill?.customerName)
        assertEquals("0911000000", bill?.customerPhone)
        assertEquals("hotel booking", bill?.description)
        assertEquals(merchantId, bill?.merchantID)
        assertEquals(normalizePaymentCode(paymentCode), normalizePaymentCode(bill?.wbcCode ?: ""))
        assertFalse(bill?.updateTimeStamp.isNullOrEmpty())
    }

    private fun normalizePaymentCode(value: String): String =
        value.replace(" ", "")

    private fun cursorBefore(updateTimeStamp: String, fallback: String): String {
        if (updateTimeStamp.isEmpty() || updateTimeStamp.any { !it.isDigit() }) {
            return fallback
        }

        val chars = updateTimeStamp.toCharArray()
        for (index in chars.indices.reversed()) {
            if (chars[index] > '0') {
                chars[index] = chars[index] - 1
                return String(chars)
            }
            chars[index] = '9'
        }
        return fallback
    }

    private fun billResponseJson(): Map<String, Any> =
        mapOf(
            "customerCode" to "cc01",
            "customerName" to "Elias Haileselassie",
            "customerPhone" to "0911000000",
            "time" to "2021-07-22 22:14",
            "description" to "hotel booking",
            "amount" to "270.90",
            "billReference" to "kt/2021/130",
            "merchantID" to "merchant-from-client",
            "extras" to emptyMap<String, Any>(),
            "wbcCode" to "123 456 789",
            "paymentStatus" to 0,
            "updateTimeStamp" to "20250101100000000001"
        )

    private fun paymentStatusJson(): Map<String, Any> =
        mapOf(
            "status" to 2,
            "data" to mapOf(
                "id" to 1,
                "status" to 2,
                "bankID" to "cbe_birr",
                "paymentReference" to "BANK-REF-1",
                "paymentDate" to "2025-01-01 10:00:00",
                "confirmed" to true,
                "confirmedTime" to "2025-01-01 10:00:01",
                "amount" to "270.90",
                "wbcCode" to "123 456 789",
                "updateTimeStamp" to "20250101100001000001"
            )
        )

    private fun paymentResponseJson(): Map<String, Any> =
        mapOf(
            "status" to 3,
            "id" to 2,
            "bankID" to "cbe_birr",
            "paymentReference" to "BANK-REF-2",
            "paymentDate" to "2025-01-01 10:01:00",
            "confirmed" to true,
            "confirmedTime" to "2025-01-01 10:01:01",
            "canceled" to true,
            "canceledTime" to "2025-01-01 10:02:00",
            "amount" to "270.90",
            "wbcCode" to "123 456 789",
            "updateTimeStamp" to "20250101100100000001"
        )

    private fun paymentWebhookPayloadJson(): Map<String, Any> =
        mapOf(
            "status" to 2,
            "data" to mapOf(
                "status" to 2,
                "id" to 121356,
                "bankID" to "cbe_mobile",
                "paymentReference" to "FTC356A577695",
                "paymentDate" to "2026-06-25 12:00:00",
                "time" to "2026-06-25 12:00:00",
                "confirmed" to true,
                "confirmedTime" to "2026-06-25 12:00:00",
                "canceled" to false,
                "canceledTime" to "",
                "amount" to "100.00",
                "wbcCode" to "000 000 000",
                "updateTimeStamp" to "2026062512000000000"
            )
        )

    private fun successResponse(): MockResponse =
        MockResponse().setResponseCode(200).setBody(
            """{"error":null,"errorCode":null,"res":"OK"}"""
        )

    private fun apiErrorResponse(): MockResponse =
        MockResponse().setResponseCode(200).setBody(
            """{"error":"invalid api key","errorCode":"ERROR_INVALID_API_KEY","res":null}"""
        )
}

private data class EndpointCall(
    val name: String,
    val method: String,
    val path: String,
    val expectedQuery: Map<String, String> = emptyMap(),
    val invoke: (WeBirrClient) -> ApiResponse<*>
)
