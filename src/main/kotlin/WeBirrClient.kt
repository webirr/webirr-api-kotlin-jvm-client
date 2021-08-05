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

    private lateinit var _apiKey: String
    private lateinit var _api: WeBirrApi

    init {
       _apiKey = apiKey;
       _api = WeBirrApiAdapter.CreateWeBirrApi(isTestEnv);
    }
    /**
     * Create a new bill at WeBirr Servers.
     * @param {object} bill represents an invoice or bill for a customer. see sample for structure of the Bill
     * @returns {object} see sample for structure of the returned ApiResponse Object
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of the returned PaymentCode on success.
     */
    fun createBill(bill: Bill, callBack: (ApiResponse<String>) -> Unit ) {

            var call = _api.createBill(_apiKey, bill)

            call.enqueue( object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                     if(response.isSuccessful){
                          callBack(response.body()!!);

                     } else {
                         callBack( ApiResponse<String>( "http error ${response.raw().code()} ${response.raw().message()}" ))
                     }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    callBack( ApiResponse<String>( "exception ${t.message}" ))
                }
            });
    }
    /**
     * Update an existing bill at WeBirr Servers, if the bill is not paid yet.
     * The billReference has to be the same as the original bill created.
     * @param {object} bill represents an invoice or bill for a customer. see sample for structure of the Bill
     * @returns {object} see sample for structure of the returned ApiResponse Object
     * Check if(ApiResponse.error == null) to see if there are errors.
     * ApiResponse.res will have the value of "OK" on success.
     */
    fun updateBill(bill: Bill, callBack: (ApiResponse<String>) -> Unit ) {

        var call = _api.updateBill(_apiKey, bill)

        call.enqueue( object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if(response.isSuccessful){
                    callBack(response.body()!!);

                } else {
                    callBack( ApiResponse<String>( "http error ${response.raw().code()} ${response.raw().message()}" ))
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                callBack( ApiResponse<String>( "exception ${t.message}" ))
            }
        });
    }

}