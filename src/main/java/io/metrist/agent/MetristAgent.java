package io.metrist.agent;

import static net.bytebuddy.matcher.ElementMatchers.hasMethodName;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import java.lang.instrument.Instrumentation;
import java.net.http.HttpResponse;

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
                      .and(returns(named("java.net.http.HttpResponse")))));
        })
        .asTerminalTransformation()
        .installOn(instrumentation);
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
      @Advice.Return HttpResponse<?> response) {
    System.out.printf("took %d",  System.nanoTime() - startTime);
  }
}