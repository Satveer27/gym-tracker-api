package com.satveer27.gym_tracker_api.dto.users;

import com.satveer27.gym_tracker_api.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class GetAllUsersResponse {
    private List<UserResponse> users;
    private int currentPage;
    private int totalPages;
    private Long totalItems;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public static GetAllUsersResponse from(Page<User> users) {
        ArrayList<UserResponse> userResponses = new ArrayList<>();
        for(User user : users.getContent()) {
            userResponses.add(UserResponse.from(user));
        }
        return GetAllUsersResponse.builder()
                .users(userResponses)
                .currentPage(users.getNumber())
                .totalPages(users.getTotalPages())
                .totalItems(users.getTotalElements())
                .pageSize(users.getSize())
                .hasNext(users.hasNext())
                .hasPrevious(users.hasPrevious())
                .build();
    }
}
