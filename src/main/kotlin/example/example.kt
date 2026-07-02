package webirr.example

import com.google.gson.Gson
import webirr.Bill
import webirr.PaymentRecord
import webirr.PaymentWebhookPayload
import webirr.WeBirrClient

val apiKey: String = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId: String = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"
val api = WeBirrClient(merchantId, apiKey, true)

suspend fun main() {
    createAndUpdateBill()
    Thread.sleep(2000)
    getPaymentStatus()
    Thread.sleep(2000)
    getBillAndListBills()
    Thread.sleep(2000)
    deleteBill()
    Thread.sleep(2000)
    BulkPaymentPollingConsumer(api).fetchAndProcessPayments()
    Thread.sleep(2000)
    getStat()
    Thread.sleep(2000)
    getSupportedBanks()
}

/**
 * Creating a new Bill / Updating an existing Bill on WeBirr Servers
 */
suspend fun createAndUpdateBill() {
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

    val created = api.createBill(bill)
    if (created.error == null) {
        // success
        val paymentCode = created.res ?: "" // returns paymentcode such as 429 723 975
        println("Payment Code = $paymentCode") // we may want to save payment code in local db.
    } else {
        // fail
        println("error: ${created.error}")
        println("errorCode: ${created.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT_DUP_REF
    }

    // update existing bill if it is not paid
    bill.amount = "278.00"
    bill.customerName = "Elias kotlin"
    //bill.billReference = "WE SHOULD NOT CHANGE THIS";

    println("Updating Bill...")
    val updated = api.updateBill(bill)
    if (updated.error == null) {
        // success
        println("bill is updated successfully") //it.res will be 'OK'  no need to check here!
    } else {
        // fail
        println("error: ${updated.error}")
        println("errorCode: ${updated.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
    }
}

/**
 * Getting a Bill and Listing Bills
 */
suspend fun getBillAndListBills() {
    val billReference = "BILL_REFERENCE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL"
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL"

    println("Getting Bill By Reference...")
    val byReference = api.getBillByReference(billReference)
    if (byReference.error == null) {
        // success
        println("Payment Code = ${byReference.res?.wbcCode}")
        println("Payment Status = ${byReference.res?.paymentStatus}")
        println("Last Timestamp = ${byReference.res?.updateTimeStamp}")
    } else {
        // fail
        println("error: ${byReference.error}")
        println("errorCode: ${byReference.errorCode}")
    }

    println("Getting Bill By Payment Code...")
    val byPaymentCode = api.getBillByPaymentCode(paymentCode)
    if (byPaymentCode.error == null) {
        // success
        println("Bill Reference = ${byPaymentCode.res?.billReference}")
        println("Payment Status = ${byPaymentCode.res?.paymentStatus}")
        println("Last Timestamp = ${byPaymentCode.res?.updateTimeStamp}")
    } else {
        // fail
        println("error: ${byPaymentCode.error}")
        println("errorCode: ${byPaymentCode.errorCode}")
    }

    println("Listing Bills...")
    val paymentStatus = -1 // -1 all, 0 pending, 1 unconfirmed payment, 2 paid.
    val lastTimeStamp = "20251231" // Date-only cursor; use "20251231235959" when you need time precision.
    val limit = 10

    val bills = api.getBills(paymentStatus, lastTimeStamp, limit)
    if (bills.error == null) {
        // success
        println("Bills returned: ${bills.res?.size ?: 0}")
        for (bill in bills.res ?: emptyList()) {
            println("Bill Reference = ${bill.billReference}")
            println("Payment Code = ${bill.wbcCode}")
            println("Payment Status = ${bill.paymentStatus}")
            println("Last Timestamp = ${bill.updateTimeStamp}")
        }
    } else {
        // fail
        println("error: ${bills.error}")
        println("errorCode: ${bills.errorCode}")
    }
}

/**
 * Getting Payment status of an existing Bill from WeBirr Servers
 */
suspend fun getPaymentStatus() {
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // such as '141 263 782';

    println("Getting Payment Status...")

    val status = api.getPaymentStatus(paymentCode)
    if (status.error == null) {
        // success
        if (status.res?.isPaid ?: false) {
            println("bill is paid")
            println("bill payment detail")
            println("Bank: ${status.res?.data?.bankID}")
            println("Bank Reference Number: ${status.res?.data?.paymentReference}")
            println("Amount Paid: ${status.res?.data?.amount}")
            println("Payment Date: ${status.res?.data?.paymentDate}")
        } else {
            println("bill is pending payment")
        }
    } else {
        // fail
        println("error: ${status.error}")
        println("errorCode: ${status.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
    }
}

suspend fun deleteBill() {
    val paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // suchas as '141 263 782';

    println("Deleting Bill...")

    val deleted = api.deleteBill(paymentCode)
    if (deleted.error == null) {
        // success
        println("bill is deleted successfully") //res.res will be 'OK'  no need to check here!
    } else {
        // fail
        println("error: ${deleted.error}")
        println("errorCode: ${deleted.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
    }
}

/**
 * Getting list of Payments and process them with Bulk Polling Consumer
 */
class BulkPaymentPollingConsumer(private val api: WeBirrClient) {
    private var lastTimeStamp = "20251231" // use a saved cursor; time precision can be like "20251231235959"

    suspend fun fetchAndProcessPayments() {
        val limit = 100

        println("Getting Payments...")

        val payments = api.getPayments(lastTimeStamp, limit)
        if (payments.error == null) {
            // success
            for (payment in payments.res ?: emptyList()) {
                processPayment(payment)
                if (payment.updateTimeStamp > lastTimeStamp) {
                    lastTimeStamp = payment.updateTimeStamp
                    println("Next cursor candidate: $lastTimeStamp")
                }
            }
            // Save lastTimeStamp to your database only after the batch is processed successfully.
        } else {
            // fail
            println("error: ${payments.error}")
            println("errorCode: ${payments.errorCode}")
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
        val payload = Gson().fromJson(rawBody, PaymentWebhookPayload::class.java)
        processPayment(payload.data)
        200 to """{"error":null}"""
    } catch (_: Exception) {
        400 to """{"error":"invalid json"}"""
    }
}

fun processPayment(payment: PaymentRecord) {
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
suspend fun getStat() {
    val dateFrom = "2025-01-01"
    val dateTo = "2030-01-31"

    println("Getting Stat...")

    val stat = api.getStat(dateFrom, dateTo)
    if (stat.error == null) {
        // success
        println("Bills Created: ${stat.res?.nBills}")
        println("Bills Paid: ${stat.res?.nBillsPaid}")
        println("Bills Unpaid: ${stat.res?.nBillsUnpaid}")
        println("Amount Bills: ${stat.res?.amountBills}")
        println("Amount Paid: ${stat.res?.amountPaid}")
        println("Amount Unpaid: ${stat.res?.amountUnpaid}")
    } else {
        // fail
        println("error: ${stat.error}")
        println("errorCode: ${stat.errorCode}")
    }
}

/**
 * Getting banks enabled for this merchant checkout.
 */
suspend fun getSupportedBanks() {
    println("Getting Supported Banks...")

    val banks = api.getSupportedBanks()
    if (banks.error == null) {
        for (bank in banks.res ?: emptyList()) {
            println("${bank.bankID} - ${bank.name}")
        }
        println("Use only these merchant-specific banks when showing checkout payment instructions.")
    } else {
        println("error: ${banks.error}")
        println("errorCode: ${banks.errorCode}")
    }
}
