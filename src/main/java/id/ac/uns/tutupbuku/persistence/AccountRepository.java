package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.security.PasswordHash;
import java.util.Optional;

public interface AccountRepository {
    Optional<StoredAccount> findAccount(String nim);
    UserProfile getProfile(String nim);
    void createAccount(String nim, String name, PasswordHash hash);
    void updateProfile(String nim, String name, String studyProgram);
}
