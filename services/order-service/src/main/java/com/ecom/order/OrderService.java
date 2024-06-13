package com.ecom.order;

import com.ecom.customer.CustomerClient;
import com.ecom.customer.CustomerResponse;
import com.ecom.exception.BusinessException;
import com.ecom.kafka.OrderConfirmation;
import com.ecom.kafka.OrderProducer;
import com.ecom.orderline.OrderLineRequest;
import com.ecom.orderline.OrderLineService;
import com.ecom.payment.PaymentClient;
import com.ecom.payment.PaymentRequest;
import com.ecom.product.ProductClient;
import com.ecom.product.PurchaseRequest;
import com.ecom.product.PurchaseResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;

    @Transactional
    public Integer createOrder(OrderRequest request) {

        //check the customer
        CustomerResponse customer = customerClient.findByCustomerId(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order: No customer exist with id:: " + request.customerId()));

        //purchase the products --> product-service
        List<PurchaseResponse> purchaseProducts =
                this.productClient.purchaseProducts(request.products());

        //persist order
        var order = this.orderRepository.save(mapper.toOrder(request));

        //persist order lines
        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        // start payment process --> payment-service
        paymentClient.requestOrderPayment(new PaymentRequest(
                request.amount(),
                request.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        ));

        //send the order confirmation --> notification-service (kafka)
        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        request.amount(),
                        request.paymentMethod(),
                        customer,
                        purchaseProducts
                )
        );

        return order.getId();
    }

    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(mapper::fromOrder)
                .toList();
    }

    public OrderResponse findById(Integer orderId) {
        return orderRepository.findById(orderId)
                .map(mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No Order found with provided id: %d", orderId)
                ));
    }
}
