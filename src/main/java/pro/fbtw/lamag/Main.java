package pro.fbtw.lamag;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.File;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: lama <file.lama>");
            System.exit(2);
        }
        Source source = Source.newBuilder(LamaLanguage.ID, new File(args[0])).build();
        try (Context context = Context.newBuilder(LamaLanguage.ID).in(System.in).out(System.out).build()) {
            context.eval(source);
        }
    }
}
