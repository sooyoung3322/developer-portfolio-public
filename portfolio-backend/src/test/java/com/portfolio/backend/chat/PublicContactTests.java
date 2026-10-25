package com.portfolio.backend.chat;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PublicContactTests {
 @Test void onlyApprovedWholeAddressIsPublic() {
  var guard = new PrivacyGuard();
  assertThat(guard.sensitive("문의: " + PublicContact.EMAIL)).isFalse();
  assertThat(guard.sensitive("other@example.org")).isTrue();
  assertThat(guard.sensitive("prefix" + PublicContact.EMAIL)).isTrue();
  assertThat(guard.sensitive(PublicContact.EMAIL + ".private.org")).isTrue();
  assertThat(guard.sensitive(PublicContact.EMAIL + " 010-1234-5678")).isTrue();
  assertThat(Knowledge.publicContact().getContent()).contains(PublicContact.EMAIL);
 }
}
