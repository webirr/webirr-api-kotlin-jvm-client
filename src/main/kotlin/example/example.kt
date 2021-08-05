/**
 * Creating a new Bill / Updating an existing Bill on WeBirr Servers
 */
package webirr.example

import webirr.*;

fun main() {

    //val apiKey = "YOUR_API_KEY"
    //val merchantId = "YOUR_MERCHANT_ID"

    val apiKey =  System.getenv("wb_apikey_1") ?: ""
    val merchantId =  System.getenv("wb_merchid_1") ?: ""

    val api = WeBirrClient(apiKey, true)

    var bill = Bill(
        "cc01",
        "Elias Haileselassie",
        "kt/2021/130",
        "2021-07-22 22:14",
        "hotel booking",
        "270.90",
        merchantId,
    )

    println("Creating Bill...")

    api.createBillAsync(bill) { it ->

        if (it.error == null) {
            // success
            val paymentCode = it.res ?: "" // returns paymentcode such as 429 723 975
            println("Payment Code = ${paymentCode}") // we may want to save payment code in local db.

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

    println("Updating Bill...");
    api.updateBillAsync(bill) { it ->

        if (it.error == null) {
            // success
            println("bill is updated successfully")  //it.res will be 'OK'  no need to check here!

        } else {
            // fail
            println("error: ${it.error}")
            println("errorCode: ${it.errorCode}") // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }
    }

}


