package com.portfolio.backend.chat;

import tools.jackson.databind.ObjectMapper;

/** Synthetic demo contact only; no private profile data. */
public final class PublicContact {
 private PublicContact() {}
 public static final String EMAIL = load();
 private static String load() {
  try (var stream = PublicContact.class.getResourceAsStream("/public-contact.json")) {
   if (stream == null) throw new IllegalStateException("Public contact resource missing");
   String email = new ObjectMapper().readTree(stream).path("email").asString();
   if (!email.matches("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    throw new IllegalStateException("Invalid public contact");
   return email;
  } catch (Exception error) { throw new IllegalStateException("Cannot load public contact", error); }
 }
}
