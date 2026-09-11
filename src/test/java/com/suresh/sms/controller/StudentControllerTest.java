package com.suresh.sms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.entity.Student;
import com.suresh.sms.service.StudentService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService service;


    // GET ALL STUDENTS
 

    @Test
    void testGetAllStudents() throws Exception {

        StudentDTO s1 = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        StudentDTO s2 = new StudentDTO(
                2L,
                "Rahul",
                "rahul@gmail.com",
                "B.Tech",
                45000.0
        );

        when(service.getAllStudents())
                .thenReturn(Arrays.asList(s1, s2));

        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())

                // ApiResponse
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Students retrieved successfully"))

                // Data
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name")
                        .value("Suresh"))
                .andExpect(jsonPath("$.data[1].name")
                        .value("Rahul"));
    }


    
    // GET STUDENT BY ID


    @Test
    void testGetStudentById() throws Exception {

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        when(service.getStudentById(1L))
                .thenReturn(dto);

        mockMvc.perform(get("/students/1"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Student retrieved successfully"))

                .andExpect(jsonPath("$.data.id")
                        .value(1))
                .andExpect(jsonPath("$.data.name")
                        .value("Suresh"))
                .andExpect(jsonPath("$.data.email")
                        .value("suresh@gmail.com"))
                .andExpect(jsonPath("$.data.course")
                        .value("MCA"))
                .andExpect(jsonPath("$.data.fee")
                        .value(50000.0));
    }


    
    // SAVE STUDENT
    

    @Test
    void testSaveStudent() throws Exception {

        StudentDTO dto = new StudentDTO(
                null,
                "Kiran",
                "kiran@gmail.com",
                "MCA",
                55000.0
        );

        StudentDTO saved = new StudentDTO(
                1L,
                "Kiran",
                "kiran@gmail.com",
                "MCA",
                55000.0
        );

        when(service.saveStudent(any(StudentDTO.class)))
                .thenReturn(saved);

        mockMvc.perform(
                post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message")
                .value("Student created successfully"))

        .andExpect(jsonPath("$.data.id")
                .value(1))
        .andExpect(jsonPath("$.data.name")
                .value("Kiran"))
        .andExpect(jsonPath("$.data.course")
                .value("MCA"))
        .andExpect(jsonPath("$.data.fee")
                .value(55000.0));
    }


   
    // UPDATE STUDENT
   

    @Test
    void testUpdateStudent() throws Exception {

        StudentDTO dto = new StudentDTO(
                1L,
                "Kiran",
                "kiran@gmail.com",
                "B.Tech",
                60000.0
        );

        when(service.updateStudent(
                any(Long.class),
                any(StudentDTO.class)))
                .thenReturn(dto);

        mockMvc.perform(
                put("/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(dto)
                        )
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message")
                .value("Student updated successfully"))

        .andExpect(jsonPath("$.data.id")
                .value(1))
        .andExpect(jsonPath("$.data.name")
                .value("Kiran"))
        .andExpect(jsonPath("$.data.course")
                .value("B.Tech"))
        .andExpect(jsonPath("$.data.fee")
                .value(60000.0));
    }


 
    // DELETE STUDENT
   

    @Test
    void testDeleteStudent() throws Exception {

        doNothing()
                .when(service)
                .deleteStudent(1L);

        mockMvc.perform(
                delete("/students/1")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value("Student deleted successfully"))

        .andExpect(jsonPath("$.data")
                .doesNotExist());

        verify(service)
                .deleteStudent(1L);
    }



    // SEARCH STUDENT BY NAME


    @Test
    void testGetStudentByName() throws Exception {

        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        when(service.getStudentByName("Suresh"))
                .thenReturn(Arrays.asList(student));

        mockMvc.perform(
                get("/students/search/Suresh")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value("Students found successfully"))

        .andExpect(jsonPath("$.data.length()")
                .value(1))

        .andExpect(jsonPath("$.data[0].name")
                .value("Suresh"));
    }


   
    // PAGINATION
   
    @Test
    void testGetStudents() throws Exception {

        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        Page<StudentDTO> page =
                new PageImpl<>(
                        Arrays.asList(student),
                        PageRequest.of(0, 10),
                        1
                );

        when(service.getStudents(0, 10))
                .thenReturn(page);

        mockMvc.perform(
                get("/students/page")
                        .param("page", "0")
                        .param("size", "10")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value("Students retrieved successfully"))

        .andExpect(jsonPath("$.data.content.length()")
                .value(1))

        .andExpect(jsonPath("$.data.content[0].name")
                .value("Suresh"));
    }



    // SORT ASCENDING


    @Test
    void testSortStudents() throws Exception {

        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        when(service.sortStudents("name"))
                .thenReturn(Arrays.asList(student));

        mockMvc.perform(
                get("/students/sort/name")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value("Students sorted successfully"))

        .andExpect(jsonPath("$.data.length()")
                .value(1))

        .andExpect(jsonPath("$.data[0].name")
                .value("Suresh"));
    }


  
    // SORT DESCENDING


    @Test
    void testSortStudentsDesc() throws Exception {

        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        when(service.sortStudentsDesc("name"))
                .thenReturn(Arrays.asList(student));

        mockMvc.perform(
                get("/students/sortDesc/name")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value("Students sorted successfully in descending order"))

        .andExpect(jsonPath("$.data.length()")
                .value(1))

        .andExpect(jsonPath("$.data[0].name")
                .value("Suresh"));
    }



    // PAGINATION + SORTING


    @Test
    void testGetStudentsWithPaginationAndSorting()
            throws Exception {

        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        Page<StudentDTO> page =
                new PageImpl<>(
                        Arrays.asList(student),
                        PageRequest.of(0, 10),
                        1
                );

        when(
                service.getStudentsWithPaginationAndSorting(
                        0,
                        10,
                        "name"
                )
        )
        .thenReturn(page);

        mockMvc.perform(
                get("/students/pageSort")
                        .param("page", "0")
                        .param("size", "10")
                        .param("field", "name")
        )
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.success")
                .value(true))

        .andExpect(jsonPath("$.message")
                .value(
                    "Students retrieved with pagination and sorting"
                ))

        .andExpect(jsonPath("$.data.content.length()")
                .value(1))

        .andExpect(jsonPath("$.data.content[0].name")
                .value("Suresh"));
    }
}