package webirr

class Payment {

    // 0 = not paid, 1 = payment in progress,  2. paid !
    var status : Int = 0
    var data : PaymentDetail? = null

    // true if the bill is paid (payment process completed)
    val isPaid : Boolean
      get() = status == 2;

}

class PaymentDetail {
    var id : Int = 0;
    var paymentReference : String = ""
    var confirmed : Boolean = false
    var confirmedTime : String = ""
    var bankID : String = ""
    var time : String = ""
    var amount : String = ""
    var wbcCode : String = ""
}