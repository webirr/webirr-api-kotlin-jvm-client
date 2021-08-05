import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import webirr.Bill
import webirr.WeBirrClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WeBirrClientTests {

    @Test
    fun createBill_should_get_error_from_WebService_on_invalid_api_key_TestEnv() {

        val bill = sampleBill
        val api = WeBirrClient("x", true);

        var errorCode: String? = null

        api.createBill(bill) {
            errorCode = it.errorCode
            lock.countDown()
        }

        lock.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(errorCode)

    }

    @Test
    fun createBill_should_get_error_from_WebService_on_invalid_api_key_ProdEnv() {

        val bill = sampleBill
        val api = WeBirrClient("x", false);

        var errorCode: String? = null

        api.createBill(bill) {
            errorCode = it.errorCode
            lock.countDown()
        }

        lock.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(errorCode)

    }

    @Test
    fun updateBill_should_get_error_from_WebService_on_invalid_api_key() {

        val bill = sampleBill
        val api = WeBirrClient("x", true);

        var errorCode: String? = null

        api.updateBill(bill) {
            errorCode = it.errorCode
            lock.countDown()
        }

        lock.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(errorCode)

    }

    private val sampleBill : Bill
        get() = Bill (
            "270.90",
            "cc01",
            "Elias Haileselassie",
            "2021-07-22 22:14",
            "hotel booking",
            "kt/2021/130",
            "x"
        )

    private val lock
        get() = CountDownLatch(1)

}