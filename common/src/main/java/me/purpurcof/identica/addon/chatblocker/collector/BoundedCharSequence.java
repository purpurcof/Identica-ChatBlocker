package me.purpurcof.identica.addon.chatblocker.collector;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link CharSequence} view that gives up after a fixed number of {@link #charAt(int)} calls.
 *
 * <p>Message templates are compiled to regular expressions with bounded lazy gaps ({@code .{0,64}?}).
 * Several of them in one expression backtrack exponentially on hostile input (a template with five
 * placeholders took ~7 s on a 240 character message), so every match runs against a step budget and
 * fails closed when it is exceeded.
 */
final class BoundedCharSequence implements CharSequence {

    /** Thrown when the step budget is used up. */
    static final class BudgetExceededException extends RuntimeException {
        BudgetExceededException() {
            super("regex step budget exceeded", null, false, false);
        }
    }

    private final CharSequence delegate;
    private final long maxSteps;
    private long steps;

    BoundedCharSequence(@NotNull CharSequence delegate, long maxSteps) {
        this.delegate = delegate;
        this.maxSteps = maxSteps;
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public char charAt(int index) {
        if (++steps > maxSteps) throw new BudgetExceededException();
        return delegate.charAt(index);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end) {
        return delegate.subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return delegate.toString();
    }
}