package com.suresh.sms.controller;

import java.util.List;


import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.entity.Student;
import com.suresh.sms.service.StudentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/students")
@SecurityRequirement(name = "Bearer Authentication")
public class StudentController {

    @Autowired
    private StudentService service;


   
    // CREATE STUDENT


    @PostMapping
    public ResponseEntity<ApiResponse<StudentDTO>> saveStudent(
            @Valid @RequestBody StudentDTO dto) {

        StudentDTO savedStudent =
                service.saveStudent(dto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student created successfully",
                        savedStudent
                )
        );
    }


  
    // GET ALL STUDENTS


    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getAllStudents() {

        List<StudentDTO> students =
                service.getAllStudents();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Students retrieved successfully",
                        students
                )
        );
    }


    
    // GET STUDENT BY ID

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> getStudentById(
            @PathVariable Long id) {

        StudentDTO student =
                service.getStudentById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student retrieved successfully",
                        student
                )
        );
    }



    // SEARCH STUDENT BY NAME
    

    @GetMapping("/search/{name}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getStudentByName(
            @PathVariable String name) {

        List<StudentDTO> students = service.getStudentByName(name);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students found successfully", students));
    }


  
    // PAGINATION
 

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<StudentDTO>>> getStudents(
            @RequestParam int page,
            @RequestParam int size) {

        Page<StudentDTO> students =
                service.getStudents(page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Students retrieved successfully",
                        students
                )
        );
    }


    
    // SORT ASCENDING
   

    @GetMapping("/sort/{field}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> sortStudents(
            @PathVariable String field) {

        List<StudentDTO> students = service.sortStudents(field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students sorted successfully", students));
    }

 
    // SORT DESCENDING
   

    @GetMapping("/sortDesc/{field}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> sortStudentsDesc(
            @PathVariable String field) {

        List<StudentDTO> students = service.sortStudentsDesc(field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students sorted successfully in descending order", students));
    }
    
    // PAGINATION + SORTING
    

    @GetMapping("/pageSort")
    public ResponseEntity<ApiResponse<Page<StudentDTO>>>
    getStudentsWithPaginationAndSorting(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String field) {

        Page<StudentDTO> students =
                service.getStudentsWithPaginationAndSorting(page, size, field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students retrieved with pagination and sorting", students));
    }
   
    // UPDATE STUDENT
    

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO dto) {

        StudentDTO updatedStudent =
                service.updateStudent(id, dto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student updated successfully",
                        updatedStudent
                )
        );
    }


    
    // DELETE STUDENT


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStudent(
            @PathVariable Long id) {

        service.deleteStudent(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student deleted successfully",
                        null
                )
        );
    }
}