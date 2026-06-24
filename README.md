Official Kotlin/JVM Client Library for WeBirr Payment Gateway APIs

This Client Library provides convenient access to WeBirr Payment Gateway APIs from Java/Kotlin/JVM Apps.

## Install

include the following lines to add webirr client library into your project build

With gradle

Step 1. Add the JitPack repository to your build file

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies {
    implementation 'com.github.webirr:webirr-api-kotlin-jvm-client:Tag'
}
```

With maven

Step 1. Add the JitPack repository to your build file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Step 2. Add the dependency

```xml
<dependency>
    <groupId>com.github.webirr</groupId>
    <artifactId>webirr-api-kotlin-jvm-client</artifactId>
    <version>Tag</version>
</dependency>
```

## Usage

The library needs to be configured with a *merchant Id* & *API key*. You can get it by contacting [webirr.com](https://webirr.com)

> You can use this library for production or test environments. you will need to set isTestEnv=true for test, and false for production apps when creating objects of class WeBirrClient

For TestEnv examples and smoke tests, set these environment variables:

```bash
export WEBIRR_TEST_ENV_MERCHANT_ID="YOUR_MERCHANT_ID"
export WEBIRR_TEST_ENV_API_KEY="YOUR_API_KEY"
```

Create the client with merchant ID, API key, and environment. The client sets `Bill.merchantID` automatically before create/update calls, so examples should not set bill merchant ID manually.

```kotlin
val api = WeBirrClient(merchantId, apiKey, true)
```

For batch or mass bill workloads, you can pass a caller-owned `OkHttpClient` so your app controls connection reuse and transport policy.

```kotlin
val okHttpClient = OkHttpClient.Builder().build()
val api = WeBirrClient(merchantId, apiKey, true, okHttpClient)
```

In 2.x, the client constructor requires the merchant ID argument. When merchant ID is explicitly empty, the client does not send an empty `merchant_id` query parameter and does not overwrite `Bill.merchantID`.

## Example

### Creating a new Bill / Updating an existing Bill on WeBirr Servers

```kotlin
package webirr.example

import webirr.Bill
import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun createAndUpdateBillAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)

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
```

### Getting a Bill and Listing Bills

```kotlin
package webirr.example

import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun getBillAndListBillsAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)
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
```

### Getting Supported Banks for Checkout

```kotlin
package webirr.example

import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun getSupportedBanksAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)

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
```

Checkout pages should render bank-specific instructions only from `getSupportedBanksAsync()`. Do not show a broad static bank list unless those banks are returned for the configured merchant.

### Getting Payment status of an existing Bill from WeBirr Servers

```kotlin
package webirr.example

import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun getPaymentStatusAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)

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
```

*Sample object returned from getPaymentStatus()*

```json
{
  "status": 2,
  "data": {
    "id": 1,
    "status": 2,
    "bankID": "cbe_birr",
    "paymentReference": "BANK-REF-1",
    "paymentDate": "2025-01-01 10:00:00",
    "confirmed": true,
    "confirmedTime": "2025-01-01 10:00:01",
    "amount": "270.90",
    "wbcCode": "429 723 975",
    "updateTimeStamp": "20250101100001000001"
  }
}
```

### Deleting an existing Bill from WeBirr Servers (if it is not paid)

```kotlin
package webirr.example

import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun deleteBillAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)

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
```

### Getting list of Payments and process them with Bulk Polling Consumer

```kotlin
package webirr.example

import webirr.PaymentResponse
import webirr.WeBirrClient

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
```

### Webhooks - Payment processing using Webhook Callbacks

```kotlin
package webirr.example

import com.google.gson.Gson
import webirr.PaymentResponse

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
```

Host webhook handlers on HTTPS, validate the HTTP method is POST before calling the processing code, validate the `authKey`, make payment processing idempotent, and enqueue longer work to a background process.

### Gettting basic Statistics about bills created and payments received for a date range

```kotlin
package webirr.example

import webirr.WeBirrClient

val apiKey = System.getenv("WEBIRR_TEST_ENV_API_KEY") ?: "YOUR_API_KEY"
val merchantId = System.getenv("WEBIRR_TEST_ENV_MERCHANT_ID") ?: "YOUR_MERCHANT_ID"

fun getStatAsync() {
    val api = WeBirrClient(merchantId, apiKey, true)
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
```

## Standalone Example

The `src/main/kotlin/example/example.kt` file includes workflows equivalent to the README sections:

| Workflow | Coverage |
| --- | --- |
| `createAndUpdateBillAsync` | Create bill, save payment code, update same bill. |
| `getPaymentStatusAsync` | Single payment status by saved payment code. |
| `deleteBillAsync` | Delete unpaid bill by payment code. |
| `fetchAndProcessPayments` | Poll payments with `lastTimeStamp`, process each payment, save `updateTimeStamp`. |
| `getStatAsync` | Merchant stats by date range. |
| `processWebhookPayment` | Webhook callback processing helper. |
| `getBillAndListBillsAsync` | Get bill by reference, get bill by payment code, list bills. |
| `getSupportedBanksAsync` | Get banks enabled for the configured merchant checkout. |

## Tests

Fast tests use a mock web server:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home
bash ./gradlew test
```

Live TestEnv smoke tests call the running gateway when TestEnv credentials are available:

```bash
export WEBIRR_TEST_ENV_MERCHANT_ID="YOUR_MERCHANT_ID"
export WEBIRR_TEST_ENV_API_KEY="YOUR_API_KEY"
bash ./gradlew test
```
