package com.portfolio.backend.chat;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.ArrayDeque;
@Component
public class ChatSessionLimiter {
 private final Clock clock;
 public ChatSessionLimiter(Clock clock) {this.clock=clock;}
 public boolean allow(HttpSession session) {return check(session)==0;}
 @SuppressWarnings("unchecked") public int check(HttpSession session) {
  synchronized(session) {
   var times=(ArrayDeque<Long>)session.getAttribute("ai.window");if(times==null){times=new ArrayDeque<>();session.setAttribute("ai.window",times);}
   long now=clock.millis();while(!times.isEmpty() && times.getFirst()<=now-600000)times.removeFirst();
   if(times.size()>=5)return (int)((times.getFirst()+600000-now+999)/1000);times.addLast(now);return 0;
  }
 }
}
