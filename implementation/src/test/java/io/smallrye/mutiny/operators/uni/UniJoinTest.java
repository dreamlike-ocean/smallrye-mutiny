package io.smallrye.mutiny.operators.uni;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.spies.UniOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.UniOnItemSpy;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

class UniJoinTest {

    @Nested
    @SuppressWarnings("ResultOfMethodCallIgnored")
    class Nulls {

        @Test
        void allNull() {
            assertThatThrownBy(() -> Uni.join().all((Uni<Object>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` contains a `null` value");

            assertThatThrownBy(() -> Uni.join().all((List<Uni<Object>>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` must not be `null`");
        }

        @Test
        void firstNull() {
            assertThatThrownBy(() -> Uni.join().first((Uni<Object>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` contains a `null` value");

            assertThatThrownBy(() -> Uni.join().first((List<Uni<Object>>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` must not be `null`");
        }

        @Test
        void oneIsNull() {
            List<Uni<Object>> unis = Arrays.asList(Uni.createFrom().item(1), null, Uni.createFrom().item("3"));

            assertThatThrownBy(() -> Uni.join().all(unis))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` contains a `null` value");

            assertThatThrownBy(() -> Uni.join().first(unis))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`unis` contains a `null` value");
        }
    }

    @Nested
    @SuppressWarnings("ResultOfMethodCallIgnored")
    class Empty {
        @Test
        void emptyArrays() {
            assertThatThrownBy(() -> Uni.join().all(new Uni[0]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> assertThat(((Throwable) e).getMessage()).contains("empty"));

            assertThatThrownBy(() -> Uni.join().first(new Uni[0]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> assertThat(((Throwable) e).getMessage()).contains("empty"));
        }

        @Test
        void emptyLists() {
            assertThatThrownBy(() -> Uni.join().all(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> assertThat(((Throwable) e).getMessage()).contains("empty"));

            assertThatThrownBy(() -> Uni.join().first(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> assertThat(((Throwable) e).getMessage()).contains("empty"));
        }
    }

    @Nested
    class JoinAll {

        @Test
        void joinItems() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andCollectFailures();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2, 3));
        }

        @Test
        void joinItemsAndFailFast() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andFailFast();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2, 3));
        }

        @Test
        void joinBuilderCollectFailures() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            UniJoin.Builder<Integer> builder = Uni.join().builder();
            builder.add(a).add(b).add(c);
            Uni<List<Integer>> uni = builder.joinAll().andCollectFailures();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2, 3));
        }

        @Test
        void joinBuilderFailFast() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            UniJoin.Builder<Integer> builder = Uni.join().builder();
            builder.add(a).add(b).add(c);
            Uni<List<Integer>> uni = builder.joinAll().andFailFast();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2, 3));
        }

        @Test
        void joinNumericTypesItems() {
            Uni<Number> a = Uni.createFrom().item(1);
            Uni<Number> b = Uni.createFrom().item(2L);
            Uni<Number> c = Uni.createFrom().item(3);

            Uni<List<Number>> uni = Uni.join().all(a, b, c).andCollectFailures();

            UniAssertSubscriber<List<Number>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2L, 3));
        }

        @Test
        void joinDisparateTypesItems() {
            Uni<Object> a = Uni.createFrom().item(1);
            Uni<Object> b = Uni.createFrom().item("2");
            Uni<Object> c = Uni.createFrom().item(3L);

            Uni<List<Object>> uni = Uni.join().all(a, b, c).andCollectFailures();

            UniAssertSubscriber<List<Object>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, "2", 3L));
        }

        @Test
        void joinWithOneFailedItemAndCollectFailures() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andCollectFailures();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(CompositeException.class);
            CompositeException failures = (CompositeException) sub.getFailure();
            assertThat(failures.getCauses())
                    .hasSize(1)
                    .allMatch(err -> err instanceof IOException)
                    .anyMatch(err -> err.getMessage().equals("boom"));
        }

        @Test
        void joinWithTwoFailedItemsAndCollectFailures() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().failure(new RuntimeException("bam"));

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andCollectFailures();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(CompositeException.class);
            CompositeException failures = (CompositeException) sub.getFailure();
            assertThat(failures.getCauses()).hasSize(2);
            assertThat(failures.getCauses()).element(0, as(THROWABLE))
                    .isInstanceOf(IOException.class)
                    .hasMessage("boom");
            assertThat(failures.getCauses()).element(1, as(THROWABLE))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("bam");
        }

        @Test
        void joinWithOneFailedItemAndFailFast() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andFailFast();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        void joinWithTwoFailedItemsAndFailFast() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().failure(new RuntimeException("bam"));

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andFailFast();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        void earlyCancellation() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c).andCollectFailures();
            UniOnItemSpy<List<Integer>> spy = Spy.onItem(uni);

            UniAssertSubscriber<List<Integer>> sub = new UniAssertSubscriber<>(true);
            spy.subscribe().withSubscriber(sub);
            sub.assertNotTerminated();
            assertThat(spy.invocationCount()).isEqualTo(0L);
        }

        @Test
        void lateCancellation() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().emitter(e -> {
                // Do nothing
            });
            Uni<Integer> c = Uni.createFrom().item(3);

            UniOnCancellationSpy<Integer> sa = Spy.onCancellation(a);
            UniOnCancellationSpy<Integer> sb = Spy.onCancellation(b);
            UniOnCancellationSpy<Integer> sc = Spy.onCancellation(c);

            Uni<List<Integer>> uni = Uni.join().all(sa, sb, sc).andCollectFailures();

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(0L);
            assertThat(sb.invocationCount()).isEqualTo(0L);
            assertThat(sc.invocationCount()).isEqualTo(0L);

            sub.cancel();
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(0L);
            assertThat(sb.invocationCount()).isEqualTo(1L);
            assertThat(sc.invocationCount()).isEqualTo(0L);
        }
    }

    @Nested
    class JoinFirst {

        @Test
        void joinItems() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<Integer> uni = Uni.join().first(a, b, c).toTerminate();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(1);
        }

        @Test
        void joinItemsWithItem() {
            Uni<Integer> a = Uni.createFrom().failure(new IOException("boom #1"));
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom #2"));
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<Integer> uni = Uni.join().first(a, b, c).withItem();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(3);
        }

        @Test
        void joinItemsWithItemBuilder() {
            Uni<Integer> a = Uni.createFrom().failure(new IOException("boom #1"));
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom #2"));
            Uni<Integer> c = Uni.createFrom().item(3);

            UniJoin.Builder<Integer> builder = Uni.join().builder();
            builder.add(a).add(b).add(c);
            Uni<Integer> uni = builder.joinFirst().withItem();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(3);
        }

        @Test
        void joinItemsWithItemAndFailure() {
            Uni<Integer> a = Uni.createFrom().failure(new IOException("boom #1"));
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom #2"));
            Uni<Integer> c = Uni.createFrom().failure(new IOException("boom #3"));

            Uni<Integer> uni = Uni.join().first(a, b, c).withItem();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(CompositeException.class);
            CompositeException failures = (CompositeException) sub.getFailure();
            assertThat(failures.getCauses())
                    .hasSize(3)
                    .allMatch(err -> err instanceof IOException)
                    .anyMatch(err -> err.getMessage().equals("boom #1"))
                    .anyMatch(err -> err.getMessage().equals("boom #2"))
                    .anyMatch(err -> err.getMessage().equals("boom #3"));
        }

        @Test
        void joinBuilder() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            UniJoin.Builder<Integer> builder = Uni.join().builder();
            builder.add(a).add(b).add(c);
            Uni<Integer> uni = builder.joinFirst().toTerminate();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(1);
        }

        @Test
        void joinOne() {
            Uni<Integer> a = Uni.createFrom().emitter(emitter -> {
                // Do nothing
            });
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().emitter(emitter -> {
                // Do nothing
            });

            Uni<Integer> uni = Uni.join().first(a, b, c).toTerminate();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(2);
        }

        @Test
        void joinFailure() {
            Uni<Integer> a = Uni.createFrom().emitter(emitter -> {
                // Do nothing
            });
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().emitter(emitter -> {
                // Do nothing
            });

            Uni<Integer> uni = Uni.join().first(a, b, c).toTerminate();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        void earlyCancellation() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<Integer> uni = Uni.join().first(a, b, c).toTerminate();
            UniOnItemSpy<Integer> spy = Spy.onItem(uni);

            UniAssertSubscriber<Integer> sub = new UniAssertSubscriber<>(true);
            spy.subscribe().withSubscriber(sub);
            sub.assertNotTerminated();
            assertThat(spy.invocationCount()).isEqualTo(0L);
        }

        @Test
        void lateCancellation() {
            Uni<Integer> a = Uni.createFrom().emitter(e -> {
                // Do nothing
            });
            Uni<Integer> b = Uni.createFrom().emitter(e -> {
                // Do nothing
            });
            Uni<Integer> c = Uni.createFrom().emitter(e -> {
                // Do nothing
            });
            ;

            UniOnCancellationSpy<Integer> sa = Spy.onCancellation(a);
            UniOnCancellationSpy<Integer> sb = Spy.onCancellation(b);
            UniOnCancellationSpy<Integer> sc = Spy.onCancellation(c);

            Uni<Integer> uni = Uni.join().first(sa, sb, sc).toTerminate();

            UniAssertSubscriber<Integer> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(0L);
            assertThat(sb.invocationCount()).isEqualTo(0L);
            assertThat(sc.invocationCount()).isEqualTo(0L);

            sub.cancel();
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(1L);
            assertThat(sb.invocationCount()).isEqualTo(1L);
            assertThat(sc.invocationCount()).isEqualTo(1L);
        }
    }
}
