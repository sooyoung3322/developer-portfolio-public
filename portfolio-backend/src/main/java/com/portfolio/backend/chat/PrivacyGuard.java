package com.portfolio.backend.chat;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
@Component
public class PrivacyGuard {
 private static final Pattern SENSITIVE=Pattern.compile("(?i)([\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?:\\+?82[- .]?)?0?1[016789][- .]?\\d{3,4}[- .]?\\d{4}|\\d{6}[- ]?[1-4]\\d{6}|AIza[\\w-]{20,}|(?:api[_ -]?key|password|비밀번호|성명|이름|주소)\\s*[:=은는]|(?:서울|부산|경기|인천).{0,30}(?:로|길|동)\\s*\\d|\\bsk-[\\w-]{12,})");
 public boolean sensitive(String text) {
  // Match whole email tokens so the approved address cannot exempt a longer address.
  String filtered = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}").matcher(text)
   .replaceAll(match -> match.group().equals(PublicContact.EMAIL) ? "PUBLIC_CONTACT" : match.group());
  return SENSITIVE.matcher(filtered).find();
 }
 public boolean injection(String text) { return Pattern.compile("(?i)(system\\s*prompt|ignore.{0,30}instruction|시스템.{0,12}(프롬프트|지시)|이전.{0,12}무시|내부.{0,12}지시|api.{0,5}키.{0,10}(알려|출력))").matcher(text).find(); }
}
