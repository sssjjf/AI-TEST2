package com.example.jpatest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private GroupRepository groupRepository;

    @PostMapping("/add")
   public void test(){

        User user = User.builder().username("shengjiufei").build();
        User save1 = userRepository.save(user);

        Order testOrder = Order.builder().commont("testOrder").user(save1).build();
        Order save = orderRepository.save(testOrder);

        Brand brand = Brand.builder().name("testBrand").build();
        Group group= Group.builder().name("testGroup").build();
       // Group group1 = groupRepository.save(group);
        brand.setGroups(Collections.singleton(group));
        brandRepository.save(brand);

    }

    @GetMapping("/get")
    public List<User> get(){
        return userRepository.findAll();
    }

    @GetMapping("/get/b")
    public List<Brand> getB(){
        return brandRepository.findAll();
    }

    @PostMapping("/delete")
    public void delete(){
        userRepository.deleteAll();
    }
}