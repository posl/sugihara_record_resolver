import reactor.core.publisher.Mono;
import utils.BigFraction;
import utils.BigFractionUtils;

import java.math.BigInteger;

import static utils.BigFractionUtils.*;

/**
 * This class shows how to apply Project Reactor features
 * synchronously to reduce and display BigFractions via basic Mono
 * operations, including just(), fromCallable(), map(), doOnSuccess(),
 * and then().
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class MonoEx {
    /**
     * Create a new unreduced big fraction.
     */
    private static final BigFraction sUnreducedFraction = BigFraction
        .valueOf(new BigInteger(sBI1),
                 new BigInteger(sBI2),
                 false);

    /**
     * Test synchronous BigFraction reduction using a mono and a
     * pipeline of operations that run on the calling thread.
     */
    public static Mono<Void> testFractionReductionSync1() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionReductionSync1()\n");

        return Mono
            // Use just() to begin synchronously reducing a big
            // fraction in the calling thread.
            .just(BigFraction
                  // Reduce the BigFraction.
                  .reduce(sUnreducedFraction))

            // Use doOnSuccess() to print the BigFraction. If
            // something goes wrong doOnSuccess() will be skipped.
            .doOnSuccess(bigFraction -> sb
                         .append("     unreducedFraction "
                                 + sUnreducedFraction.toString()
                                 + "\n     reduced improper fraction = "
                                 + bigFraction.toString()
                                 + "\n     calling BigFraction::toMixedString\n"))

            // After big fraction is reduced return a mono and use
            // map() to call a function that converts the reduced
            // fraction to a mixed fraction string.
            .map(BigFraction::toMixedString)

            // Use doOnSuccess() to print the result after it's been
            // successfully converted to a mixed fraction.  If an
            // exception is thrown doOnSuccess() will be skipped.
            .doOnSuccess(result -> {
                    sb.append("     mixed reduced fraction = " + result + "\n");
                    // Display the result.
                    BigFractionUtils.display(sb.toString());
                })

            // Return an empty mono to synchronize with the
            // AsyncTaskBarrier framework.
            .then();
    }

    /**
     * Test synchronous BigFraction reduction using a mono and a
     * pipeline of operations that run on the calling thread.
     * Combines mono with Java functional programming features.
     */
    public static Mono<Void> testFractionReductionSync2() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionReductionSync2()\n");

        return Mono
            // Use fromCallable() to begin synchronously reducing a
            // big fraction in the calling thread.
            .fromCallable(() -> BigFraction
                          // Reduce the BigFraction.
                          .reduce(sUnreducedFraction))

            // Use doOnSuccess() to log the BigFraction. If something
            // goes wrong doOnSuccess() will be skipped.
            .doOnSuccess(bf -> 
                         logBigFraction(sUnreducedFraction, bf, sb))

            // After big fraction is reduced return a mono and use
            // map() to call a function that converts the reduced
            // fraction to a mixed fraction string.
            .map(BigFraction::toMixedString)

            // Use doOnSuccess() to print the result after it's been
            // successfully converted to a mixed fraction.  If
            // something goes wrong doOnSuccess() will be skipped.
            .doOnSuccess(bf -> displayMixedBigFraction(bf, sb))

            // Return an empty mono to synchronize with the
            // AsyncTaskBarrier framework.
            .then();
    }
}
