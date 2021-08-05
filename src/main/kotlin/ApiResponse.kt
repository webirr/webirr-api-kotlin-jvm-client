package webirr;

class ApiResponse<T>()
{
    var error: String? = null
    var errorCode: String? = null
    var res : T? = null

    constructor(error: String?):this() {
        this.error = error;
    }
}