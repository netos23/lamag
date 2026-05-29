package pro.fbtw.lamag;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.io.IOException;

public final class Main {

    private static final long STACK_SIZE = 512L * 1024 * 1024;

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.err.println("usage: lamag <file.lama>");
            System.exit(2);
            return;
        }

        int[] exitCode = {0};
        Thread worker = new Thread(null, () -> exitCode[0] = run(args[0]), "lama-main", STACK_SIZE);
        worker.start();
        worker.join();
        System.exit(exitCode[0]);
    }

    private static int run(String path) {
        Source source;
        try {
            source = Source.newBuilder(LamaLanguage.ID, new File(path)).build();
        } catch (IOException e) {
            System.err.println("lamag: cannot read " + path + ": " + e.getMessage());
            return 1;
        }

        try (Context context = Context.newBuilder(LamaLanguage.ID)
                .in(System.in)
                .out(System.out)
                .err(System.err)
                .allowAllAccess(true)
                .build()) {
            context.eval(source);
            return 0;
        } catch (PolyglotException ex) {
            if (ex.isInternalError()) {
                ex.printStackTrace();
            } else {
                System.err.println(ex.getMessage());
            }
            return 1;
        }
    }
}
