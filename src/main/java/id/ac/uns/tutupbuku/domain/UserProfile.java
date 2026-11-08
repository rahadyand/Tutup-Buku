package id.ac.uns.tutupbuku.domain;

import java.util.Optional;

/** Identitas akun tidak tergantung semester yang sedang aktif. */
public record UserProfile(String nim, String name, String studyProgram, Optional<Long> activeSemesterId) { }
