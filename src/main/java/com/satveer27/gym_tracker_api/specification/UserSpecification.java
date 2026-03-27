package com.satveer27.gym_tracker_api.specification;

import com.satveer27.gym_tracker_api.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class UserSpecification {

    public static Specification<User> hasUserName(String userName) {
        return (root, criteriaQuery, criteriaBuilder) ->
        {
            if (userName == null) {
                return null;
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("username")),
                    "%" + userName.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasEmail(String email) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (email == null) {
                return null;
            }
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("email")), email.toLowerCase());
        };
    }

    public static Specification<User> hasRole(String role) {
        return (root, criteriaQuery, criteriaBuilder) ->
        {   if (role == null){
            return null;
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("role")), role.toUpperCase());
        };
    }

    public static Specification<User> betweenTimeStamp(LocalDateTime start, LocalDateTime end) {
        return (root, criteriaQuery, criteriaBuilder) ->{
            if(start==null && end==null){
                return null;
            } else if (start == null && end != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), end);
            } else if (start != null && end == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), start);
            } else{
                return criteriaBuilder.between(root.get("timestamp"), start, end);
            }
        };
    }
}
