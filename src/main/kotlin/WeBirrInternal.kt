package webirr

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Internal class to use Retrofit
 */
internal interface WeBirrApi {

    @POST("einvoice/api/postbill")
    @Headers("Content-Type: application/json")
    fun createBill(@Query("api_key") apiKey: String, @Body bill: Bill): Call<ApiResponse<String>>

    @PUT("einvoice/api/postbill")
    @Headers("Content-Type: application/json")
    fun updateBill(@Query("api_key") apiKey: String, @Body bill: Bill): Call<ApiResponse<String>>

    @GET("getPaymentStatus")
    fun getPaymentStatus(@Query("api_key") apiKey: String, @Query("bill_reference") billRef: String): Call<ApiResponse<Payment>>
}

internal object WeBirrApiAdapter {

    fun CreateWeBirrApi(isTestEnv: Boolean) : WeBirrApi = Retrofit.Builder()
        .baseUrl( if(isTestEnv) "https://api.webirr.com/" else "https://api.webirr.com:8080" )
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeBirrApi ::class.java)
}