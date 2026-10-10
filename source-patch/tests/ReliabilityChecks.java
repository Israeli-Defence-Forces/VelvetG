import me.movi.violet.VioletEngine;
import me.movi.violet.VioletDiagnostics;
import me.movi.violet.SoupConfirmation;
import me.movi.violet.BlockFilter;

public final class ReliabilityChecks {
    private static int checks;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
    public static void main(String[] args) {
        VioletEngine e = new VioletEngine(() -> .5);
        e.physical(0, 0); e.generated(1, 0, 4);
        check(e.physicalCps(0, 999999999L) == 1, "Physical edge included before one second");
        check(e.generatedCps(1, 1000000000L) == 0, "Generated edge expires at one second");
        check(e.averageCps(1000000000L) == 1, "Fixed-window average retains old events");
        check(e.averageCps(5000000000L) == 0, "Five-second boundary expires events");
        e.clear();
        for (int i = 0; i < 10000; i++) e.physical(0, i);
        check(e.physicalCps(0, 10000) == 512, "Physical flood has fixed storage");
        e.generated(1, 10000, Integer.MAX_VALUE);
        check(e.generatedCps(1, 10000) == 512, "Invalid generated count cannot cause unbounded work");
        e.generated(1, 10000, -100);
        check(e.generatedCps(1, 10000) == 512, "Negative generated count is ignored");
        e.clear();
        for (int i = 0; i < 300; i++) { long t = i * 16666667L; e.generated(0, t, 1); e.generated(1, t, 1); }
        check(e.averageCps(4999999999L) == 120, "Both hands retain five seconds at maximum supported rate");
        e.clear(); check(e.averageCps(4999999999L) == 0, "World-session reset clears average");
        VioletDiagnostics.enabled(false); VioletDiagnostics.physical(0); VioletDiagnostics.generated(4, 0);
        check(VioletDiagnostics.report().contains("physical=0 generated=0"), "Diagnostics off does no recording");
        VioletDiagnostics.enabled(true);
        for (int i = 0; i < 10000; i++) VioletDiagnostics.physical(i);
        check(VioletDiagnostics.report().split("\n").length == 130, "Diagnostic event storage remains bounded");
        VioletDiagnostics.generated(3, 10001); VioletDiagnostics.tick(120); VioletDiagnostics.tick(80); VioletDiagnostics.render(200);
        String report = VioletDiagnostics.report();
        check(report.contains("generated=3") && report.contains("meanTickNs=100") && report.contains("maxRenderNs=200"), "Origin and timing counters reflect measurements");
        VioletDiagnostics.enabled(false);
        check(!VioletDiagnostics.report().contains("9999 physical") && VioletDiagnostics.report().contains("physical=0"), "Disabling removes retained events and counters");
        SoupConfirmation soup = new SoupConfirmation(); Object stack = new Object();
        check(soup.allows(2, stack, 1, 10), "First soup use allowed");
        soup.attempted(2, stack, 1, 10);
        check(!soup.allows(2, stack, 1, 10), "No duplicate use without confirmation");
        check(!soup.allows(2, stack, 1, 8), "Taking damage does not confirm consumption");
        check(soup.allows(2, stack, 1, 14), "Healing confirms instant soup");
        soup.attempted(2, stack, 2, 10);
        check(soup.allows(2, stack, 1, 10), "In-place count change confirms inventory update");
        soup.attempted(2, stack, 1, 10);
        check(soup.allows(2, new Object(), 1, 10), "Replacement stack invalidates pending identity");
        soup.attempted(2, stack, 1, 10); soup.clear();
        check(soup.allows(2, stack, 1, 10), "Disable/world transition permits new session");
        BlockFilter filter = new BlockFilter();
        check(filter.allows("minecraft:dirt",0,""), "All-block mode preserves historical behavior");
        check(filter.allows("minecraft:stone",1,"stone, minecraft:obsidian"), "Bare and namespaced whitelist identifiers");
        check(!filter.allows("minecraft:dirt",1,"stone, minecraft:obsidian"), "Whitelist excludes unlisted blocks");
        check(!filter.allows("minecraft:obsidian",2,"obsidian"), "Blacklist excludes matching blocks");
        check(filter.allows("minecraft:dirt",2,"obsidian"), "Blacklist permits other blocks");
        check(!filter.allows("minecraft:stone",1,"invalid block"), "Invalid whitelist fails closed");
        System.out.println("PASS: " + checks + " bounded CPS, rolling average, diagnostics and soup-confirmation checks");
    }
}
