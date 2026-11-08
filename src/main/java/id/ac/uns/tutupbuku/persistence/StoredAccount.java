package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.security.PasswordHash;

public record StoredAccount(UserProfile profile, PasswordHash passwordHash) {
    @Override public String toString() { return "StoredAccount[redacted]"; }
}
