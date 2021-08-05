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
    implementation 'com.webirr:webirr:Tag'
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
    <groupId>com.webirr</groupId>
    <artifactId>webirr</artifactId>
    <version>Tag</version>
</dependency>
```

## Usage

The library needs to be configured with a *merchant Id* & *API key*. You can get it by contacting [webirr.com](https://webirr.com)

> You can use this library for production or test environments. you will need to set isTestEnv=true for test, and false for production apps when creating objects of class WeBirrClient

## Example

```kotlin
package webirr.example

import webirr.*;

val apiKey = "YOUR_API_KEY"
val merchantId = "YOUR_MERCHANT_ID"

//val apiKey =  System.getenv("wb_apikey_1") ?: ""
//val merchantId =  System.getenv("wb_merchid_1") ?: ""

fun main() {

    createAndUpdateBillAsync();  Thread.sleep(2000);
    getPaymentStatusAsync(); Thread.sleep(2000);
    deleteBillAsync()

}

/**
 * Creating a new Bill / Updating an existing Bill on WeBirr Servers
 */
fun createAndUpdateBillAsync(){

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

/**
 * Getting Payment status of an existing Bill from WeBirr Servers
 */
fun getPaymentStatusAsync(){

    val api = WeBirrClient(apiKey, true)

    var paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // such as '141 263 782';

    println("Getting Payment Status...")

    api.getPaymentStatusAsync(paymentCode) { it ->

        if (it.error == null) {
            // success
            if (it.res?.isPaid ?: false)
            {
                println("bill is paid");
                println("bill payment detail");
                println("Bank: ${it.res?.data?.bankID}");
                println("Bank Reference Number: ${it.res?.data?.paymentReference}");
                println("Amount Paid: ${it.res?.data?.amount}");
            }
            else
                println("bill is pending payment");

        } else {
            // fail
            println("error: ${it.error}");
            println("errorCode: ${it.errorCode}"); // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }
    }

}

fun deleteBillAsync(){

    val api = WeBirrClient(apiKey, true)

    var paymentCode = "PAYMENT_CODE_YOU_SAVED_AFTER_CREATING_A_NEW_BILL" // suchas as '141 263 782';

    println("Deleting Bill...")
    
    api.deleteBillAsync(paymentCode) { it ->

        if (it.error == null) {
            // success
            println("bill is deleted successfully"); //res.res will be 'OK'  no need to check here!
        } else {
            // fail
            println("error: ${it.error}");
            println("errorCode: ${it.errorCode}"); // can be used to handle specific busines error such as ERROR_INVLAID_INPUT
        }

    }
}

```



