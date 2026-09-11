package com.whiteowl.workbench.collection;

import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class ActiveTradesResolver {

    private static final String HOLDINGS_LABEL = "Holdings";
    private static final String POSITIONS_LABEL = "Positions";

    private final ActiveAccountManager activeAccountManager;

    public List<String> getAvailableNames() {
        if (!activeAccountManager.isConnected()) return List.of();
        return List.of(HOLDINGS_LABEL, POSITIONS_LABEL);
    }

    public Set<String> resolve(Set<String> selectedNames) {
        return activeAccountManager.getActiveAdapter()
                .map(adapter -> resolveFromAdapter(adapter, selectedNames))
                .orElseGet(Set::of);
    }

    private Set<String> resolveFromAdapter(BrokerAdapter adapter, Set<String> selectedNames) {
        boolean includeAll = selectedNames.isEmpty();
        Set<String> ids = new HashSet<>();
        if (includeAll || selectedNames.contains(HOLDINGS_LABEL)) {
            collectHoldings(adapter, ids);
        }
        if (includeAll || selectedNames.contains(POSITIONS_LABEL)) {
            collectPositions(adapter, ids);
        }
        return ids;
    }

    private void collectHoldings(BrokerAdapter adapter, Set<String> ids) {
        try {
            for (Holding holding : adapter.fetchHoldings()) {
                addScripId(holding.getScrip(), ids);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch holdings for active trades filter", e);
        }
    }

    private void collectPositions(BrokerAdapter adapter, Set<String> ids) {
        try {
            for (Position position : adapter.fetchPositions()) {
                if (position.getQuantity() != 0) {
                    addScripId(position.getScrip(), ids);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch positions for active trades filter", e);
        }
    }

    private void addScripId(Scrip scrip, Set<String> ids) {
        if (scrip != null && scrip.getId() != null) {
            ids.add(scrip.getId());
        }
    }

}
