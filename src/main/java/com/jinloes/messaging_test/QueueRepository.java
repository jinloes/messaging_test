package com.jinloes.messaging_test;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.util.postgres.PostgresDSL;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.jooq.impl.DSL.field;

public class QueueRepository {
  private DSLContext dslContext;

  public QueueRepository(DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  public Flux<Record> getValues() {
    return Mono.fromCompletionStage(
        dslContext.transactionResultAsync(
            configuration ->
                dslContext.select().from("test")
                    .orderBy(field("id").asc())
                    .forUpdate()
                    .skipLocked()
                    .fetch(record -> record))).flatMapIterable(val -> val);
  }

  public static void main(String[] args) throws InterruptedException {
    Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    DSLContext context = PostgresDSL.using("jdbc:postgresql://localhost/glx_manager_dev");
    QueueRepository repository = new QueueRepository(context);
    CountDownLatch latch = new CountDownLatch(2);

    new Thread(() -> {
      Connection connection = context.configuration().connectionProvider().acquire();
      Flux.create((Consumer<FluxSink<Double>>) fluxSink ->
          fluxSink.onRequest(value -> {
            LOGGER.info("Requesting {} values", value);
            fluxSink.next(Math.random());
          }))
          //.delayElements(Duration.ofMillis(10))
          .map(val -> {
            LOGGER.info("DOING SOMETHING WIL VAL.");
            return val;
          })
          //.delayElements(Duration.ofMillis(10))
          .subscribe(new Subscriber<Double>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
              s.request(1);
              subscription = s;
            }

            @Override
            public void onNext(Double val) {
              LOGGER.info("VALUE: {}", val);
              subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
          });
    })
        .start();
    /*new Thread(() -> repository.getValues()
        .subscribeOn(Schedulers.immediate())
        .delayElements(Duration.of(2, ChronoUnit.SECONDS))
        //.log()
        .doOnNext(val -> LOGGER.info("FIELD 2: {}", val.get("field_2")))
        .doOnComplete(latch::countDown)
        .subscribe())
        .start();

    new Thread(() -> repository.getValues()
        .subscribeOn(Schedulers.immediate())
        //.log()
        .doOnNext(val -> LOGGER.info("FIELD 2: {}", val.get("field_2")))
        .doOnComplete(latch::countDown)
        .subscribe()).start();*/

    latch.await(5, TimeUnit.SECONDS);
  }
}
