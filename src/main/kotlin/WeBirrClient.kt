package webirr

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
     * @returns {Unit} but uses callBack that will be called when the async task is done.
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of the returned PaymentCode on success.
     */
    fun createBillAsync(bill: Bill, callBack: (ApiResponse<String>) -> Unit) {
        val call = api.createBill(apiKey, merchantId, prepareBill(bill))
        call.enqueue(ApiResponseCallBack(callBack))
    }

    /**
     * Update an existing bill at WeBirr Servers, if the bill is not paid yet.
     * The billReference has to be the same as the original bill created.
     * @param {Bill} bill represents an invoice or bill for a customer
     * @returns {Unit} but uses callBack that will be called when the async task is done.
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of "OK" on success.
     */
    fun updateBillAsync(bill: Bill, callBack: (ApiResponse<String>) -> Unit) {
        val call = api.updateBill(apiKey, merchantId, prepareBill(bill))
        call.enqueue(ApiResponseCallBack(callBack))
    }

    /**
     * Delete an existing bill at WeBirr Servers, if the bill is not paid yet.
     * @param {string} paymentCode is the number that WeBirr Payment Gateway returns on createBillAsync.
     */
    fun deleteBillAsync(paymentCode: String, callBack: (ApiResponse<String>) -> Unit) {
        val call = api.deleteBill(apiKey, merchantId, paymentCode)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    /**
     * Get Payment Status of a bill from WeBirr Servers.
     */
    fun getPaymentStatusAsync(paymentCode: String, callBack: (ApiResponse<Payment>) -> Unit) {
        val call = api.getPaymentStatus(apiKey, merchantId, paymentCode)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getBillByReferenceAsync(billReference: String, callBack: (ApiResponse<BillResponse>) -> Unit) {
        val call = api.getBillByReference(apiKey, merchantId, billReference)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getBillByPaymentCodeAsync(paymentCode: String, callBack: (ApiResponse<BillResponse>) -> Unit) {
        val call = api.getBillByPaymentCode(apiKey, merchantId, paymentCode)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getBillsAsync(
        paymentStatus: Int = -1,
        lastTimeStamp: String = "",
        limit: Int = 100,
        callBack: (ApiResponse<List<BillResponse>>) -> Unit
    ) {
        val call = api.getBills(apiKey, merchantId, paymentStatus, lastTimeStamp, limit)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getPaymentsAsync(
        lastTimeStamp: String = "",
        limit: Int = 100,
        callBack: (ApiResponse<List<PaymentResponse>>) -> Unit
    ) {
        val call = api.getPayments(apiKey, merchantId, lastTimeStamp, limit)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getStatAsync(dateFrom: String, dateTo: String, callBack: (ApiResponse<Stat>) -> Unit) {
        val call = api.getStat(apiKey, merchantId, dateFrom, dateTo)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    fun getSupportedBanksAsync(callBack: (ApiResponse<List<SupportedBank>>) -> Unit) {
        val call = api.getSupportedBanks(apiKey, merchantId)
        call.enqueue(ApiResponseCallBack(callBack))
    }

    private fun prepareBill(bill: Bill): Bill {
        bill.merchantID = merchantId
        return bill
    }
}

class ApiResponseCallBack<T>(private val callBack: (ApiResponse<T>) -> Unit) : Callback<ApiResponse<T>> {
    override fun onResponse(call: Call<ApiResponse<T>>, response: Response<ApiResponse<T>>) {
        if (response.isSuccessful) {
            callBack(response.body() ?: ApiResponse("empty response"))
        } else {
            callBack(ApiResponse("http error ${response.raw().code()} ${response.raw().message()}"))
        }
    }

    override fun onFailure(call: Call<ApiResponse<T>>, t: Throwable) {
        callBack(ApiResponse("exception ${t.message}"))
    }
}
