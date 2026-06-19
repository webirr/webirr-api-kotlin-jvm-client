package webirr

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Internal class to use Retrofit.
 */
internal interface WeBirrApi {
    @POST("einvoice/api/bill")
    @Headers("Content-Type: application/json")
    fun createBill(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Body bill: Bill
    ): Call<ApiResponse<String>>

    @PUT("einvoice/api/bill")
    @Headers("Content-Type: application/json")
    fun updateBill(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Body bill: Bill
    ): Call<ApiResponse<String>>

    @DELETE("einvoice/api/bill")
    fun deleteBill(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("wbc_code") paymentCode: String
    ): Call<ApiResponse<String>>

    @GET("einvoice/api/paymentStatus")
    fun getPaymentStatus(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("wbc_code") paymentCode: String
    ): Call<ApiResponse<Payment>>

    @GET("einvoice/api/bill")
    fun getBillByReference(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("bill_reference") billReference: String
    ): Call<ApiResponse<BillResponse>>

    @GET("einvoice/api/bill")
    fun getBillByPaymentCode(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("wbc_code") paymentCode: String
    ): Call<ApiResponse<BillResponse>>

    @GET("einvoice/api/bills")
    fun getBills(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("payment_status") paymentStatus: Int,
        @Query("last_timestamp") lastTimeStamp: String,
        @Query("limit") limit: Int
    ): Call<ApiResponse<List<BillResponse>>>

    @GET("einvoice/api/payments")
    fun getPayments(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("last_timestamp") lastTimeStamp: String,
        @Query("limit") limit: Int
    ): Call<ApiResponse<List<PaymentResponse>>>

    @GET("merchant/stat")
    fun getStat(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?,
        @Query("date_from") dateFrom: String,
        @Query("date_to") dateTo: String
    ): Call<ApiResponse<Stat>>

    @GET("einvoice/api/banks")
    fun getSupportedBanks(
        @Query("api_key") apiKey: String,
        @Query("merchant_id") merchantId: String?
    ): Call<ApiResponse<List<SupportedBank>>>
}

internal object WeBirrApiAdapter {
    fun createWeBirrApi(isTestEnv: Boolean, okHttpClient: OkHttpClient = OkHttpClient()): WeBirrApi {
        val baseUrl = if (isTestEnv) "https://api.webirr.net/" else "https://api.webirr.net:8080/"
        return createWeBirrApi(baseUrl, okHttpClient)
    }

    fun createWeBirrApi(baseUrl: String, okHttpClient: OkHttpClient = OkHttpClient()): WeBirrApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeBirrApi::class.java)
}
