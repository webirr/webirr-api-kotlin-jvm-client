package webirr

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A WeBirrClient instance object can be used to
 * Create, Update or Delete a Bill at WeBirr Servers and also to
 * Get the Payment Status of a bill.
 * It is a wrapper for the REST Web Service API.
 */
class WeBirrClient(apiKey:  String, isTestEnv: Boolean) {

    private var _apiKey: String
    private var _api: WeBirrApi

    init {
       _apiKey = apiKey;
       _api = WeBirrApiAdapter.CreateWeBirrApi(isTestEnv);
    }
    /**
     * Create a new bill at WeBirr Servers.
     * @param {Bill} bill represents an invoice or bill for a customer
     * @returns {Unit} but uses callBack that will be called when the async task is done.
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of the returned PaymentCode on success.
     */
    fun createBillAsync(bill: Bill, callBack: (ApiResponse<String>) -> Unit ) {

        var call = _api.createBill(_apiKey, bill)
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
    fun updateBillAsync(bill: Bill, callBack: (ApiResponse<String>) -> Unit ) {

        var call = _api.updateBill(_apiKey, bill)
        call.enqueue(ApiResponseCallBack(callBack))

    }
    /**
     * Delete an existing bill at WeBirr Servers, if the bill is not paid yet.
     * @param {string} paymentCode is the number that WeBirr Payment Gateway returns on createBillAsync.
     * @returns {Unit} but uses callBack that will be called when the async task is done.
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of "OK" on success.
     */
    fun deleteBillAsync(paymentCode: String, callBack: (ApiResponse<String>) -> Unit ) {

        var call = _api.deleteBill(_apiKey, paymentCode)
        call.enqueue(ApiResponseCallBack(callBack))

    }
    /**
     * Get Payment Status of a bill from WeBirr Servers
     * @param {string} paymentCode is the number that WeBirr Payment Gateway returns on createBill.
     * @returns {Unit} but uses callBack that will be called when the async task is done.
     * Check if(returnedResult.error == null) to see if there are errors.
     * ApiResponse.res will have [Payment] object on success (will be null otherwise!)
     * ApiResponse.res?.isPaid ?? false -> will return true if the bill is paid (payment completed)
     * ApiResponse.res?.data ?? null -> will have [PaymentDetail] object
     */
    fun getPaymentStatusAsync(paymentCode: String, callBack: (ApiResponse<Payment>) -> Unit ) {

        var call = _api.getPaymentStatus(_apiKey, paymentCode)
        call.enqueue(ApiResponseCallBack(callBack))

    }
}

class ApiResponseCallBack<T>( val callBack: (ApiResponse<T>) -> Unit  ) : Callback<ApiResponse<T>> {

    override fun onResponse(call: Call<ApiResponse<T>>, response: Response<ApiResponse<T>>) {
        if(response.isSuccessful)
            callBack(response.body()!!)
        else
            callBack( ApiResponse<T>( "http error ${response.raw().code()} ${response.raw().message()}" ))
    }
    override fun onFailure(call: Call<ApiResponse<T>>, t: Throwable) {
        callBack( ApiResponse<T>( "exception ${t.message}" ))
    }

}