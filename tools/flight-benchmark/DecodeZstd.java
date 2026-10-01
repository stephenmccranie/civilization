import java.util.Base64;
import java.util.Scanner;
import dhcomgithubluben.zstd.Zstd;

/** Uses the pinned DH runtime's codec; no additional Python packages required. */
class DecodeZstd {
    public static void main(String[] args) {
        var lines = new Scanner(System.in);
        while (lines.hasNextLine()) {
            byte[] compressed = Base64.getDecoder().decode(lines.nextLine());
            System.out.println(Base64.getEncoder().encodeToString(Zstd.decompress(compressed)));
        }
    }
}
