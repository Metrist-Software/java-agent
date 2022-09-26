package io.metrist.agent;

import static net.bytebuddy.matcher.ElementMatchers.hasMethodName;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.instrument.Instrumentation;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;;

public class MetristAgent {
  public static void premain(String arguments, Instrumentation instrumentation) {
    new AgentBuilder.Default()
        .ignore(ElementMatchers.none())
        .type(hasSuperType(named("java.net.http.HttpClient")))
        .transform((builder, typeDescription, classLoader,
            module, protectionDomain) -> {
          return builder
              .visit(Advice.to(SendAdvice.class)
                  .on(hasMethodName("send")
                      .and(returns(named("java.net.http.HttpResponse")))))
              .visit(Advice.to(SendAsyncAdvice.class)
                  .on(hasMethodName("sendAsync")
                      .and(returns(named("java.util.concurrent.CompletableFuture")))
                      .and(takesArguments(3))));
        })
        .asTerminalTransformation()
        .installOn(instrumentation);
  }
}

class SendAdvice {
  @Advice.OnMethodEnter
  public static long start() {
    return System.nanoTime();
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void exit(
      @Advice.Enter long startTime,
      @Advice.Return HttpResponse<?> response) {
    System.out.printf("took %d", System.nanoTime() - startTime);
  }
}

class SendAsyncAdvice {
  @Advice.OnMethodExit()
  public static void exit(
      @Advice.Return(readOnly = false) CompletableFuture<HttpResponse<?>> future) {
    long startTime = System.nanoTime();
    future = future.whenComplete((arg0, arg1) -> {
      System.out.printf("took %d", System.nanoTime() - startTime);
    });
  }
}