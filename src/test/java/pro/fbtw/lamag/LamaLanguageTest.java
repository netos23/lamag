package pro.fbtw.lamag;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class LamaLanguageTest {
    @Test
    public void arithmeticAndReadWrite() throws Exception {
        String program = "var x = read (), y = read (), z = x * y * 3; write (z)";
        assertEquals("> > 90\n", run(program, "5 6\n"));
    }

    @Test
    public void recursiveFunction() throws Exception {
        String program = ""
                + "fun fact (n) { if n == 0 then 1 else n * fact (n - 1) fi }\n"
                + "write (fact (5))";
        assertEquals("120\n", run(program, ""));
    }

    @Test
    public void arraysAndPostfixCalls() throws Exception {
        String program = "var a = [1, 2, 3]; a[1] := 7; write (a[1]); write (a.length)";
        assertEquals("7\n3\n", run(program, ""));
    }

    @Test
    public void casePatternsAndLists() throws Exception {
        String program = ""
                + "fun hd (l) { case l of h : _ -> h esac }\n"
                + "write ({4, 5, 6}.hd)";
        assertEquals("4\n", run(program, ""));
    }

    private static String run(String program, String input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Context context = Context.newBuilder(LamaLanguage.ID)
                .in(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .out(output)
                .build()) {
            context.eval(Source.newBuilder(LamaLanguage.ID, program, "test.lama").build());
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
