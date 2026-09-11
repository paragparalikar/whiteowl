package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteProduct;
import com.whiteowl.core.order.model.Product;

final class KiteProductMapper {

    public Product toProduct(KiteProduct kiteProduct) {
        if (kiteProduct == null) return null;
        return switch (kiteProduct) {
            case CNC -> Product.CNC;
            case NRML -> Product.NRML;
            case MIS -> Product.MIS;
        };
    }

    public KiteProduct toKiteProduct(Product product) {
        if (product == null) return null;
        return switch (product) {
            case CNC -> KiteProduct.CNC;
            case NRML -> KiteProduct.NRML;
            case MIS -> KiteProduct.MIS;
        };
    }

}
