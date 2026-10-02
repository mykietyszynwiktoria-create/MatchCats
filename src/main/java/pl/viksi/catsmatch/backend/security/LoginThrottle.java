package pl.viksi.catsmatch.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.time.Clock;
import java.util.*;

@Service
public class LoginThrottle {
    private static final long WINDOW=600_000;
    private final Clock clock;
    private final Map<String,Attempt> attempts=new LinkedHashMap<>();
    private record Attempt(int count,long expires) {}
    @Autowired public LoginThrottle() { this(Clock.systemUTC()); }
    public LoginThrottle(Clock clock) { this.clock=clock; }
    private String key(String address,String username) { return "user:"+address+":"+username.toLowerCase(Locale.ROOT); }
    public synchronized void check(String address,String username) {
        long now=clock.millis();attempts.entrySet().removeIf(e->e.getValue().expires<=now);
        String user=key(address,username), ip="ip:"+address;
        if(attempts.getOrDefault(user,new Attempt(0,0)).count>=10 || attempts.getOrDefault(ip,new Attempt(0,0)).count>=100)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"TOO_MANY_ATTEMPTS","Wait before trying to sign in again");
        increment(user,now);increment(ip,now);
        while(attempts.size()>5000)attempts.remove(attempts.keySet().iterator().next());
    }
    private void increment(String key,long now) {
        Attempt current=attempts.get(key);attempts.put(key,new Attempt(current==null?1:current.count+1,current==null?now+WINDOW:current.expires));
    }
    public synchronized void success(String address,String username) { attempts.remove(key(address,username)); }
}
