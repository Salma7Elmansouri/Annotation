package org.example.annotation.Service;

import org.example.annotation.Entity.User;
import org.example.annotation.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Transactional
@Service
public class UserService {
    @Autowired
    private  UserRepository userRepository;
     @Autowired
    private  PasswordEncoder passwordEncoder;


   // Save user
    public void save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }
    //Find All Annotators
    public List<User> findAllAnnotators() {
        return userRepository.findByRoleAndActiveTrue("ANNOTATOR");
    }
    //Find Users by Username ,that are Active (Not deleted)
    public Optional<User> findByUsernameAndActiveTrue(String username){
        return userRepository.findByUsernameAndActiveTrue(username);
    }
    //Find User by ID
    public User getById(Long annotatorId) {
        return userRepository.getById(annotatorId);
    }
    //Find Users by IDs
    public List<User> findByIds(List<Long> annotatorIds) {
        return userRepository.findAllById(annotatorIds);
    }
    //Update and Save User
    public void update(Long id, User updatedData) {
        User ex = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ex.setUsername(updatedData.getUsername());
        ex.setFirstName(updatedData.getFirstName());
        ex.setLastName(updatedData.getLastName());
        ex.setRole("ANNOTATOR");
        userRepository.save(ex);
    }
}
