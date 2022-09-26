package io.metrist.agent;

import static net.bytebuddy.matcher.ElementMatchers.hasMethodName;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Collections;
import java.util.logging.Logger;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Listener;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;

public class MetristAgent {
  public static void premain(String arguments, Instrumentation instrumentation) throws IOException {
    injectHelpers(instrumentation);

    new AgentBuilder.Default()
        .ignore(none())
        .with(Listener.StreamWriting.toSystemOut().withErrorsOnly())
        .type(nameStartsWith("java.net.")
            .or(nameStartsWith("jdk.internal."))
            .and(not(named("jdk.internal.net.http.HttpClientFacade")))
            .and(hasSuperType(named("java.net.http.HttpClient"))))
        .transform((builder, typeDescription, classLoader,
            module, protectionDomain) -> {
          return builder
              .visit(Advice.to(SendAdvice.class)
                  .on(hasMethodName("send")
                      .and(isPublic())
                      .and(takesArguments(2))
                      .and(takesArgument(0, named("java.net.http.HttpRequest")))));
        })
        .asTerminalTransformation()
        .installOn(instrumentation);
  }

  private static void injectHelpers(Instrumentation instrumentation) throws IOException {
    File temp = createTempDir();
    try {
      ClassInjector.UsingInstrumentation.of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
          .inject(Collections.singletonMap(
              new TypeDescription.ForLoadedType(Transport.class),
              ClassFileLocator.ForClassLoader.read(Transport.class)));
    } finally {
      deleteTempDir(temp);
    }
  }

  private static File createTempDir() throws IOException {
    return Files.createTempDirectory("metrist-temp-jars").toFile();
  }

  private static void deleteTempDir(File file) {
    // Not using Files.delete for deleting the directory because failures
    // create Exceptions which may prove expensive. Instead using the
    // older File API which simply returns a boolean.
    boolean deleted = file.delete();
    if (!deleted) {
      file.deleteOnExit();
    }
  }
}

class SendAdvice {
  @Advice.OnMethodEnter()
  public static long start() {
    return System.nanoTime();
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void exit(
      @Advice.Enter long startTime,
      @Advice.Return HttpResponse<?> response) throws MalformedURLException {
    HttpRequest request = response.request();
    URL url = request.uri().toURL();
    String message = new StringBuilder("0\t")
        .append(request.method())
        .append("\t")
        .append(url.getHost())
        .append("\t")
        .append(url.getPath())
        .append("\t")
        .append(System.nanoTime() - startTime)
        .toString();
    Transport.getInstance().send(message);
  }
}