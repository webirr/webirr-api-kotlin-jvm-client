package webirr

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A WeBirrClient instance object can be used to
 * Create, Update or Delete a Bill at WeBirr Servers and also to
 * Get bill/payment information.
 * It is a wrapper for the REST Web Service API.
 */
class WeBirrClient {
    private val apiKey: String
    private val merchantId: String
    private val api: WeBirrApi

    constructor(
        merchantId: String,
        apiKey: String,
        isTestEnv: Boolean,
        okHttpClient: OkHttpClient = OkHttpClient()
    ) {
        this.apiKey = apiKey
        this.merchantId = merchantId
        this.api = WeBirrApiAdapter.createWeBirrApi(isTestEnv, okHttpClient)
    }

    internal constructor(merchantId: String, apiKey: String, api: WeBirrApi) {
        this.apiKey = apiKey
        this.merchantId = merchantId
        this.api = api
    }

    /**
     * Create a new bill at WeBirr Servers.
     * @param {Bill} bill represents an invoice or bill for a customer
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of the returned PaymentCode on success.
     */
    suspend fun createBill(bill: Bill): ApiResponse<String> =
        await(api.createBill(apiKey, merchantId, prepareBill(bill)))

    /**
     * Update an existing bill at WeBirr Servers, if the bill is not paid yet.
     * The billReference has to be the same as the original bill created.
     * @param {Bill} bill represents an invoice or bill for a customer
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of "OK" on success.
     */
    suspend fun updateBill(bill: Bill): ApiResponse<String> =
        await(api.updateBill(apiKey, merchantId, prepareBill(bill)))

    /**
     * Delete an existing bill at WeBirr Servers, if the bill is not paid yet.
     * @param {string} paymentCode is the number that WeBirr Payment Gateway returns on createBill.
     */
    suspend fun deleteBill(paymentCode: String): ApiResponse<String> =
        await(api.deleteBill(apiKey, merchantId, paymentCode))

     /**
     * Get Payment Status of a bill from WeBirr Servers.
     */
    suspend fun getPaymentStatus(paymentCode: String): ApiResponse<Payment> =
        await(api.getPaymentStatus(apiKey, merchantId, paymentCode))

    suspend fun getBillByReference(billReference: String): ApiResponse<BillResponse> =
        await(api.getBillByReference(apiKey, merchantId, billReference))

    suspend fun getBillByPaymentCode(paymentCode: String): ApiResponse<BillResponse> =
        await(api.getBillByPaymentCode(apiKey, merchantId, paymentCode))

    suspend fun getBills(
        paymentStatus: Int = -1,
        lastTimeStamp: String = "",
        limit: Int = 100
    ): ApiResponse<List<BillResponse>> =
        await(api.getBills(apiKey, merchantId, paymentStatus, lastTimeStamp, limit))

    suspend fun getPayments(
        lastTimeStamp: String = "",
        limit: Int = 100
    ): ApiResponse<List<PaymentResponse>> =
        await(api.getPayments(apiKey, merchantId, lastTimeStamp, limit))

    suspend fun getStat(dateFrom: String, dateTo: String): ApiResponse<Stat> =
        await(api.getStat(apiKey, merchantId, dateFrom, dateTo))

    suspend fun getSupportedBanks(): ApiResponse<List<SupportedBank>> =
        await(api.getSupportedBanks(apiKey, merchantId))

    private fun prepareBill(bill: Bill): Bill {
        bill.merchantID = merchantId
        return bill
    }

    private suspend fun <T> await(call: Call<ApiResponse<T>>): ApiResponse<T> =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback<ApiResponse<T>> {
                override fun onResponse(call: Call<ApiResponse<T>>, response: Response<ApiResponse<T>>) {
                    if (!continuation.isActive) {
                        return
                    }
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(
                            WebirrPlatformException(
                                "http error ${response.raw().code()} ${response.raw().message()}",
                                response.raw().code(),
                                response.raw().message()
                            )
                        )
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        continuation.resumeWithException(
                            WebirrPlatformException(
                                "empty response",
                                response.raw().code(),
                                response.raw().message()
                            )
                        )
                        return
                    }

                    continuation.resume(body)
                }

                override fun onFailure(call: Call<ApiResponse<T>>, t: Throwable) {
                    if (!continuation.isActive) {
                        return
                    }
                    continuation.resumeWithException(t)
                }
            })
        }
}

class WebirrPlatformException(
    message: String,
    val statusCode: Int? = null,
    val status: String? = null,
    cause: Throwable? = null
) : IOException(message, cause) {
    fun isTransient(): Boolean =
        statusCode == null || statusCode >= 500 || statusCode == 429 || statusCode == 408
}

object WebirrErrors {
    fun isTransient(error: Throwable): Boolean {
        if (error is WebirrPlatformException) {
            return error.isTransient()
        }
        if (error is SocketTimeoutException) {
            return true
        }
        if (error is IOException) {
            return true
        }
        return false
    }
}
