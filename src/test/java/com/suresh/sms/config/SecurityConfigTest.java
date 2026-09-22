package com.suresh.sms.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;


  
    // LOGIN IS PUBLIC
    

    @Test
    void testLoginIsPublic() throws Exception {

        mockMvc.perform(
                post("/auth/login")
        )
        .andExpect(status().isBadRequest());
    }


   
    // USER REGISTER IS PUBLIC
  
    @Test
    void testRegisterIsPublic() throws Exception {

        mockMvc.perform(
                post("/users/register")
        )
        .andExpect(status().isBadRequest());
    }


   
    // USER CAN READ STUDENTS
   

    @Test
    void testUserCanGetStudents() throws Exception {

        mockMvc.perform(
                get("/students")
                        .with(
                                user("suresh3")
                                        .roles("USER")
                        )
        )
        .andExpect(status().isOk());
    }



    // USER CANNOT CREATE STUDENT
  

    @Test
    void testUserCannotCreateStudent() throws Exception {

        mockMvc.perform(
                post("/students")
                        .with(
                                user("suresh3")
                                        .roles("USER")
                        )
        )
        .andExpect(status().isForbidden());
    }


    
    // USER CANNOT UPDATE STUDENT
   

    @Test
    void testUserCannotUpdateStudent() throws Exception {

        mockMvc.perform(
                put("/students/1")
                        .with(
                                user("suresh3")
                                        .roles("USER")
                        )
        )
        .andExpect(status().isForbidden());
    }


    
    // USER CANNOT DELETE STUDENT
    

    @Test
    void testUserCannotDeleteStudent() throws Exception {

        mockMvc.perform(
                delete("/students/1")
                        .with(
                                user("suresh3")
                                        .roles("USER")
                        )
        )
        .andExpect(status().isForbidden());
    }


   
    // NO TOKEN - GET STUDENTS
    

    @Test
    void testNoTokenGetStudents() throws Exception {

        mockMvc.perform(
                get("/students")
        )
        .andExpect(status().isUnauthorized());
    }


   
    // NO TOKEN - POST STUDENT
    

    @Test
    void testNoTokenCreateStudent() throws Exception {

        mockMvc.perform(
                post("/students")
        )
        .andExpect(status().isUnauthorized());
    }


    
    // NO TOKEN - PUT STUDENT
    

    @Test
    void testNoTokenUpdateStudent() throws Exception {

        mockMvc.perform(
                put("/students/1")
        )
        .andExpect(status().isUnauthorized());
    }


    
    // NO TOKEN - DELETE STUDENT
    

    @Test
    void testNoTokenDeleteStudent() throws Exception {

        mockMvc.perform(
                delete("/students/1")
        )
        .andExpect(status().isUnauthorized());
    }

    // 401 AND 403 RETURN A JSON BODY

    @Test
    void testUnauthorizedHasJsonBody() throws Exception {

        mockMvc.perform(get("/students"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testForbiddenHasJsonBody() throws Exception {

        mockMvc.perform(
                post("/students")
                        .with(user("suresh3").roles("USER"))
        )
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message").exists());
    }

    // SEARCH ENDPOINT: login required, works for USER (real database)

    @Test
    void testSearchRequiresLogin() throws Exception {

        mockMvc.perform(get("/students/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserCanSearchStudents() throws Exception {

        mockMvc.perform(
                get("/students/search")
                        .param("size", "5")
                        .with(user("suresh3").roles("USER"))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.page").value(0));
    }
}
