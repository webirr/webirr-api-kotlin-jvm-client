package webirr.example

import com.google.gson.Gson
import webirr.Bill
import webirr.PaymentResponse
import webirr.WeBirrClient

val apiKey: String = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId: String = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"
val api = WeBirrClient(merchantId, apiKey, true)

fun main() {
    createAndUpdateBillAsync()
    Thread.sleep(2000)
    getPaymentStatusAsync()
    Thread.sleep(2000)
    getBillAndListBillsAsync()
    Thread.sleep(2000)
    deleteBillAsync()
    Thread.sleep(2000)
    BulkPaymentPollingConsumer(api).fetchAndProcessPayments()
    Thread.sleep(2000)
    getStatAsync()
    Thread.sleep(2000)
    getSupportedBanksAsync()
}

/**
 * Creating a new Bill / Updating an existing Bill on WeBirr Servers
 */
fun createAndUpdateBillAsync() {
    val bill = Bill(
        customerCode = "cc01", // it can be email address or phone number if you dont have customer code
        customerName = "Elias Haileselassie",
        billReference = "kt/2021/130", // your unique reference number
        time = "2021-07-22 22:14", // your bill time, always in this format
        description = "hotel booking",
        amount = "270.90",
        customerPhone = "0911000000",
        extras = emptyMap()
    )

    println("Creating Bill...")

    api.createBillAsync(bill) {
        if (it.error == null) {
            // success
            val paymentCode = it.res ?: "" // returns paymentcode such as 429 723 975
            println("Payment Code = $paymentCode") // we may want to save payment code in local db.
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT_DUP_REF
        }
    }

    // the above method call is async!
    Thread.sleep(2000)

    // update existing bill if it is not paid
    bill.amount = "278.00"
    bill.customerName = "Elias kotlin"
    //bill.billReference = "WE SHOULD NOT CHANGE THIS";

    println("Updating Bill...")
    api.updateBillAsync(bill) {
        if (it.error == null) {
            // success
            println("bill is updated successfully") //it.res will be 'OK'  no need to check here!
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }
    }
}

/**
 * Getting a Bill and Listing Bills
 */
fun getBillAndListBillsAsync() {
    val billReference = "BILL_REFERENCE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL"
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL"

    println("Getting Bill By Reference...")
    api.getBillByReferenceAsync(billReference) {
        if (it.error == null) {
            // success
            println("Payment Code = ${it.res?.wbcCode}")
            println("Payment Status = ${it.res?.paymentStatus}")
            println("Last Timestamp = ${it.res?.updateTimeStamp}")
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}")
        }
    }

    Thread.sleep(2000)

    println("Getting Bill By Payment Code...")
    api.getBillByPaymentCodeAsync(paymentCode) {
        if (it.error == null) {
            // success
            println("Bill Reference = ${it.res?.billReference}")
            println("Payment Status = ${it.res?.paymentStatus}")
            println("Last Timestamp = ${it.res?.updateTimeStamp}")
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}")
        }
    }

    Thread.sleep(2000)

    println("Listing Bills...")
    val paymentStatus = -1 // -1 all, 0 pending, 1 unconfirmed payment, 2 paid.
    val lastTimeStamp = "20251231" // Date-only cursor; use "20251231235959" when you need time precision.
    val limit = 10

    api.getBillsAsync(paymentStatus, lastTimeStamp, limit) {
        if (it.error == null) {
            // success
            println("Bills returned: ${it.res?.size ?: 0}")
            for (bill in it.res ?: emptyList()) {
                println("Bill Reference = ${bill.billReference}")
                println("Payment Code = ${bill.wbcCode}")
                println("Payment Status = ${bill.paymentStatus}")
                println("Last Timestamp = ${bill.updateTimeStamp}")
            }
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}")
        }
    }
}

/**
 * Getting Payment status of an existing Bill from WeBirr Servers
 */
fun getPaymentStatusAsync() {
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // such as '141 263 782';

    println("Getting Payment Status...")

    api.getPaymentStatusAsync(paymentCode) {
        if (it.error == null) {
            // success
            if (it.res?.isPaid ?: false) {
                println("bill is paid")
                println("bill payment detail")
                println("Bank: ${it.res?.data?.bankID}")
                println("Bank Reference Number: ${it.res?.data?.paymentReference}")
                println("Amount Paid: ${it.res?.data?.amount}")
                println("Payment Date: ${it.res?.data?.paymentDate}")
            } else {
                println("bill is pending payment")
            }
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }
    }
}

fun deleteBillAsync() {
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // suchas as '141 263 782';

    println("Deleting Bill...")

    api.deleteBillAsync(paymentCode) {
        if (it.error == null) {
            // success
            println("bill is deleted successfully") //res.res will be 'OK'  no need to check here!
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }
    }
}

/**
 * Getting list of Payments and process them with Bulk Polling Consumer
 */
class BulkPaymentPollingConsumer(private val api: WeBirrClient) {
    private var lastTimeStamp = "20251231" // use a saved cursor; time precision can be like "20251231235959"

    fun fetchAndProcessPayments() {
        val limit = 100

        println("Getting Payments...")

        api.getPaymentsAsync(lastTimeStamp, limit) {
            if (it.error == null) {
                // success
                for (payment in it.res ?: emptyList()) {
                    processPayment(payment)
                    if (payment.updateTimeStamp.isNotEmpty()) {
                        lastTimeStamp = payment.updateTimeStamp
                        println("Last Timestamp: $lastTimeStamp") // save updateTimeStamp to your database for the next getPaymentsAsync() call
                    }
                }
            } else {
                // fail
                println("error: ${it.error}")
                println("errorCode: ${it.errorCode}")
            }
        }
    }
}

/**
 * Webhooks - Payment processing using Webhook Callbacks
 */
fun processWebhookPayment(rawBody: String, authKey: String?): Pair<Int, String> {
    val expectedAuthKey = System.getenv("WEBIRR_WEBHOOK_AUTH_KEY") ?: "YOUR_WEBHOOK_AUTH_KEY"

    if (authKey != expectedAuthKey) {
        return 401 to """{"error":"unauthorized"}"""
    }

    if (rawBody.isEmpty()) {
        return 400 to """{"error":"empty request body"}"""
    }

    return try {
        val payment = Gson().fromJson(rawBody, PaymentResponse::class.java)
        processPayment(payment)
        200 to """{"error":null}"""
    } catch (_: Exception) {
        400 to """{"error":"invalid json"}"""
    }
}

fun processPayment(payment: PaymentResponse) {
    if (payment.isPaid) {
        println("bill is paid")
    } else if (payment.isReversed) {
        println("bill payment is reversed")
    }

    println("Bank: ${payment.bankID}")
    println("Bank Reference Number: ${payment.paymentReference}")
    println("Amount Paid: ${payment.amount}")
    println("Payment Date: ${payment.paymentDate}")
    println("Canceled Time: ${payment.canceledTime}")
    println("Update Timestamp: ${payment.updateTimeStamp}")
}

/**
 * Gettting basic Statistics about bills created and payments received for a date range
 */
fun getStatAsync() {
    val dateFrom = "2025-01-01"
    val dateTo = "2030-01-31"

    println("Getting Stat...")

    api.getStatAsync(dateFrom, dateTo) {
        if (it.error == null) {
            // success
            println("Bills Created: ${it.res?.nBills}")
            println("Bills Paid: ${it.res?.nBillsPaid}")
            println("Bills Unpaid: ${it.res?.nBillsUnpaid}")
            println("Amount Bills: ${it.res?.amountBills}")
            println("Amount Paid: ${it.res?.amountPaid}")
            println("Amount Unpaid: ${it.res?.amountUnpaid}")
        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}")
        }
    }
}

/**
 * Getting banks enabled for this merchant checkout.
 */
fun getSupportedBanksAsync() {
    println("Getting Supported Banks...")

    api.getSupportedBanksAsync {
        if (it.error == null) {
            for (bank in it.res ?: emptyList()) {
                println("${bank.bankID} - ${bank.name}")
            }
            println("Use only these merchant-specific banks when showing checkout payment instructions.")
        } else {
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}")
        }
    }
}
