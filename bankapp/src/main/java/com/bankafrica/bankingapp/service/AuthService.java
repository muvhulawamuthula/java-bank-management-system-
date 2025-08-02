package com.bankafrica.bankingapp.service;


import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;


    public User registerUser(String firstName, String lastName, String email,
                             String idNumber, String phoneNumber, String password,
                             BigDecimal initialDeposit) throws Exception {


        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email already registered");
        }

        if (userRepository.existsByIdNumber(idNumber)) {
            throw new Exception("ID number already registered");
        }


        if (initialDeposit.compareTo(new BigDecimal("100.00")) < 0) {
            throw new Exception("Minimum initial deposit is R100.00");
        }


        User user = new User(firstName, lastName, email, idNumber, phoneNumber, password);


        String fullName = firstName + " " + lastName;
        BankAccount bankAccount = new BankAccount(fullName, initialDeposit);


        user.setBankAccount(bankAccount);


        return userRepository.save(user);
    }


    public User loginUser(String email, String password) throws Exception {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new Exception("Invalid email or password");
        }

        User user = userOpt.get();


        if (!user.getPassword().equals(password)) {
            throw new Exception("Invalid email or password");
        }

        return user;
    }


    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}