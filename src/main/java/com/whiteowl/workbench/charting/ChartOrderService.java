package com.whiteowl.workbench.charting;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.portfolio.model.Funds;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;

import java.util.List;
import java.util.OptionalInt;

public interface ChartOrderService {

    List<GttOrder> fetchGtts();

    GttOrder createGtt(GttOrder gtt);

    GttOrder updateGtt(GttOrder gtt);

    void cancelGtt(int gttId);

    List<OcoGttOrder> fetchOcoGtts();

    OcoGttOrder createOcoGtt(OcoGttOrder oco);

    OcoGttOrder updateOcoGtt(OcoGttOrder oco);

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

    default int suggestQuantity(float price, double positionSize) {
        if (price <= 0 || positionSize <= 0) return 1;
        int quantity = (int) Math.floor(positionSize / price);
        return Math.max(quantity, 1);
    }

    default OptionalInt findPortfolioQuantity(String scripId, OrderSide side) {
        int positionQty = fetchPositions().stream()
                .filter(p -> p.getScrip() != null && scripId.equals(p.getScrip().getId()))
                .mapToInt(Position::getQuantity)
                .sum();
        if (positionQty > 0 && side == OrderSide.SELL) return OptionalInt.of(positionQty);
        if (positionQty < 0 && side == OrderSide.BUY) return OptionalInt.of(Math.abs(positionQty));
        int holdingQty = fetchHoldings().stream()
                .filter(h -> h.getScrip() != null && scripId.equals(h.getScrip().getId()))
                .mapToInt(Holding::getQuantity)
                .sum();
        if (holdingQty > 0 && side == OrderSide.SELL) return OptionalInt.of(holdingQty);
        return OptionalInt.empty();
    }

}
