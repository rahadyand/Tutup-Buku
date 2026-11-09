package id.ac.uns.tutupbuku.persistence;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Hanya PNG/JPEG terverifikasi, disimpan pada direktori pemilik dengan nama acak. */
public final class AssetStore {
    private final Path dataDirectory;
    public AssetStore(Path dataDirectory) { this.dataDirectory = dataDirectory.toAbsolutePath().normalize(); }
    private String ownerKey(String owner) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(owner.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private String prefix(String owner) { return "assets/" + ownerKey(owner) + "/"; }
    public String importImage(String owner, Path selected) {
        Path temporary = null;
        try {
            if (!Files.isRegularFile(selected) || Files.size(selected) > 10 * 1024 * 1024) throw new IOException("Pilih gambar PNG/JPEG paling besar 10 MB.");
            String extension;
            try (var input = ImageIO.createImageInputStream(selected.toFile())) {
                if (input == null) throw new IOException("Gambar tidak dapat dibaca.");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("File bukan gambar PNG/JPEG yang sah.");
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) throw new IOException("Hanya PNG/JPEG yang didukung.");
                    reader.setInput(input);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000) throw new IOException("Resolusi gambar terlalu besar (maksimum 16 juta piksel).");
                    reader.read(0); // Pastikan isi dapat didekode, bukan hanya ekstensi nama.
                    extension = format.equals("png") ? "png" : "jpg";
                } finally { reader.dispose(); }
            }
            String reference = prefix(owner) + UUID.randomUUID() + "." + extension;
            Path directory = dataDirectory.resolve(prefix(owner)); Files.createDirectories(directory);
            Path root = dataDirectory.toRealPath();
            if (!directory.toRealPath().startsWith(root.resolve(prefix(owner)).normalize())) throw new IOException("Lokasi aset tidak aman.");
            temporary = Files.createTempFile(directory, ".import-", ".tmp");
            Files.copy(selected, temporary, StandardCopyOption.REPLACE_EXISTING);
            Path destination = dataDirectory.resolve(reference);
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, destination); }
            return reference;
        } catch (IOException error) {
            throw new StorageException("Gambar gagal disalin. " + error.getMessage() + " Aset yang sudah ada tetap dipertahankan.", error);
        } finally {
            // Hanya file sementara milik operasi ini, bukan aset yang dirujuk materi.
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
    public Optional<Path> resolve(String owner, String reference) {
        if (reference == null || !reference.matches("assets/[a-f0-9]{64}/[a-f0-9-]{36}\\.(png|jpg)")) return Optional.empty();
        if (!reference.startsWith(prefix(owner))) return Optional.empty();
        try {
            Path expected = dataDirectory.resolve(prefix(owner)).normalize();
            Path path = dataDirectory.resolve(reference).normalize();
            if (!path.startsWith(expected) || !Files.isRegularFile(path)) return Optional.empty();
            Path root = dataDirectory.toRealPath();
            if (!path.toRealPath().startsWith(root.resolve(prefix(owner)).normalize())) return Optional.empty();
            return Optional.of(path);
        } catch (IOException error) { return Optional.empty(); }
    }
    public Optional<String> dataUri(String owner, String reference) {
        return resolve(owner, reference).map(path -> {
            try {
                if (Files.size(path) > 10 * 1024 * 1024) return null;
                return "data:image/" + (reference.endsWith(".png") ? "png" : "jpeg") + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
            } catch (IOException error) { return null; }
        });
    }
}
