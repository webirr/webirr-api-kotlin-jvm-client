package webirr.example

/**
 * example.kt
 * Creating a new Bill / Updating an existing Bill on WeBirr Servers
 */

import webirr.*;

fun main() {

    val apiKey = "YOUR_API_KEY"
    val merchantId = "YOUR_MERCHANT_ID"

    val api = WeBirrClient(apiKey, true)

    var bill = Bill(
        "270.90",
        "cc01",
        "Elias Haileselassie",
        "2021-07-22 22:14",
        "hotel booking",
        "kt/2021/130",
        merchantId
    )

    println("Creating Bill...")

    api.createBill(bill) { it ->

        if (it.error == null) {
            // success
            val paymentCode = it.res ?: ""; // returns paymentcode such as 429 723 975
            println("Payment Code = ${paymentCode}"); // we may want to save payment code in local db.

        } else {
            // fail
            println("error: ${it.error}");
            println("errorCode: ${it.errorCode}"); // can be used to handle specific busines error such as ERROR_INVLAID_INPUT_DUP_REF
        }
    }

// the above method call is async!
    Thread.sleep(2000)

}


