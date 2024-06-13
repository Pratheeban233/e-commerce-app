package com.ecom.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public String createCustomer(CustomerRequest request) {
        var customer = repository.save(mapper.toCustomer(request));
        return customer.getId();
    }

    public void updateCustomer(CustomerRequest request) {
        Customer customer = repository.findById(request.id())
                .orElseThrow(() -> new CustomerNotFoundException(
                        String.format("Can not update customer: No Customer found with id :: %s", request.id())
                ));

        BeanUtils.copyProperties(request, customer);
        repository.save(customer);

    }

    public List<CustomerResponse> findAllCustomers() {
        return repository.findAll()
                .stream()
                .map(mapper::fromCustomer)
                .toList();
    }

    public CustomerResponse fetchCustomer(String id) {
        return repository.findById(id)
                .map(mapper::fromCustomer)
                .orElseThrow(() -> new CustomerNotFoundException(
                        String.format("No Customer found with id : " + id)
                ));
    }

    public Boolean existById(String id) {
        return repository.existsById(id);
    }

    public String deleteCustomer(String id) {
        repository.deleteById(id);
        return "Customer deleted.";
    }
}
