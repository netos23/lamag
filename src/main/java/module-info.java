module pro.fbtw.lamag {
    requires java.base;
    requires java.logging;
    requires jdk.unsupported;
    requires org.antlr.antlr4.runtime;
    requires org.graalvm.polyglot;
    requires org.graalvm.truffle;

    exports pro.fbtw.lamag;

    provides com.oracle.truffle.api.provider.TruffleLanguageProvider
            with pro.fbtw.lamag.LamaLanguageProvider;
}
