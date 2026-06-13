package webirr

data class Bill(
    var customerCode: String,
    var customerName: String,
    var billReference: String,
    var time: String,
    var description: String,
    var amount: String,
    var merchantID: String = "",
    var customerPhone: String = "",
    var extras: Map<String, Any> = emptyMap()
)

data class BillResponse(
    var customerCode: String = "",
    var customerName: String = "",
    var billReference: String = "",
    var time: String = "",
    var description: String = "",
    var amount: String = "",
    var merchantID: String = "",
    var customerPhone: String = "",
    var extras: Map<String, Any> = emptyMap(),
    var wbcCode: String = "",
    var paymentStatus: Int = 0,
    var updateTimeStamp: String = ""
)
