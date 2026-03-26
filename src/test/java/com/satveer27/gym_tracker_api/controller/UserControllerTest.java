package com.satveer27.gym_tracker_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satveer27.gym_tracker_api.BaseIntegrationTest;
import com.satveer27.gym_tracker_api.dto.users.UpdatedUserRequest;
import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import org.hibernate.sql.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


public class UserControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    void register_shouldReturn409_whenDuplicateUsername() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("duplicate");
        request.setEmail("first@email.com");
        request.setPassword("password123");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        request.setEmail("second@email.com");
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400_whenFieldsAreEmpty() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("");
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_shouldReturn400_whenFieldsAreNull() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(null);
        request.setEmail(null);
        request.setPassword(null);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_shouldReturn400_whenInvalidEmailStructure() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("not-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.username").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors.password").doesNotExist());
    }

    @Test
    void register_shouldReturn400_whenInvalidSizeRequest() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ss");
        request.setEmail("test@gmail.com");
        request.setPassword("passwo");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_shouldReturn409_whenDuplicateEmail() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("user1");
        request.setEmail("same@email.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        request.setUsername("user2");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldStoreHashedPassword() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findByUsername("testuser").orElseThrow();


        assertNotEquals("password123", savedUser.getPasswordHash());


        assertTrue(savedUser.getPasswordHash().startsWith("$2a$"));
    }

    @Test
    void register_shouldNotReturnPasswordInResponse() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getUser_shouldReturn200_whenUserExist() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/users/" + id))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    void getUser_shouldReturn404_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }


    @Test
    void getUser_shouldReturn400_WhenInvalidUrl() throws Exception {
        mockMvc.perform(get("/api/v1/users/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void return404_whenUserAccessUnknownPage() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isNotFound());
    }

    @Test
    void return200_whenUserUpdates_allFieldChange() throws Exception {
        UserRegisterRequest user = new UserRegisterRequest();
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
        ).andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        String jsonBody = "{\"role\": \"trainer\", \"username\": \"testuser2\", \"email\": \"testuser2@gmail.com\"}";
        mockMvc.perform(patch("/api/v1/users/update/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("testuser2"))
                .andExpect(jsonPath("$.email").value("testuser2@gmail.com"))
                .andExpect(jsonPath("$.role").value("TRAINER"));

    }

    @Test
    void return200_whenUserUpdates_partialFieldChange() throws Exception {
        UserRegisterRequest user = new UserRegisterRequest();
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser2");
        request.setRole(Role.TRAINER);

        mockMvc.perform(patch("/api/v1/users/update/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("testuser2"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andExpect(jsonPath("$.role").value("TRAINER"));

    }

    @Test
    void return200_whenUserUpdates_allFieldEmptyString() throws Exception {
        UserRegisterRequest user = new UserRegisterRequest();
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("");
        request.setEmail("");

        mockMvc.perform(patch("/api/v1/users/update/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void return400_whenUserUpdates_invalidEmailAndUsername() throws Exception {
        UserRegisterRequest user = new UserRegisterRequest();
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setEmail("dasda");
        request.setUsername("t");

        mockMvc.perform(patch("/api/v1/users/update/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.role").doesNotExist());
    }


}
