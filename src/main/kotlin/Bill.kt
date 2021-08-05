package webirr

data class Bill (
    var customerCode : String,
    var customerName : String,
    var billReference : String,
    var time : String,
    var description : String,
    var amount : String,
    var merchantID : String,
)