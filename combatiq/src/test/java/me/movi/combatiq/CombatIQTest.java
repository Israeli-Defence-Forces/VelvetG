package me.movi.combatiq;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import static me.movi.combatiq.CombatIQ.*;

/** Executable dependency-free regression suite. It does not simulate Minecraft physics. */
public final class CombatIQTest {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }
    private static Event e(long ns, long seq, int type, int stage, int flags) {
        Event e = new Event();
        e.ns = ns; e.sourceNs = UNKNOWN_SOURCE_TIME; e.sequence = seq;
        e.session = 1; e.producer = MAIN; e.type = type; e.stage = stage; e.flags = flags;
        return e;
    }
    private static Analyzer active() {
        Analyzer a = new Analyzer();
        a.accept(e(0, 1, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY | BINDING_DOWN));
        return a;
    }
    private static void sampling() {
        Analyzer a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(20, 3, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 1, "release entirely between samples");
        check(a.snapshot().sampledReleases == 0, "unsampled not double counted");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(15, 3, MOVEMENT_SAMPLE, SAMPLE, 0));
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().sampledReleases == 1, "consumed binding-up sample");
        check(a.snapshot().unsampledReleases == 0, "sampled release not called missed");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        Event sample = e(15, 3, MOVEMENT_SAMPLE, SAMPLE, BINDING_DOWN | MODIFIED_INPUT);
        sample.x = 0; a.accept(sample);
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 1, "zero forward motion is not a release sample");
        check(a.snapshot().modifiedSamples == 1, "modified input retained");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(11, 3, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        a.accept(e(21, 5, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 1, "repeat edges ignored");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, SYNTHETIC));
        a.accept(e(20, 3, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 0, "synthetic release not scored");
    }
    private static void phase3Checks() {
        Analyzer a = new Analyzer();
        a.accept(e(0, 1, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY));
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(20, 3, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 0,
                "initial unknown W state cannot count an intentional release");

        a = active();
        a.accept(e(500, 2, FRAME, SAMPLE, 0));
        Event old = e(400, 1, HURT, RECEIVE, 0);
        old.producer = INBOUND;
        a.accept(old);
        check(a.snapshot().observationNs == 500,
                "cross-producer display watermark does not regress");
        check(a.snapshot().complete, "cross-producer ordering is not a gap");

        Event invalid = e(1000, 1, HURT, INTENT, 0);
        check(!validEvent(invalid), "impossible stage rejected");
        Event acceptable = e(1001, 1, ATTACK, INTENT, 0);
        check(validEvent(acceptable), "normal attack intent permitted");
        Event received = e(1002, 1, HURT, RECEIVE, 0);
        received.producer = INBOUND;
        check(validEvent(received), "dedicated inbound lane accepted");

        String log = "# CombatIQ schema=2\\n" + HEADER + "\\n";
        StringWriter w = new StringWriter();
        try {
            w.write(log);
            Event context = e(0, 1, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY | BINDING_DOWN);
            writeEvent(w, context);
            writeEvent(w, invalid);
            w.write("# LOSS,0,0,0,0,0,0\\n# END\\n");
            boolean refused = false;
            try { replay(new StringReader(w.toString()), false); }
            catch (IOException ex) { refused = true; }
            check(refused, "schema2 replay refuses invalid producer-stage combination");
        } catch (IOException ex) { throw new AssertionError(ex); }
    }

    private static void boundaries() {
        Analyzer a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(15, 3, CONTEXT, SAMPLE, IN_WORLD));
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().censoredReleases == 1, "focus loss censors pending interval");
        check(a.snapshot().unsampledReleases == 0, "focus loss is not a missed tap");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        Event boundary = e(15, 3, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY | BINDING_DOWN);
        boundary.session = 2; a.accept(boundary);
        Event press = e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN); press.session = 2;
        a.accept(press);
        check(a.snapshot().censoredReleases == 1, "connection identity bounds a release");
        check(a.snapshot().unsampledReleases == 0, "no cross-connection pairing");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(!a.snapshot().complete, "missing sequence suspends verdicts");
        check(a.snapshot().unsampledReleases == 0, "gap cannot create a false miss");
        check(a.snapshot().censoredReleases == 1, "gap censors interval");

        a = active();
        a.accept(e(20, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(10, 3, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(!a.snapshot().complete, "same-producer backwards time invalidated");

        a = active();
        a.accept(e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0));
        a.accept(e(15, 3, CORRECTION, APPLY, 0));
        a.accept(e(20, 4, FORWARD_EDGE, INPUT_QUEUE, DOWN));
        check(a.snapshot().unsampledReleases == 0, "correction censors pairing");
    }
    private static void stages() {
        Analyzer a = active();
        a.accept(e(1, 2, LOCAL_SPRINT, SAMPLE, DOWN));
        Event n = e(1, 1, SPRINT_STOP, WRITE, 0); n.producer = NETWORK; a.accept(n);
        Snapshot s = a.snapshot();
        check(s.localSprintKnown && s.localSprint, "local sprint retained");
        check(s.writtenSprintKnown && !s.writtenSprint, "written sprint separately retained");
        a.accept(e(2, 3, ATTACK, INTENT, 0));
        n = e(2, 2, ATTACK, WRITE, 0); n.producer = NETWORK; a.accept(n);
        n = e(3, 1, HURT, RECEIVE, 0); n.producer = INBOUND; a.accept(n);
        a.accept(e(4, 4, HURT, APPLY, 0));
        n = e(4, 2, VELOCITY, RECEIVE, 0); n.producer = INBOUND; a.accept(n);
        n = e(5, 3, USE_START, WRITE, 0); n.producer = NETWORK; a.accept(n);
        s = a.snapshot();
        check(s.attackIntents == 1 && s.attackWrites == 1, "intent and write counted separately");
        check(s.hurtReceives == 1 && s.hurtApplies == 1, "receive and apply separated");
        check(s.velocityReceives == 1 && s.useWrites == 1, "velocity and use observed");
        check(s.confirmedHits == 0, "hurt plus velocity never confirms own hit");

        Event truth = e(6, 1, SERVER_ACCEPT, GROUND_TRUTH, 0); truth.producer = SERVER;
        a.accept(truth);
        check(a.snapshot().confirmedHits == 0, "untrusted trace cannot assert ground truth");
        Analyzer trusted = new Analyzer(true);
        trusted.accept(e(0, 1, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY));
        trusted.accept(truth);
        check(trusted.snapshot().confirmedHits == 1, "explicit private-server truth accepted");

        a = active();
        Event stale = e(1, 1, SPRINT_START, WRITE, 0);
        stale.producer = NETWORK; stale.session = 99; a.accept(stale);
        check(!a.snapshot().writtenSprintKnown, "stale connection packet excluded from state");
        a.accept(e(100, 2, FRAME, SAMPLE, 0));
        a.accept(e(105, 3, FRAME, SAMPLE, 0));
        check(a.snapshot().frameIntervalNs == 5, "wall frame interval independent of tick index");
    }
    private static boolean offer(Ring r, long at, int type, int flags) {
        return r.offer(at, UNKNOWN_SOURCE_TIME, 1, at, type, SAMPLE, 0, 0, flags, at, -at, at / 2.0);
    }
    private static void rings() throws Exception {
        boolean rejected = false;
        try { new Ring(3, MAIN); } catch (IllegalArgumentException ex) { rejected = true; }
        check(rejected, "non-power-of-two capacity rejected");
        Ring r = new Ring(2, MAIN);
        check(offer(r, 1, TICK, 0) && offer(r, 2, TICK, 0), "capacity usable");
        check(!offer(r, 3, TICK, 0) && r.dropped() == 1, "full buffer drops newest");
        Event out = new Event();
        check(r.poll(out) && out.sequence == 1 && out.x == 1, "first payload/order");
        check(offer(r, 4, TICK, 0), "slot reuse after drain");
        check(r.poll(out) && out.sequence == 2, "unconsumed entry retained");
        check(r.poll(out) && out.sequence == 4, "sequence exposes dropped offer");
        check(!r.poll(out), "empty ring");

        final Ring owned = new Ring(8, MAIN);
        offer(owned, 1, TICK, 0);
        Thread wrong = new Thread(new Runnable() {
            public void run() { offer(owned, 2, TICK, 0); }
        }); wrong.start(); wrong.join();
        check(owned.misuse() == 1, "second producer rejected rather than racing");

        final Ring concurrent = new Ring(256, MAIN);
        final int total = 100000;
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final long[] accepted = new long[1], read = new long[1];
        Thread producer = new Thread(new Runnable() {
            public void run() {
                try { for (int i = 1; i <= total; i++) if (offer(concurrent, i, TICK, 0)) accepted[0]++; }
                catch (Throwable ex) { failure.set(ex); }
            }
        });
        Thread consumer = new Thread(new Runnable() {
            public void run() {
                Event event = new Event(); long prior = 0;
                try {
                    while (producer.isAlive() || !concurrent.isEmpty()) {
                        if (concurrent.poll(event)) {
                            if (event.sequence <= prior || event.x != event.ns
                                    || event.y != -event.ns || event.z != event.ns / 2.0)
                                throw new AssertionError("torn or reordered event");
                            prior = event.sequence; read[0]++;
                        } else Thread.yield();
                    }
                } catch (Throwable ex) { failure.set(ex); }
            }
        });
        producer.start(); consumer.start(); producer.join(); consumer.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        check(accepted[0] == read[0], "all accepted concurrent records read");
        check(accepted[0] + concurrent.dropped() == total, "all offers accounted for");
    }
    private static String log(Event... events) throws IOException {
        StringWriter w = new StringWriter();
        w.write("# CombatIQ schema=1\n" + HEADER + "\n");
        for (Event e : events) writeEvent(w, e);
        w.write("# LOSS,0,0,0,0\n# END\n"); return w.toString();
    }
    private static void replayTests() throws Exception {
        Event context = e(0, 1, CONTEXT, SAMPLE, FOCUSED | IN_WORLD | GAMEPLAY | BINDING_DOWN);
        Event release = e(10, 2, FORWARD_EDGE, INPUT_QUEUE, 0);
        Event press = e(20, 3, FORWARD_EDGE, INPUT_QUEUE, DOWN);
        String text = log(context, release, press);
        Snapshot s = replay(new StringReader(text), false);
        check(s.complete && s.unsampledReleases == 1, "CSV round trip preserves analysis");
        check(!replay(new StringReader(text.replace("# LOSS,0,0,0,0", "# LOSS,1,0,0,0")), false)
                .complete, "tail loss survives replay");
        check(!replay(new StringReader(text.replace("# END\n", "")), false).complete,
                "truncated recording is incomplete");
        boolean bad = false;
        try { replay(new StringReader(text.replace("schema=1", "schema=99")), false); }
        catch (IOException ex) { bad = true; }
        check(bad, "unsupported schema refused");
        bad = false;
        try { replay(new StringReader(text + "extra\n"), false); } catch (IOException ex) { bad = true; }
        check(bad, "trailing data refused");
        String huge = text.replace("20,-922", "NaN,-922");
        bad = false;
        try { replay(new StringReader(huge), false); } catch (IOException ex) { bad = true; }
        check(bad, "invalid timestamp refused");
        check(!replay(new StringReader(text.replace("# LOSS,0,0,0,0\n", "")), false).complete,
                "missing loss trailer cannot masquerade as complete");
        bad = false;
        try { replay(new StringReader(text.replace("# END", "# LOSS,0,0,0,0\n# END")), false); }
        catch (IOException ex) { bad = true; }
        check(bad, "duplicate loss trailer refused");
        bad = false;
        StringWriter extra = new StringWriter(); writeEvent(extra, press);
        try { replay(new StringReader(text.replace("# END", extra.toString() + "# END")), false); }
        catch (IOException ex) { bad = true; }
        check(bad, "events after loss trailer refused");
        Snapshot pending = replay(new StringReader(log(context, release)), false);
        check(pending.complete && pending.censoredReleases == 1 && pending.unsampledReleases == 0,
                "unfinished release is censored at recording end");
    }
    private static void async() throws Exception {
        Path dir = Files.createTempDirectory("combatiq-test-");
        Path path = dir.resolve("trace.csv");
        Session session = new Session(path, 32);
        session.main.offer(0, UNKNOWN_SOURCE_TIME, 1, 0, CONTEXT, SAMPLE, 0, 0,
                FOCUSED | IN_WORLD | GAMEPLAY | BINDING_DOWN, 0, 0, 0);
        session.main.offer(10, UNKNOWN_SOURCE_TIME, 1, 0, FORWARD_EDGE, INPUT_QUEUE, 0, 0, 0, 0, 0, 0);
        session.main.offer(20, UNKNOWN_SOURCE_TIME, 1, 0, FORWARD_EDGE, INPUT_QUEUE, 0, 0, DOWN, 0, 0, 0);
        session.stop(); session.awaitStopped(5000);
        check(session.isStopped() && session.failure() == null, "async stop drains and closes");
        check(session.snapshot().unsampledReleases == 1, "live worker analysis");
        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Snapshot replayed = replay(in, false);
            check(replayed.complete && replayed.unsampledReleases == 1, "async export replay");
        }
        Session duplicate = new Session(path, 2);
        duplicate.stop();
        boolean refused = false;
        try { duplicate.awaitStopped(5000); } catch (IOException ex) { refused = true; }
        check(refused, "existing recording never overwritten");
        check(!offer(session.main, 100, TICK, 0), "stopped producer refuses capture");
        Files.delete(path); Files.delete(dir);
    }
    public static void main(String[] args) throws Exception {
        sampling(); boundaries(); stages(); rings(); replayTests(); async(); phase3Checks();
        System.out.println("PASS: " + assertions + " assertions; Java " + System.getProperty("java.version"));
        System.out.println("Scope: telemetry kernel, concurrency, replay and file lifecycle. No game/FPS validation.");
    }
}
