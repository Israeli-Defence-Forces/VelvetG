package me.movi.violet;

/** Observe a real inventory/health change before retrying an instant-soup stack. */
public final class SoupConfirmation {
    private Object stack;
    private int slot = -1, count;
    private float health;

    public void attempted(int selected, Object identity, int quantity, float hp) {
        slot = selected; stack = identity; count = quantity; health = hp;
    }
    public boolean allows(int selected, Object identity, int quantity, float hp) {
        if (stack == null) return true;
        if (slot != selected || stack != identity || count != quantity || hp > health + .001f) {
            clear(); return true;
        }
        // Damage alone is not evidence of consumption. No inventory transaction is invented.
        return false;
    }
    public void clear() { stack = null; slot = -1; count = 0; health = 0; }
}
