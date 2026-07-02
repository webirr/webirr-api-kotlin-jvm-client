package webirr

import com.google.gson.annotations.SerializedName

class Payment {
    // 0 = not paid, 1 = payment in progress, 2 = paid.
    var status: Int = 0
    var data: PaymentDetail? = null

    // true if the bill is paid (payment process completed)
    val isPaid: Boolean
        get() = status == 2
}

class PaymentDetail {
    var id: Int = 0
    var status: Int = 0
    var paymentReference: String = ""
    var confirmed: Boolean = false
    var confirmedTime: String = ""
    var bankID: String = ""

    @SerializedName(value = "paymentDate", alternate = ["time"])
    var paymentDate: String = ""

    @Deprecated("Prefer paymentDate.")
    var time: String
        get() = paymentDate
        set(value) {
            paymentDate = value
        }

    var amount: String = ""
    var wbcCode: String = ""
    var updateTimeStamp: String = ""
}

class PaymentResponse {
    var status: Int = 0
    var id: Int = 0
    var bankID: String = ""
    var paymentReference: String = ""

    @SerializedName(value = "paymentDate", alternate = ["time"])
    var paymentDate: String = ""

    @Deprecated("Prefer paymentDate.")
    var time: String
        get() = paymentDate
        set(value) {
            paymentDate = value
        }

    var confirmed: Boolean = false
    var confirmedTime: String = ""
    var canceled: Boolean = false
    var canceledTime: String = ""
    var amount: String = ""
    var wbcCode: String = ""
    var updateTimeStamp: String = ""

    val isPaid: Boolean
        get() = status == 2

    val isReversed: Boolean
        get() = status == 3
}

class PaymentWebhookPayload {
    var status: Int = 0
    var data: PaymentResponse = PaymentResponse()
}
