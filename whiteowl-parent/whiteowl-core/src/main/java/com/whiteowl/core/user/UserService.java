package com.whiteowl.core.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class UserService implements UserDetailsService {

	@NonNull private final UserRepository userRepository;
	@Getter private final PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
	}
	
	public long count() {
		return userRepository.count();
	}
	
	public Page<User> findAll(Pageable pageable){
		return userRepository.findAll(pageable);
	}
	
	public User save(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return userRepository.save(user);
	}
	
	public boolean existsById(String username) {
		return userRepository.existsById(username);
	}
	
	public void delete(User user) {
		userRepository.delete(user);
	}

}
