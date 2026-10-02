package pl.viksi.catsmatch.backend;

import org.junit.jupiter.api.Test;
import pl.viksi.catsmatch.backend.security.LoginThrottle;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LoginThrottleTests {
    static class MutableClock extends Clock {
        long millis;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return Instant.ofEpochMilli(millis);}
    }
    @Test void failedAttemptsAreBoundedAndExpire() {
        var clock=new MutableClock();var throttle=new LoginThrottle(clock);
        for(int i=0;i<10;i++)throttle.check("127.0.0.1","Alice");
        var error=assertThrows(ApiException.class,()->throttle.check("127.0.0.1","alice"));assertEquals(429,error.status.value());
        clock.millis=600_001;assertDoesNotThrow(()->throttle.check("127.0.0.1","alice"));
    }
    @Test void successfulLoginClearsUserFailuresButNotAddressLimit() {
        var throttle=new LoginThrottle(new MutableClock());
        for(int i=0;i<10;i++)throttle.check("127.0.0.1","alice");
        throttle.success("127.0.0.1","ALICE");assertDoesNotThrow(()->throttle.check("127.0.0.1","alice"));
        for(int i=11;i<100;i++){throttle.check("127.0.0.1","user"+i);throttle.success("127.0.0.1","user"+i);}
        assertThrows(ApiException.class,()->throttle.check("127.0.0.1","newname"));
        assertDoesNotThrow(()->throttle.check("127.0.0.2","newname"));
    }
}
