package com.ecom.kafka;

import com.ecom.customer.CustomerResponse;
import com.ecom.payment.PaymentMethod;
import com.ecom.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
