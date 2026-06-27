package com.whiteowl.workbench.charting;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Funds;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;

import java.util.List;

public interface ChartOrderService {

    List<GttOrder> fetchGtts();

    GttOrder createGtt(GttOrder gtt);

    GttOrder updateGtt(GttOrder gtt);

    void cancelGtt(int gttId);

    Order createOrder(Order order);

    Order updateOrder(Order order);

    Order cancelOrder(Order order);

    List<Order> fetchOrders();

    List<Position> fetchPositions();

    List<Holding> fetchHoldings();

    Funds fetchFunds();

    default double calculateAccountSize() {
        List<Position> positions = fetchPositions();
        List<Holding> holdings = fetchHoldings();
        Funds funds = fetchFunds();
        double positionsValue = positions.stream()
                .mapToDouble(p -> Math.abs(p.getQuantity()) * (double) p.getLastPrice())
                .sum();
        double holdingsValue = holdings.stream()
                .mapToDouble(h -> h.getQuantity() * (double) h.getLastPrice())
                .sum();
        double fundsValue = funds.getEquityAvailableCash();
        return positionsValue + holdingsValue + fundsValue;
    }

    default int suggestQuantity(float price, int preferredPositionCount) {
        if (price <= 0 || preferredPositionCount <= 0) return 1;
        double accountSize = calculateAccountSize();
        double amountPerTrade = accountSize / preferredPositionCount;
        int quantity = (int) Math.floor(amountPerTrade / price);
        return Math.max(quantity, 1);
    }

}
