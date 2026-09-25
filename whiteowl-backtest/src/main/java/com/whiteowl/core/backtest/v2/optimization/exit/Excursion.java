package com.whiteowl.core.backtest.v2.optimization.exit;

import com.whiteowl.core.backtest.v2.engine.Side;

/**
 * MAE/MFE for one closed trade, normalized by the ATR at the entry bar.
 *
 * <ul>
 *   <li>{@code maePrice}/{@code mfePrice} — price excursions following the spec
 *       convention: LONG → {@code min(low) − entry} / {@code max(high) − entry};
 *       SHORT mirrored. MAE is ≤ 0 for adverse moves.</li>
 *   <li>{@code maeAtr}/{@code mfeAtr} — excursion magnitudes in ATR multiples
 *       (always ≥ 0).</li>
 *   <li>{@code maeBar}/{@code mfeBar} — bar index of the extreme, counting the
 *       entry bar as 0.</li>
 * </ul>
 */
public record Excursion(
        int positionId,
        String scripId,
        Side side,
        float entryPrice,
        int entryBarIndex,
        int exitBarIndex,
        float entryAtr,
        float maePrice,
        float mfePrice,
        float maeAtr,
        float mfeAtr,
        int maeBar,
        int mfeBar) {
}
